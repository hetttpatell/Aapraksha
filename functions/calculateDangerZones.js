/**
 * calculateDangerZones.js
 * 
 * Firebase Cloud Function — Firestore onWrite trigger
 * 
 * PURPOSE:
 * When an SOS alert is created or its status changes, this function
 * recalculates the danger score for the geographic zone (geohash cell)
 * where the SOS occurred. The result is cached in the 'danger_zones'
 * Firestore collection for cheap reads by the Android app.
 * 
 * EFFICIENCY:
 * - Only fires on actual SOS data changes (NOT on a timer/schedule)
 * - One function invocation per SOS event = well within 2M free calls/month
 * - Results cached in Firestore = Android reads from cache, not recomputes
 * 
 * SCORING FORMULA:
 * dangerScore = (
 *   0.40 × frequencyScore +      // SOS count in last 30 days (normalized)
 *   0.25 × recencyScore   +      // How recently the last SOS occurred
 *   0.20 × severityScore  +      // Duration + resolved ratio
 *   0.15 × timePatternScore      // Night incidents weighted higher
 * )
 * 
 * RANK MAPPING:
 *   0.0  - 0.2  → SAFE (Rank 1)
 *   0.2  - 0.4  → CAUTION (Rank 2)
 *   0.4  - 0.6  → HIGH_RISK (Rank 3)
 *   0.6  - 0.8  → DANGER_ZONE (Rank 4)
 *   0.8  - 1.0  → ACTIVE_THREAT (Rank 5)
 */

const { onDocumentWritten } = require("firebase-functions/v2/firestore");
const { getFirestore, Timestamp } = require("firebase-admin/firestore");
const { initializeApp } = require("firebase-admin/app");

initializeApp();
const db = getFirestore();

// ========== Constants ==========

const COLLECTION_ALERTS = "alerts";
const COLLECTION_DANGER_ZONES = "danger_zones";

// Scoring weights
const WEIGHT_FREQUENCY = 0.40;
const WEIGHT_RECENCY = 0.25;
const WEIGHT_SEVERITY = 0.20;
const WEIGHT_TIME_PATTERN = 0.15;

// Thresholds
const MAX_SOS_FOR_NORMALIZATION = 20; // 20+ SOS = max frequency score
const RECENCY_DECAY_DAYS = 30;        // Scores decay over 30 days
const NIGHT_BONUS_MULTIPLIER = 1.3;   // Night SOS weighted 30% higher

// Rank thresholds
const RANK_THRESHOLDS = [
  { min: 0.8, rank: 5, label: "ACTIVE_THREAT" },
  { min: 0.6, rank: 4, label: "DANGER_ZONE" },
  { min: 0.4, rank: 3, label: "HIGH_RISK" },
  { min: 0.2, rank: 2, label: "CAUTION" },
  { min: 0.0, rank: 1, label: "SAFE" },
];

// Time of day windows for peak detection
const TIME_WINDOWS = {
  MORNING: "6 AM - 12 PM",
  AFTERNOON: "12 PM - 5 PM",
  EVENING: "5 PM - 9 PM",
  NIGHT: "9 PM - 6 AM",
};

// ========== Helper Functions ==========

/**
 * Calculate normalized frequency score (0.0 - 1.0)
 * More SOS events = higher score, capped at MAX_SOS_FOR_NORMALIZATION
 */
function calculateFrequencyScore(sosCount) {
  return Math.min(sosCount / MAX_SOS_FOR_NORMALIZATION, 1.0);
}

/**
 * Calculate recency score (0.0 - 1.0)
 * More recent SOS = higher score, decays exponentially over 30 days
 */
function calculateRecencyScore(lastSosTime) {
  if (!lastSosTime) return 0.0;

  const now = Date.now();
  const lastSosMs = lastSosTime.toMillis ? lastSosTime.toMillis() : lastSosTime;
  const daysSinceLastSos = (now - lastSosMs) / (1000 * 60 * 60 * 24);

  if (daysSinceLastSos <= 0) return 1.0;
  if (daysSinceLastSos >= RECENCY_DECAY_DAYS) return 0.0;

  // Exponential decay: score = e^(-daysSince / halfLife)
  const halfLife = RECENCY_DECAY_DAYS / 3; // ~10 day half-life
  return Math.exp(-daysSinceLastSos / halfLife);
}

/**
 * Calculate severity score (0.0 - 1.0)
 * Based on average SOS duration and resolution rate
 */
