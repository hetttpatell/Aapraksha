package com.example.aapraksha.ai.danger;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;

import java.io.Serializable;

/**
 * DangerZone model for Firestore 'danger_zones' collection.
 * Stores cached danger intelligence scores for geographic zones.
 * 
 * Danger scores are calculated by a Cloud Function (onWrite trigger)
 * whenever a new SOS event is logged — NOT on a scheduled timer.
 * Results are cached here so the Android app can read them cheaply.
 */
public class DangerZone implements Serializable {

    /**
     * Zone danger rank levels (1 = safest, 5 = most dangerous)
     */
    public static final int RANK_SAFE = 1;
    public static final int RANK_CAUTION = 2;
    public static final int RANK_HIGH_RISK = 3;
    public static final int RANK_DANGER_ZONE = 4;
    public static final int RANK_ACTIVE_THREAT = 5;

    /**
     * Label constants matching rank levels
     */
    public static final String LABEL_SAFE = "SAFE";
    public static final String LABEL_CAUTION = "CAUTION";
    public static final String LABEL_HIGH_RISK = "HIGH_RISK";
    public static final String LABEL_DANGER_ZONE = "DANGER_ZONE";
    public static final String LABEL_ACTIVE_THREAT = "ACTIVE_THREAT";

    private String geohash;             // GeoHash string (precision 6 = ~1.2km cell)
    private int rank;                   // 1-5 (SAFE to ACTIVE_THREAT)
    private String label;               // Human-readable rank label
    private int sosCount30Days;         // Total SOS events in this zone in last 30 days
    private int unresolvedCount;        // Currently unresolved SOS events
    private Timestamp lastSosTime;      // Timestamp of the most recent SOS
    private double dangerScore;         // Calculated score 0.0 - 1.0
    private Timestamp calculatedAt;     // When the score was last computed
    private String peakTimeWindow;      // e.g., "10 PM - 1 AM"
    private double centerLatitude;      // Center of the geohash cell
    private double centerLongitude;     // Center of the geohash cell
    private String nearestLandmark;     // Optional: human-readable location name

    /**
     * Default constructor for Firestore deserialization
     */
    public DangerZone() {
        this.rank = RANK_SAFE;
        this.label = LABEL_SAFE;
        this.dangerScore = 0.0;
    }

    /**
     * Construct from scored data (used by Cloud Function results)
     */
    public DangerZone(String geohash, double dangerScore) {
        this.geohash = geohash;
        this.dangerScore = dangerScore;
        updateRankFromScore();
    }

    // ========== Rank Classification ==========

    /**
     * Get rank and label from danger score.
     * Score thresholds:
     *   0.0  - 0.2  → SAFE (Rank 1)
     *   0.2  - 0.4  → CAUTION (Rank 2)
     *   0.4  - 0.6  → HIGH_RISK (Rank 3)
     *   0.6  - 0.8  → DANGER_ZONE (Rank 4)
     *   0.8  - 1.0  → ACTIVE_THREAT (Rank 5)
     */
    public void updateRankFromScore() {
        if (dangerScore >= 0.8) {
            this.rank = RANK_ACTIVE_THREAT;
            this.label = LABEL_ACTIVE_THREAT;
        } else if (dangerScore >= 0.6) {
            this.rank = RANK_DANGER_ZONE;
            this.label = LABEL_DANGER_ZONE;
        } else if (dangerScore >= 0.4) {
            this.rank = RANK_HIGH_RISK;
            this.label = LABEL_HIGH_RISK;
        } else if (dangerScore >= 0.2) {
            this.rank = RANK_CAUTION;
            this.label = LABEL_CAUTION;
        } else {
            this.rank = RANK_SAFE;
            this.label = LABEL_SAFE;
        }
    }

    /**
     * Whether this zone should trigger a user warning notification
     */
    public boolean shouldWarnUser() {
        return rank >= RANK_HIGH_RISK;
    }

    /**
     * Whether emergency contacts should receive a standby alert
     * when user enters this zone
     */
    public boolean shouldAlertContacts() {
        return rank >= RANK_DANGER_ZONE;
    }

    /**
     * Get a user-facing description of this zone's danger level
     */
    public String getUserNotificationText() {
        switch (rank) {
            case RANK_CAUTION:
                return "Heads up: " + sosCount30Days + " SOS alert(s) reported nearby in the last 30 days.";
            case RANK_HIGH_RISK:
                return "⚠️ HIGH RISK area detected. " + sosCount30Days + " SOS alerts this month. " +
                       "Most incidents: " + (peakTimeWindow != null ? peakTimeWindow : "Unknown") + ". Stay alert.";
            case RANK_DANGER_ZONE:
                return "🚨 DANGER ZONE! " + sosCount30Days + " SOS alerts this month. " +
                       "Your emergency contacts have been notified to standby.";
            case RANK_ACTIVE_THREAT:
                return "⛔ ACTIVE THREAT nearby! " + unresolvedCount + " unresolved SOS right now. " +
                       "Leave the area immediately. Contacts are on standby.";
            default:
                return null; // SAFE — no notification
        }
    }

    /**
     * Get the standby notification text for emergency contacts
     */
    public String getContactStandbyText(String userName) {
        if (!shouldAlertContacts()) return null;

        return "STANDBY ALERT — Aapraksha\n" +
               userName + " has entered a " + label.replace("_", " ") + " near " +
               (nearestLandmark != null ? nearestLandmark : "their current location") + ".\n" +
               "No emergency yet — this is a precautionary alert.\n" +
               "Please stay reachable. You will be notified immediately if an SOS is triggered.";
    }

    // ========== Getters & Setters ==========

    public String getGeohash() { return geohash; }
    public void setGeohash(String geohash) { this.geohash = geohash; }

    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public int getSosCount30Days() { return sosCount30Days; }
    public void setSosCount30Days(int sosCount30Days) { this.sosCount30Days = sosCount30Days; }

    public int getUnresolvedCount() { return unresolvedCount; }
    public void setUnresolvedCount(int unresolvedCount) { this.unresolvedCount = unresolvedCount; }

    public Timestamp getLastSosTime() { return lastSosTime; }
    public void setLastSosTime(Timestamp lastSosTime) { this.lastSosTime = lastSosTime; }

    public double getDangerScore() { return dangerScore; }
    public void setDangerScore(double dangerScore) {
        this.dangerScore = dangerScore;
        updateRankFromScore();
    }

    public Timestamp getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(Timestamp calculatedAt) { this.calculatedAt = calculatedAt; }

    public String getPeakTimeWindow() { return peakTimeWindow; }
    public void setPeakTimeWindow(String peakTimeWindow) { this.peakTimeWindow = peakTimeWindow; }

    public double getCenterLatitude() { return centerLatitude; }
    public void setCenterLatitude(double centerLatitude) { this.centerLatitude = centerLatitude; }

    public double getCenterLongitude() { return centerLongitude; }
    public void setCenterLongitude(double centerLongitude) { this.centerLongitude = centerLongitude; }

    public String getNearestLandmark() { return nearestLandmark; }
    public void setNearestLandmark(String nearestLandmark) { this.nearestLandmark = nearestLandmark; }

    @Override
    public String toString() {
        return "DangerZone{" +
                "geohash='" + geohash + '\'' +
                ", rank=" + rank +
                ", label='" + label + '\'' +
                ", dangerScore=" + dangerScore +
                ", sosCount30Days=" + sosCount30Days +
                '}';
    }
}
