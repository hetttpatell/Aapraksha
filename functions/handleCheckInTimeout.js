/**
 * handleCheckInTimeout — Scheduled Cloud Function
 * 
 * Phase 9 Implementation:
 * Runs every minute to check for expired check-ins with no response.
 * Triggers SOS for timed-out check-ins and notifies emergency contacts.
 * 
 * Firestore trigger:
 * - Monitors check_ins collection
 * - Looks for documents where:
 *   - responded = false
 *   - timestamp is older than 30 seconds
 *   - action = "pending"
 * 
 * Actions:
 * 1. Mark check-in as "timeout"
 * 2. Create SOS alert document
 * 3. Send notifications to emergency contacts
 * 4. Log the timeout event
 */

const functions = require("firebase-functions");
const admin = require("firebase-admin");

// Initialize Firebase Admin (only if not already initialized)
if (!admin.apps.length) {
  admin.initializeApp();
}

const db = admin.firestore();

exports.handleCheckInTimeout = functions.pubsub
    .schedule("every 1 minutes")
    .onRun(async (context) => {
      console.log("⏰ Running check-in timeout handler...");

      const thirtySecondsAgo = admin.firestore.Timestamp.fromMillis(
          Date.now() - 30 * 1000
      );

      try {
        // Query for timed-out check-ins
        const timedOutCheckIns = await db
            .collection("check_ins")
            .where("responded", "==", false)
            .where("action", "==", "pending")
            .where("timestamp", "<=", thirtySecondsAgo)
            .get();

        if (timedOutCheckIns.empty) {
          console.log("✅ No timed-out check-ins found");
          return null;
        }

        console.log(`🚨 Found ${timedOutCheckIns.size} timed-out check-ins`);

        // Process each timed-out check-in
        const promises = timedOutCheckIns.docs.map(async (doc) => {
          const checkInData = doc.data();
          const userId = checkInData.userId;
          const triggerReason = checkInData.triggerReason;
          const location = checkInData.location;

          console.log(`Processing timeout for user: ${userId}, reason: ${triggerReason}`);

          // Mark check-in as timed out
          await doc.ref.update({
            responded: false,
            action: "timeout",
            timeoutAt: admin.firestore.Timestamp.now(),
          });

          // Create SOS alert
          await createSOSAlert(userId, triggerReason, location);

          // Notify emergency contacts
          await notifyEmergencyContacts(userId, triggerReason, location);
        });

        await Promise.all(promises);

        console.log(`✅ Processed ${timedOutCheckIns.size} timeouts successfully`);
        return null;
      } catch (error) {
        console.error("❌ Error handling check-in timeouts:", error);
        return null;
      }
    });

/**
 * Create SOS alert for timed-out check-in
 */
async function createSOSAlert(userId, triggerReason, location) {
  try {
    // Fetch user data
    const userDoc = await db.collection("users").doc(userId).get();
    if (!userDoc.exists) {
      console.error(`User ${userId} not found`);
      return;
    }

    const userData = userDoc.data();

    // Fetch emergency contacts
    const contactsSnapshot = await db
        .collection("users")
        .doc(userId)
        .collection("emergency_contacts")
        .get();

    const emergencyContacts = contactsSnapshot.docs.map((doc) => doc.data());

    // Create SOS alert document
    const alertData = {
      userId: userId,
      status: "ACTIVE",
      triggeredBy: "check_in_timeout",
      triggerReason: triggerReason,
      location: location || {latitude: 0, longitude: 0},
      timestamp: admin.firestore.Timestamp.now(),
      emergencyContacts: emergencyContacts,
      deviceInfo: {
        triggeredFrom: "check_in_system",
      },
      resolved: false,
    };

    await db.collection("alerts").add(alertData);

    console.log(`✅ SOS alert created for user ${userId}`);
  } catch (error) {
    console.error(`❌ Error creating SOS alert for ${userId}:`, error);
  }
}

/**
 * Notify emergency contacts about timed-out check-in
 */
async function notifyEmergencyContacts(userId, triggerReason, location) {
  try {
    // Fetch user data
    const userDoc = await db.collection("users").doc(userId).get();
    if (!userDoc.exists) {
      console.error(`User ${userId} not found`);
      return;
    }

    const userData = userDoc.data();
    const userName = userData.fullName || "User";

    // Fetch emergency contacts with FCM tokens
    const contactsSnapshot = await db
        .collection("users")
        .doc(userId)
        .collection("emergency_contacts")
        .get();

    const notifications = contactsSnapshot.docs.map(async (doc) => {
      const contact = doc.data();

      // Build notification message
      const message = {
        notification: {
          title: `🚨 EMERGENCY - ${userName} Needs Help`,
          body: `${userName} did not respond to a safety check-in. ` +
              `Reason: ${getTriggerReasonText(triggerReason)}. ` +
              `Please check on them immediately!`,
        },
        data: {
          type: "check_in_timeout",
          userId: userId,
          triggerReason: triggerReason,
          latitude: location ? location.latitude.toString() : "0",
          longitude: location ? location.longitude.toString() : "0",
        },
        token: contact.fcmToken, // Assume contact has FCM token
      };

      // Send notification (if FCM token exists)
      if (contact.fcmToken) {
        try {
          await admin.messaging().send(message);
          console.log(`✅ Notification sent to ${contact.name}`);
        } catch (error) {
          console.error(`❌ Error sending notification to ${contact.name}:`, error);
        }
      }
    });

    await Promise.all(notifications);

    console.log(`✅ Notified emergency contacts for user ${userId}`);
  } catch (error) {
    console.error(`❌ Error notifying contacts for ${userId}:`, error);
  }
}

/**
 * Get human-readable trigger reason text
 */
function getTriggerReasonText(triggerReason) {
  const reasons = {
    "DANGER_ZONE": "Entered high-risk area",
    "ANOMALY": "Unusual activity detected",
    "AUDIO_THREAT": "Threat sound detected",
    "FALL_DETECTED": "Fall or impact detected",
    "SCHEDULED": "Scheduled check-in",
  };

  return reasons[triggerReason] || "Safety check-in timeout";
}