function calculateSeverityScore(alerts) {
  if (alerts.length === 0) return 0.0;

  let totalDuration = 0;
  let resolvedCount = 0;
  let validDurationCount = 0;

  for (const alert of alerts) {
    const duration = alert.durationSeconds || 0;
    if (duration > 0) {
      totalDuration += duration;
      validDurationCount++;
    }
    if (alert.resolved === true) {
      resolvedCount++;
    }
  }

  // Average duration score (longer SOS = more severe)
  // Normalize: 0-60s = low, 60-300s = medium, 300s+ = high
  const avgDuration = validDurationCount > 0 ? totalDuration / validDurationCount : 0;
  const durationScore = Math.min(avgDuration / 300, 1.0);

  // Resolution ratio (more resolved = more real threats, not false alarms)
  const resolvedRatio = alerts.length > 0 ? resolvedCount / alerts.length : 0;

  // Combined severity: 60% duration + 40% resolution rate
  return 0.6 * durationScore + 0.4 * resolvedRatio;
}

/**
 * Calculate time pattern score (0.0 - 1.0)
 * Night incidents get a bonus multiplier
 */
function calculateTimePatternScore(alerts) {
  if (alerts.length === 0) return 0.0;

  let nightCount = 0;
  const timeCounts = { MORNING: 0, AFTERNOON: 0, EVENING: 0, NIGHT: 0 };

  for (const alert of alerts) {
    const timeOfDay = alert.timeOfDay || "MORNING";
    timeCounts[timeOfDay] = (timeCounts[timeOfDay] || 0) + 1;

    if (timeOfDay === "NIGHT" || timeOfDay === "EVENING") {
      nightCount++;
    }
  }

  // Night/evening ratio with bonus
  const nightRatio = nightCount / alerts.length;
  return Math.min(nightRatio * NIGHT_BONUS_MULTIPLIER, 1.0);
}

/**
 * Find the peak time window for a set of alerts
 */
function findPeakTimeWindow(alerts) {
  const timeCounts = { MORNING: 0, AFTERNOON: 0, EVENING: 0, NIGHT: 0 };

  for (const alert of alerts) {
    const timeOfDay = alert.timeOfDay || "MORNING";
    timeCounts[timeOfDay] = (timeCounts[timeOfDay] || 0) + 1;
  }

  let peakTime = "MORNING";
  let maxCount = 0;

  for (const [time, count] of Object.entries(timeCounts)) {
    if (count > maxCount) {
      maxCount = count;
      peakTime = time;
    }
  }

  return TIME_WINDOWS[peakTime] || "Unknown";
}

/**
 * Get rank and label from danger score
 */
function getRankFromScore(score) {
  for (const threshold of RANK_THRESHOLDS) {
    if (score >= threshold.min) {
      return { rank: threshold.rank, label: threshold.label };
    }
  }
  return { rank: 1, label: "SAFE" };
}

/**
 * Decode geohash to approximate center coordinates
 * (Simplified version — matches the Android GeoHashUtil.decode)
 */
function decodeGeohash(geohash) {
  const BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz";
  let latMin = -90.0, latMax = 90.0;
  let lonMin = -180.0, lonMax = 180.0;
  let isLon = true;

  for (const c of geohash) {
    const charIndex = BASE32.indexOf(c);
    if (charIndex < 0) continue;

    for (let bit = 4; bit >= 0; bit--) {
      if (isLon) {
        const mid = (lonMin + lonMax) / 2;
        if (charIndex & (1 << bit)) {
          lonMin = mid;
        } else {
          lonMax = mid;
        }
      } else {
        const mid = (latMin + latMax) / 2;
        if (charIndex & (1 << bit)) {
          latMin = mid;
        } else {
          latMax = mid;
        }
      }
      isLon = !isLon;
    }
  }

  return {
    latitude: (latMin + latMax) / 2,
    longitude: (lonMin + lonMax) / 2,
  };
}

// ========== Main Scoring Function ==========

/**
 * Recalculate the danger score for a specific geohash zone.
 * Queries all SOS events in this zone from the last 30 days,
 * computes a weighted danger score, and caches the result.
 */
