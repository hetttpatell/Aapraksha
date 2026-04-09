package com.example.aapraksha;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ListenerRegistration;

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

    public interface OnAlertsListFetchListener {
        void onSuccess(List<SOSAlert> alerts);
        void onError(String errorMessage);
    }

    public interface OnRealtimeAlertsListener {
        void onAlertsUpdated(List<SOSAlert> alerts);
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
        if (auth.getCurrentUser() == null) {
            onFailure.onError("User not authenticated");
            return;
        }
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
            deviceData.put("deviceId", android.os.Build.MODEL);
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
        if (auth.getCurrentUser() == null) {
            listener.onError("User not authenticated");
            return;
        }
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
                            if (alert != null && alert.getAlertId() == null) {
                                alert.setAlertId(querySnapshot.getDocuments().get(0).getId());
                            }
                            Log.d(TAG, "Active SOS fetched: " + (alert != null ? alert.getAlertId() : "null"));
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
        db.collection("users").document(userId).collection("emergencyContacts")
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

    /**
     * Fetch live alerts from other users (excluding current user)
     * Returns alerts with ACTIVE or TRIGGERED status
     */
    public void fetchLiveAlerts(OnAlertsListFetchListener listener) {
        try {
            db.collection("alerts")
                    .whereIn("status", java.util.Arrays.asList("ACTIVE", "TRIGGERED"))
                    .limit(50)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        List<SOSAlert> alerts = new ArrayList<>();
                        for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            SOSAlert alert = doc.toObject(SOSAlert.class);
                            if (alert != null) {
                                if (alert.getAlertId() == null) alert.setAlertId(doc.getId());
                                alerts.add(alert);
                            }
                        }
                        
                        // Sort by createdAt descending natively to bypass Firestore index requirement
                        java.util.Collections.sort(alerts, (a1, a2) -> {
                            if (a1.getCreatedAt() == null || a2.getCreatedAt() == null) return 0;
                            return a2.getCreatedAt().compareTo(a1.getCreatedAt());
                        });

                        Log.d(TAG, "Live alerts fetched: " + alerts.size());
                        listener.onSuccess(alerts);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to fetch live alerts: " + e.getMessage());
                        listener.onError(e.getMessage());
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error fetching live alerts: " + e.getMessage());
            listener.onError(e.getMessage());
        }
    }

    /**
     * Fetch user's own SOS alert history
     */
    public void fetchUserAlerts(String userId, int limit, OnAlertsListFetchListener listener) {
        try {
            db.collection("alerts")
                    .whereEqualTo("userId", userId)
                    .orderBy("sosData.triggeredAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(limit)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        List<SOSAlert> alerts = new ArrayList<>();
                        for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            SOSAlert alert = doc.toObject(SOSAlert.class);
                            if (alert != null) {
                                if (alert.getAlertId() == null) alert.setAlertId(doc.getId());
                                alerts.add(alert);
                            }
                        }
                        Log.d(TAG, "User alerts fetched from alerts: " + alerts.size());
                        listener.onSuccess(alerts);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to fetch user alerts: " + e.getMessage());
                        // If index is missing, fallback to client side sorting
                        if (e.getMessage() != null && e.getMessage().contains("FAILED_PRECONDITION")) {
                           Log.d(TAG, "Index missing, falling back to client-side sort");
                           fetchUserAlertsWithoutOrder(userId, limit, listener);
                        } else {
                           listener.onError(e.getMessage());
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error fetching user alerts: " + e.getMessage());
            listener.onError(e.getMessage());
        }
    }

    private void fetchUserAlertsWithoutOrder(String userId, int limit, OnAlertsListFetchListener listener) {
        db.collection("alerts")
                .whereEqualTo("userId", userId)
                .limit(limit)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<SOSAlert> alerts = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        SOSAlert alert = doc.toObject(SOSAlert.class);
                        if (alert != null) {
                            if (alert.getAlertId() == null) alert.setAlertId(doc.getId());
                            alerts.add(alert);
                        }
                    }
                    java.util.Collections.sort(alerts, (a1, a2) -> {
                        if (a1.getCreatedAt() == null || a2.getCreatedAt() == null) return 0;
                        return a2.getCreatedAt().compareTo(a1.getCreatedAt());
                    });
                    listener.onSuccess(alerts);
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    /**
     * Fetch alerts by status (for filtering history)
     */
    public void fetchAlertsByStatus(String userId, String status, OnAlertsListFetchListener listener) {
        try {
            db.collection("users").document(userId).collection("alert_history")
                    .whereEqualTo("alertStatus", status)
                    .limit(50)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        List<SOSAlert> alerts = new ArrayList<>();
                        for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            AlertHistory history = doc.toObject(AlertHistory.class);
                            if (history != null) {
                                SOSAlert alert = convertAlertHistoryToSOSAlert(history);
                                alerts.add(alert);
                            }
                        }
                        
                        // Sort by createdAt descending natively to bypass Firestore index requirement
                        java.util.Collections.sort(alerts, (a1, a2) -> {
                            if (a1.getCreatedAt() == null || a2.getCreatedAt() == null) return 0;
                            return a2.getCreatedAt().compareTo(a1.getCreatedAt());
                        });
                        
                        Log.d(TAG, "Filtered alerts fetched: " + alerts.size());
                        listener.onSuccess(alerts);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to fetch filtered alerts: " + e.getMessage());
                        listener.onError(e.getMessage());
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error fetching filtered alerts: " + e.getMessage());
            listener.onError(e.getMessage());
        }
    }

    /**
     * Fetch single alert by ID
     */
    public void getAlertById(String alertId, OnAlertFetchListener listener) {
        try {
            db.collection("alerts").document(alertId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            SOSAlert alert = documentSnapshot.toObject(SOSAlert.class);
                            if (alert != null && alert.getAlertId() == null) {
                                alert.setAlertId(documentSnapshot.getId());
                            }
                            Log.d(TAG, "Alert fetched by ID: " + alertId);
                            listener.onSuccess(alert);
                        } else {
                            listener.onError("Alert not found");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to fetch alert by ID: " + e.getMessage());
                        listener.onError(e.getMessage());
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error fetching alert by ID: " + e.getMessage());
            listener.onError(e.getMessage());
        }
    }

    /**
     * Listen to live alerts in real-time (excluding current user)
     * Returns ListenerRegistration to manage listener lifecycle
     */
    public ListenerRegistration listenToLiveAlerts(OnRealtimeAlertsListener listener) {
        try {
            return db.collection("alerts")
                    .whereIn("status", java.util.Arrays.asList("ACTIVE", "TRIGGERED"))
                    .limit(50)
                    .addSnapshotListener((querySnapshot, error) -> {
                        if (error != null) {
                            Log.e(TAG, "Error listening to live alerts: " + error.getMessage());
                            listener.onError(error.getMessage());
                            return;
                        }

                        if (querySnapshot != null) {
                            List<SOSAlert> alerts = new ArrayList<>();
                            for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                SOSAlert alert = doc.toObject(SOSAlert.class);
                                if (alert != null) {
                                    if (alert.getAlertId() == null) alert.setAlertId(doc.getId());
                                    alerts.add(alert);
                                }
                            }
                            
                            // Sort by createdAt descending natively to bypass Firestore index requirement
                            java.util.Collections.sort(alerts, (a1, a2) -> {
                                if (a1.getCreatedAt() == null || a2.getCreatedAt() == null) return 0;
                                return a2.getCreatedAt().compareTo(a1.getCreatedAt());
                            });

                            Log.d(TAG, "Real-time live alerts updated: " + alerts.size());
                            listener.onAlertsUpdated(alerts);
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error setting up live alerts listener: " + e.getMessage());
            listener.onError(e.getMessage());
            return null;
        }
    }

    /**
     * Listen to user's alert history in real-time
     */
    public ListenerRegistration listenToUserAlerts(String userId, OnRealtimeAlertsListener listener) {
        try {
            return db.collection("alerts")
                    .whereEqualTo("userId", userId)
                    .limit(50)
                    .addSnapshotListener((querySnapshot, error) -> {
                        if (error != null) {
                            Log.e(TAG, "Error listening to user alerts: " + error.getMessage());
                            listener.onError(error.getMessage());
                            return;
                        }

                        if (querySnapshot != null) {
                            List<SOSAlert> alerts = new ArrayList<>();
                            for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                SOSAlert alert = doc.toObject(SOSAlert.class);
                                if (alert != null) {
                                    if (alert.getAlertId() == null) alert.setAlertId(doc.getId());
                                    alerts.add(alert);
                                }
                            }
                            // Sort locally to bypass index requirement
                            java.util.Collections.sort(alerts, (a1, a2) -> {
                                if (a1.getCreatedAt() == null || a2.getCreatedAt() == null) return 0;
                                return a2.getCreatedAt().compareTo(a1.getCreatedAt());
                            });
                            
                            Log.d(TAG, "Real-time user alerts updated from alerts collection: " + alerts.size());
                            listener.onAlertsUpdated(alerts);
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error setting up user alerts listener: " + e.getMessage());
            listener.onError(e.getMessage());
            return null;
        }
    }

    /**
     * Listen to a single alert by ID in real-time
     */
    public ListenerRegistration listenToAlertById(String alertId, OnAlertFetchListener listener) {
        try {
            return db.collection("alerts").document(alertId)
                    .addSnapshotListener((documentSnapshot, error) -> {
                        if (error != null) {
                            Log.e(TAG, "Error listening to alert by ID: " + error.getMessage());
                            listener.onError(error.getMessage());
                            return;
                        }

                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            SOSAlert alert = documentSnapshot.toObject(SOSAlert.class);
                            if (alert != null) {
                                if (alert.getAlertId() == null) alert.setAlertId(documentSnapshot.getId());
                            }
                            Log.d(TAG, "Real-time alert updated by ID: " + alertId);
                            listener.onSuccess(alert);
                        } else {
                            listener.onError("Alert not found");
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error setting up alert listener by ID: " + e.getMessage());
            listener.onError(e.getMessage());
            return null;
        }
    }

    /**
     * Convert AlertHistory to SOSAlert for consistent UI display
     */
    private SOSAlert convertAlertHistoryToSOSAlert(AlertHistory history) {
        SOSAlert alert = new SOSAlert();
        alert.setAlertId(history.getAlertId());
        alert.setAlertType(history.getAlertType());
        alert.setStatus(history.getAlertStatus());
        alert.setCreatedAt(history.getTiming() != null ? history.getTiming().getTriggeredAt() : Timestamp.now());
        
        if (history.getLocation() != null) {
            SOSAlert.LocationData locationData = new SOSAlert.LocationData();
            locationData.setLatitude(history.getLocation().getLatitude());
            locationData.setLongitude(history.getLocation().getLongitude());
            locationData.setAddress(history.getLocation().getAddress());
            locationData.setAccuracy(history.getLocation().getAccuracy());
            alert.setLocation(locationData);
        }

        // Set contact info from history if available
        if (history.getContactsNotified() != null) {
            List<Object> notifList = new ArrayList<>();
            if (history.getContactsNotified().getNotificationsList() != null) {
                for (AlertHistory.ContactNotificationInfo.NotificationDetail detail : history.getContactsNotified().getNotificationsList()) {
                    Map<String, Object> notif = new HashMap<>();
                    notif.put("name", detail.getContactName());
                    notif.put("phone", detail.getContactPhone());
                    notif.put("notifiedAt", detail.getNotifiedAt());
                    notif.put("responseStatus", detail.getResponseStatus());
                    notifList.add(notif);
                }
            }
            alert.setNotificationsToContacts(notifList);
        }

        if (history.getDeviceInfo() != null) {
            Map<String, Object> deviceMap = new HashMap<>();
            deviceMap.put("deviceId", history.getDeviceInfo().getDeviceId());
            deviceMap.put("osVersion", history.getDeviceInfo().getOsVersion());
            deviceMap.put("batteryLevel", history.getDeviceInfo().getBatteryLevel());
            deviceMap.put("networkType", history.getDeviceInfo().getNetworkType());
            deviceMap.put("signalStrength", history.getDeviceInfo().getSignalStrength());
            alert.setDeviceInfo(deviceMap);
        }

        return alert;
    }

}
