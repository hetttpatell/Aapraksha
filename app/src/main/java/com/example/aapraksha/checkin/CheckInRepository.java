package com.example.aapraksha.checkin;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * CheckInRepository — Logs check-in events to Firestore
 * 
 * Phase 9 Implementation:
 * Stores all check-in requests and user responses for:
 * - Analytics (response rate, false positive rate)
 * - Incident history
 * - Pattern analysis
 * 
 * Firestore Structure:
 * check_ins/{checkInId}
 *   - userId: String
 *   - type: String ("anomaly" | "scheduled" | "manual")
 *   - triggerReason: String (source from CheckInNotificationHelper)
 *   - location: GeoPoint
 *   - timestamp: Timestamp
 *   - responded: Boolean
 *   - action: String ("safe" | "help" | "timeout")
 *   - responseTimeSeconds: Number
 */
public class CheckInRepository {

    private static final String TAG = "CheckInRepository";
    private static final String COLLECTION_CHECK_INS = "check_ins";

    /**
     * Log a check-in response to Firestore
     * 
     * @param context Application context
     * @param source Trigger source (DANGER_ZONE, ANOMALY, AUDIO_THREAT, etc.)
     * @param action User action ("safe", "help", "timeout")
     * @param responded Whether user responded (true) or timed out (false)
     * @param responseTimeSeconds Time taken to respond in seconds
     */
    public static void logCheckInResponse(
            Context context,
            String source,
            String action,
            boolean responded,
            int responseTimeSeconds
    ) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Log.w(TAG, "No authenticated user — cannot log check-in");
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> checkInData = new HashMap<>();
        checkInData.put("userId", userId);
        checkInData.put("type", "anomaly"); // For now, all are anomaly-based
        checkInData.put("triggerReason", source);
        checkInData.put("timestamp", Timestamp.now());
        checkInData.put("responded", responded);
        checkInData.put("action", action);
        checkInData.put("responseTimeSeconds", responseTimeSeconds);

        // Add location if available (can be enhanced to pass location data)
        checkInData.put("location", null);

        db.collection(COLLECTION_CHECK_INS)
                .add(checkInData)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Check-in logged: " + documentReference.getId() +
                            " | Action: " + action +
                            " | Response time: " + responseTimeSeconds + "s");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to log check-in", e);
                });
    }

    /**
     * Log a scheduled check-in (for future Phase 9 enhancement)
     * 
     * @param context Application context
     * @param latitude User's latitude
     * @param longitude User's longitude
     */
    public static void logScheduledCheckIn(Context context, double latitude, double longitude) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Log.w(TAG, "No authenticated user — cannot log scheduled check-in");
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> checkInData = new HashMap<>();
        checkInData.put("userId", userId);
        checkInData.put("type", "scheduled");
        checkInData.put("triggerReason", "periodic_check_in");
        checkInData.put("timestamp", Timestamp.now());
        checkInData.put("responded", false); // Will be updated when user responds
        checkInData.put("action", "pending");
        checkInData.put("responseTimeSeconds", 0);

        // Add location
        Map<String, Double> locationMap = new HashMap<>();
        locationMap.put("latitude", latitude);
        locationMap.put("longitude", longitude);
        checkInData.put("location", locationMap);

        db.collection(COLLECTION_CHECK_INS)
                .add(checkInData)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Scheduled check-in logged: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to log scheduled check-in", e);
                });
    }

    /**
     * Get analytics data for check-ins (for future dashboard integration)
     */
    public interface OnCheckInAnalyticsListener {
        void onAnalyticsLoaded(int totalCheckIns, int responseCount, int timeoutCount, double avgResponseTime);
        void onError(String error);
    }

    public static void getCheckInAnalytics(OnCheckInAnalyticsListener listener) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            listener.onError("No authenticated user");
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection(COLLECTION_CHECK_INS)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int totalCheckIns = querySnapshot.size();
                    int responseCount = 0;
                    int timeoutCount = 0;
                    double totalResponseTime = 0;

                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Boolean responded = doc.getBoolean("responded");
                        Long responseTime = doc.getLong("responseTimeSeconds");

                        if (responded != null && responded) {
                            responseCount++;
                            if (responseTime != null) {
                                totalResponseTime += responseTime;
                            }
                        } else {
                            timeoutCount++;
                        }
                    }

                    double avgResponseTime = responseCount > 0 ? totalResponseTime / responseCount : 0;

                    listener.onAnalyticsLoaded(totalCheckIns, responseCount, timeoutCount, avgResponseTime);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch check-in analytics", e);
                    listener.onError(e.getMessage());
                });
    }
}