async function recalculateDangerZone(geohash) {
  if (!geohash) {
    console.log("No geohash provided — skipping");
    return null;
  }

  console.log(`Recalculating danger zone for geohash: ${geohash}`);

  // Query SOS events in this geohash cell from last 30 days
  const thirtyDaysAgo = new Date();
  thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);
  const thirtyDaysAgoTimestamp = Timestamp.fromDate(thirtyDaysAgo);

  const alertsSnapshot = await db
    .collection(COLLECTION_ALERTS)
    .where("geohash", "==", geohash)
    .where("sosData.triggeredAt", ">=", thirtyDaysAgoTimestamp)
    .get();

  const alerts = [];
  let lastSosTime = null;
  let unresolvedCount = 0;

  alertsSnapshot.forEach((doc) => {
    const data = doc.data();
    alerts.push(data);

    // Track most recent SOS
    const triggeredAt = data.sosData?.triggeredAt;
    if (triggeredAt && (!lastSosTime || triggeredAt.toMillis() > lastSosTime.toMillis())) {
      lastSosTime = triggeredAt;
    }

    // Count unresolved (active) alerts
    if (data.status === "ACTIVE" || data.status === "TRIGGERED") {
      unresolvedCount++;
    }
  });

  const sosCount = alerts.length;

  // If no SOS events, clear the danger zone or mark as SAFE
  if (sosCount === 0) {
    console.log(`No SOS events in zone ${geohash} — marking as SAFE`);
    await db.collection(COLLECTION_DANGER_ZONES).doc(geohash).set({
      geohash,
      rank: 1,
      label: "SAFE",
      sosCount30Days: 0,
      unresolvedCount: 0,
      dangerScore: 0.0,
      calculatedAt: Timestamp.now(),
      peakTimeWindow: null,
      ...decodeGeohash(geohash),
    }, { merge: true });
    return null;
  }

  // Calculate individual scores
  const frequencyScore = calculateFrequencyScore(sosCount);
  const recencyScore = calculateRecencyScore(lastSosTime);
  const severityScore = calculateSeverityScore(alerts);
  const timePatternScore = calculateTimePatternScore(alerts);

  // Weighted combined score
  let dangerScore =
    WEIGHT_FREQUENCY * frequencyScore +
    WEIGHT_RECENCY * recencyScore +
    WEIGHT_SEVERITY * severityScore +
    WEIGHT_TIME_PATTERN * timePatternScore;

  // Boost: if there are currently unresolved SOS events, push score higher
  if (unresolvedCount > 0) {
    dangerScore = Math.min(dangerScore + 0.2 * unresolvedCount, 1.0);
  }

  // Clamp to [0, 1]
  dangerScore = Math.max(0, Math.min(1, dangerScore));

  const { rank, label } = getRankFromScore(dangerScore);
  const peakTimeWindow = findPeakTimeWindow(alerts);
  const center = decodeGeohash(geohash);

  const dangerZoneData = {
    geohash,
    rank,
    label,
    sosCount30Days: sosCount,
    unresolvedCount,
    lastSosTime: lastSosTime || null,
    dangerScore: Math.round(dangerScore * 1000) / 1000, // 3 decimal places
    calculatedAt: Timestamp.now(),
    peakTimeWindow,
    centerLatitude: center.latitude,
    centerLongitude: center.longitude,
  };

  console.log(`Zone ${geohash}: score=${dangerScore.toFixed(3)}, rank=${rank} (${label}), ` +
    `sosCount=${sosCount}, unresolved=${unresolvedCount}`);

  // Write to danger_zones collection (upsert)
  await db.collection(COLLECTION_DANGER_ZONES).doc(geohash).set(dangerZoneData, { merge: true });

  return dangerZoneData;
}

// ========== Cloud Function Triggers ==========

/**
 * onWrite trigger for 'alerts/{alertId}'
 * Fires when any SOS alert is created, updated, or deleted.
 * Extracts the geohash and recalculates the zone's danger score.
 */
const onSOSCreated = onDocumentWritten("alerts/{alertId}", async (event) => {
  try {
    // Get geohash from the document (after write)
    const afterData = event.data?.after?.data();
    const beforeData = event.data?.before?.data();

    // Get the geohash from either the new or old document
    const geohash = afterData?.geohash || beforeData?.geohash;

    if (!geohash) {
      console.log(`Alert ${event.params.alertId} has no geohash — skipping danger calculation`);
      return null;
    }

    // Only recalculate if:
    // 1. New alert created (before doesn't exist)
    // 2. Status changed (ACTIVE → CANCELLED, ACTIVE → RESOLVED, etc.)
    // 3. Alert deleted
    const isNew = !beforeData;
    const isDeleted = !afterData;
    const statusChanged = beforeData?.status !== afterData?.status;
    const resolvedChanged = beforeData?.resolved !== afterData?.resolved;

    if (!isNew && !isDeleted && !statusChanged && !resolvedChanged) {
      console.log(`Alert ${event.params.alertId} changed but no status/resolved change — skipping`);
      return null;
    }

    console.log(`Trigger: alert ${event.params.alertId}, ` +
      `status: ${beforeData?.status || "NEW"} → ${afterData?.status || "DELETED"}`);

    return await recalculateDangerZone(geohash);
  } catch (error) {
    console.error("Error in onSOSCreated:", error);
    return null;
  }
});

/**
 * Alias for the same trigger (kept for clarity in exports)
 */
const onSOSUpdated = onSOSCreated;

module.exports = {
  calculateDangerZones: { onSOSCreated, onSOSUpdated },
};
