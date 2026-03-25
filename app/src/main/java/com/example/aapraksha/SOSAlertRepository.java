package com.example.aapraksha;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.WriteBatch;
import com.example.aapraksha.models.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SOSAlertRepository {
    private static final String TAG = "SOSAlertRepository";
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public interface OnAlertCreatedListener {
        void onSuccess(String alertId);
        void onError(String errorMessage);
    }

    public interface OnAlertStatusChangedListener {
        void onSuccess();
        void onError(String errorMessage);
    }

    public interface OnAlertCancelledListener {
        void onSuccess();
        void onError(String errorMessage);
    }

    public interface OnAlertFetchListener {
        void onSuccess(SOSAlert alert);
        void onError(String errorMessage);
    }

    public interface OnEmergencyContactsListener {
        void onSuccess(List<EmergencyContact> contacts);
        void onError(String errorMessage);
    }

    public SOSAlertRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    /**
     * Trigger SOS alert with current location
     * Creates alert, notifies emergency contacts, starts location tracking
     */
    public void triggerSOS(double latitude, double longitude, double accuracy,
                          OnAlertCreatedListener onSuccess, OnAlertStatusChangedListener onFailure) {
        String userId = auth.getCurrentUser().getUid();

        try {
            // Get emergency contacts first
            getEmergencyContacts(userId, new OnEmergencyContactsListener() {
                @Override
                public void onSuccess(List<EmergencyContact> contacts) {
                    // Create SOS alert document
                    createSOSAlert(userId, latitude, longitude, accuracy, contacts, onSuccess, onFailure);
                }

                @Override
                public void onError(String errorMessage) {
                    Log.e(TAG, "Failed to fetch emergency contacts: " + errorMessage);
                    onFailure.onError("Failed to fetch emergency contacts: " + errorMessage);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error triggering SOS: " + e.getMessage());
            onFailure.onError("Error triggering SOS: " + e.getMessage());
        }
    }

    /**
     * Create SOS alert in Firestore
     */
    private void createSOSAlert(String userId, double latitude, double longitude,
                               double accuracy, List<EmergencyContact> emergencyContacts,
                               OnAlertCreatedListener onSuccess, OnAlertStatusChangedListener onFailure) {
        try {
            DocumentReference alertRef = db.collection("alerts").document();
            String alertId = alertRef.getId();

            // Build initial location data
            Map<String, Object> locationData = new HashMap<>();
            locationData.put("latitude", latitude);
            locationData.put("longitude", longitude);
            locationData.put("accuracy", accuracy);
            locationData.put("address", "Getting address...");
            locationData.put("timestamp", Timestamp.now());

            // Build initial SOS alert data
            Map<String, Object> alertData = new HashMap<>();
            alertData.put("alertId", alertId);
            alertData.put("userId", userId);
            alertData.put("status", "ACTIVE"); // ACTIVE, RESOLVED, CANCELLED, EXPIRED
            alertData.put("type", "EMERGENCY_SOS");
            alertData.put("createdAt", Timestamp.now());
            alertData.put("updatedAt", Timestamp.now());
            alertData.put("location", locationData);

            // Add emergency contacts that were notified
            List<Map<String, Object>> contactsList = new ArrayList<>();
            for (EmergencyContact contact : emergencyContacts) {
                Map<String, Object> contactData = new HashMap<>();
                contactData.put("contactId", contact.getContactId());
                contactData.put("name", contact.getName());
                contactData.put("phone", contact.getPhone());
                contactData.put("relation", contact.getRelation());
                contactData.put("isPriority", contact.isPriority());
                contactData.put("notificationStatus", "PENDING");
                contactData.put("notificationMethods", contact.getNotificationMethod());
                contactData.put("notifiedAt", Timestamp.now());
                contactData.put("respondedAt", null);
                contactData.put("response", null);
                contactsList.add(contactData);
            }
            alertData.put("notificationsToContacts", contactsList);

            // Device information
            Map<String, Object> deviceData = new HashMap<>();
            deviceData.put("deviceId", android.provider.Settings.Secure.getString(null, android.provider.Settings.Secure.ANDROID_ID));
            deviceData.put("deviceName", android.os.Build.DEVICE);
            deviceData.put("osVersion", android.os.Build.VERSION.SDK_INT);
            deviceData.put("batteryLevel", 85); // TODO: Get actual battery level
            deviceData.put("networkType", "WIFI"); // TODO: Get actual network type
            deviceData.put("signalStrength", 0); // TODO: Get actual signal strength
            alertData.put("deviceInfo", deviceData);

            // SOS status details
            Map<String, Object> sosStatusData = new HashMap<>();
            sosStatusData.put("isSOSActive", true);
            sosStatusData.put("lastSOSTime", Timestamp.now());
            sosStatusData.put("numberOfSOSTriggered", 1); // TODO: Get count from user stats
            sosStatusData.put("emergencyPin", null); // PIN only needed for cancellation
            alertData.put("sosStatus", sosStatusData);

            // Additional fields
            alertData.put("mediaAttachments", new ArrayList<>()); // Images/videos
            alertData.put("voiceRecording", null);
            alertData.put("notes", "");
            alertData.put("tags", new ArrayList<>()); // User-added tags

            // Write alert to Firestore
            alertRef.set(alertData).addOnSuccessListener(documentReference -> {
                Log.d(TAG, "SOS alert created successfully: " + alertId);
                
                // Update user's SOS status
                updateUserSOSStatus(userId, true, alertId);
                
                // Return alert ID
                onSuccess.onSuccess(alertId);
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Failed to create SOS alert: " + e.getMessage());
                onFailure.onError("Failed to create SOS alert: " + e.getMessage());
            });

        } catch (Exception e) {
            Log.e(TAG, "Error in createSOSAlert: " + e.getMessage());
            onFailure.onError("Error in createSOSAlert: " + e.getMessage());
        }
    }

    /**
     * Update alert location during active SOS
     */
    public void updateAlertLocation(String alertId, double latitude, double longitude,
                                   double accuracy, OnAlertStatusChangedListener listener) {
        try {
            Map<String, Object> locationData = new HashMap<>();
            locationData.put("latitude", latitude);
            locationData.put("longitude", longitude);
            locationData.put("accuracy", accuracy);
            locationData.put("timestamp", Timestamp.now());

            db.collection("alerts").document(alertId)
                    .update("location", locationData)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Alert location updated: " + alertId);
                        listener.onSuccess();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to update alert location: " + e.getMessage());
                        listener.onError(e.getMessage());
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error updating alert location: " + e.getMessage());
            listener.onError(e.getMessage());
        }
    }

    /**
     * Cancel active SOS alert
     */
    public void cancelSOS(String alertId, String userId, String emergencyPin,
                         OnAlertCancelledListener listener) {
        try {
            // Get user to verify PIN
            db.collection("users").document(userId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null && user.getEmergencyPin() != null &&
                                user.getEmergencyPin().equals(emergencyPin)) {

                            // Update alert status to CANCELLED
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("status", "CANCELLED");
                            updates.put("updatedAt", Timestamp.now());
                            updates.put("cancelledAt", Timestamp.now());
                            updates.put("cancelledBy", userId);

                            db.collection("alerts").document(alertId)
                                    .update(updates)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "SOS alert cancelled: " + alertId);
                                        
                                        // Update user's SOS status
                                        updateUserSOSStatus(userId, false, alertId);
                                        
                                        listener.onSuccess();
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Failed to cancel SOS: " + e.getMessage());
                                        listener.onError(e.getMessage());
                                    });
                        } else {
                            listener.onError("Invalid emergency PIN");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to fetch user: " + e.getMessage());
                        listener.onError("Failed to verify PIN: " + e.getMessage());
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error cancelling SOS: " + e.getMessage());
            listener.onError(e.getMessage());
        }
    }

    /**
     * Get active SOS alert for current user
     */
    public void getActiveSOS(OnAlertFetchListener listener) {
        String userId = auth.getCurrentUser().getUid();
        try {
            db.collection("alerts")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("status", "ACTIVE")
                    .limit(1)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (querySnapshot.isEmpty()) {
                            Log.d(TAG, "No active SOS found for user");
                            listener.onError("No active SOS alert");
                        } else {
                            SOSAlert alert = querySnapshot.getDocuments().get(0).toObject(SOSAlert.class);
                            Log.d(TAG, "Active SOS fetched: " + alert.getAlertId());
                            listener.onSuccess(alert);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to fetch active SOS: " + e.getMessage());
                        listener.onError(e.getMessage());
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error fetching active SOS: " + e.getMessage());
            listener.onError(e.getMessage());
        }
    }

    private void getEmergencyContacts(String userId, OnEmergencyContactsListener listener) {
        db.collection("users").document(userId).collection("emergency_contacts")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<EmergencyContact> contacts = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        EmergencyContact contact = doc.toObject(EmergencyContact.class);
                        if (contact != null) {
                            contacts.add(contact);
                        }
                    }
                    listener.onSuccess(contacts);
                })
                .addOnFailureListener(e -> {
                    listener.onError(e.getMessage());
                });
    }

    private void updateUserSOSStatus(String userId, boolean isActive, String alertId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("sosStatus.isSOSActive", isActive);
        updates.put("sosStatus.currentAlertId", isActive ? alertId : null);
        updates.put("sosStatus.lastSOSTime", Timestamp.now());

        db.collection("users").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User SOS status updated"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update user SOS status: " + e.getMessage()));
    }
}
