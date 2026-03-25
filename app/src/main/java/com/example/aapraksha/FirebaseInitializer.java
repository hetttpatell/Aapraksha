package com.example.aapraksha;

import android.content.Context;
import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FirebaseInitializer - Automatically creates all Firestore collections and Realtime Database structure
 * Runs once on first app launch to set up the backend
 */
public class FirebaseInitializer {
    private static final String TAG = "FirebaseInitializer";
    private static final String INIT_PREFERENCE = "firebase_initialized";
    
    private FirebaseFirestore db;
    private FirebaseDatabase realtimeDb;
    private Context context;

    public FirebaseInitializer(Context context) {
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
        this.realtimeDb = FirebaseDatabase.getInstance();
    }

    /**
     * Initialize Firebase collections and sample data
     * Call this once on app startup
     */
    public void initializeFirebase() {
        if (isAlreadyInitialized()) {
            Log.d(TAG, "Firebase already initialized");
            return;
        }

        Log.d(TAG, "Starting Firebase initialization...");
        
        // Create all collections
        createUsersCollection();
        createSOSAlertsCollection();
        createNetworkAlertsCollection();
        createDeviceTokensCollection();
        createActivityLogsCollection();
        createFeedbackCollection();
        
        // Create Realtime Database structure
        createRealtimeDatabaseStructure();
        
        // Mark as initialized
        markAsInitialized();
        
        Log.d(TAG, "Firebase initialization complete!");
    }

    // ========== FIRESTORE COLLECTIONS ==========

    /**
     * Create USERS collection with sample data and subcollections
     */
    private void createUsersCollection() {
        Log.d(TAG, "Creating USERS collection...");
        
        // Get current user ID (or use test user)
        String userId = "user123"; // Use auto-generated ID in production
        
        Map<String, Object> userData = new HashMap<>();
        userData.put("email", "rajesh@example.com");
        userData.put("phone", "9876543210");
        userData.put("fullName", "Rajesh Kumar");
        userData.put("accountStatus", "ACTIVE");
        userData.put("emergencyPin", "encrypted_1234");
        userData.put("profilePhotoUrl", "");
        userData.put("memberSince", Timestamp.now());
        userData.put("createdAt", Timestamp.now());
        userData.put("updatedAt", Timestamp.now());
        userData.put("lastLoginAt", Timestamp.now());
        
        // Add SOS Status
        Map<String, Object> sosStatus = new HashMap<>();
        sosStatus.put("isActive", false);
        sosStatus.put("triggeredAt", null);
        sosStatus.put("lastSosAlertId", "");
        sosStatus.put("pinAttempts", 0);
        userData.put("sosStatus", sosStatus);
        
        db.collection("users").document(userId).set(userData)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "User document created");
                createEmergencyContactsSubcollection(userId);
                createAlertHistorySubcollection(userId);
                createSettingsSubcollection(userId);
            })
            .addOnFailureListener(e -> Log.e(TAG, "Error creating user", e));
    }

    /**
     * Create EMERGENCY_CONTACTS subcollection under user
     */
    private void createEmergencyContactsSubcollection(String userId) {
        Log.d(TAG, "Creating EMERGENCY_CONTACTS subcollection...");
        
        // Sample PRIORITY contact
        Map<String, Object> priorityContact = new HashMap<>();
        priorityContact.put("name", "Mother");
        priorityContact.put("phone", "9876543210");
        priorityContact.put("relation", "Mother");
        priorityContact.put("email", "mother@example.com");
        priorityContact.put("contactType", "PRIORITY");
        priorityContact.put("isPriority", true);
        priorityContact.put("status", "ACTIVE");
        priorityContact.put("addedAt", Timestamp.now());
        priorityContact.put("lastNotifiedAt", null);
        priorityContact.put("totalNotifications", 0);
        
        // Notification methods
        List<String> notificationMethods = new ArrayList<>();
        notificationMethods.add("CALL");
        notificationMethods.add("SMS");
        notificationMethods.add("PUSH_NOTIFICATION");
        priorityContact.put("notificationMethod", notificationMethods);
        
        // Notification preferences
        Map<String, Object> notifPref = new HashMap<>();
        notifPref.put("enableImmediateNotification", true);
        notifPref.put("enableSMS", true);
        notifPref.put("enableCall", true);
        notifPref.put("enablePush", true);
        notifPref.put("enableEmail", false);
        priorityContact.put("notificationPreference", notifPref);
        
        // Response history (empty initially)
        priorityContact.put("responseHistory", new ArrayList<>());
        
        db.collection("users").document(userId).collection("emergency_contacts")
            .document("contact_1").set(priorityContact)
            .addOnSuccessListener(aVoid -> Log.d(TAG, "Priority contact created"))
            .addOnFailureListener(e -> Log.e(TAG, "Error creating priority contact", e));
        
        // Sample NORMAL contact
        Map<String, Object> normalContact = new HashMap<>();
        normalContact.put("name", "Best Friend");
        normalContact.put("phone", "8765432109");
        normalContact.put("relation", "Friend");
        normalContact.put("email", "");
        normalContact.put("contactType", "NORMAL");
        normalContact.put("isPriority", false);
        normalContact.put("status", "ACTIVE");
        normalContact.put("addedAt", Timestamp.now());
        normalContact.put("totalNotifications", 0);
        
        List<String> normalNotifMethods = new ArrayList<>();
        normalNotifMethods.add("SMS");
        normalNotifMethods.add("PUSH_NOTIFICATION");
        normalContact.put("notificationMethod", normalNotifMethods);
        
        Map<String, Object> normalNotifPref = new HashMap<>();
        normalNotifPref.put("enableImmediateNotification", true);
        normalNotifPref.put("enableSMS", true);
        normalNotifPref.put("enableCall", false);
        normalNotifPref.put("enablePush", true);
        normalNotifPref.put("enableEmail", false);
        normalContact.put("notificationPreference", normalNotifPref);
        
        normalContact.put("responseHistory", new ArrayList<>());
        
        db.collection("users").document(userId).collection("emergency_contacts")
            .document("contact_2").set(normalContact)
            .addOnSuccessListener(aVoid -> Log.d(TAG, "Normal contact created"))
            .addOnFailureListener(e -> Log.e(TAG, "Error creating normal contact", e));
    }

    /**
     * Create ALERT_HISTORY subcollection under user
     */
    private void createAlertHistorySubcollection(String userId) {
        Log.d(TAG, "Creating ALERT_HISTORY subcollection...");
        
        Map<String, Object> alertHistory = new HashMap<>();
        alertHistory.put("alertId", "alert_abc123");
        alertHistory.put("alertType", "SOS");
        alertHistory.put("alertStatus", "RESOLVED");
        alertHistory.put("notes", "Test alert - no emergency");
        alertHistory.put("reportedToAuthorities", false);
        
        // Timing
        Map<String, Object> timing = new HashMap<>();
        timing.put("triggeredAt", Timestamp.now());
        timing.put("cancelledAt", null);
        timing.put("resolvedAt", null);
        timing.put("totalDuration", 0);
        alertHistory.put("timing", timing);
        
        // Location
        Map<String, Object> location = new HashMap<>();
        location.put("latitude", 28.5244);
        location.put("longitude", 77.1855);
        location.put("address", "Gurgaon, Haryana");
        location.put("area", "Gurgaon");
        location.put("accuracy", 10.5);
        alertHistory.put("location", location);
        
        // Contacts notified
        Map<String, Object> contactsNotif = new HashMap<>();
        contactsNotif.put("totalCount", 1);
        contactsNotif.put("respondedCount", 0);
        contactsNotif.put("notRespondedCount", 1);
        contactsNotif.put("priorityContactResponded", false);
        contactsNotif.put("notificationsList", new ArrayList<>());
        alertHistory.put("contactsNotified", contactsNotif);
        
        // Tags
        List<String> tags = new ArrayList<>();
        tags.add("TEST_ALERT");
        alertHistory.put("tags", tags);
        
        alertHistory.put("sharedWith", new ArrayList<>());
        
        db.collection("users").document(userId).collection("alert_history")
            .document("history_1").set(alertHistory)
            .addOnSuccessListener(aVoid -> Log.d(TAG, "Alert history created"))
            .addOnFailureListener(e -> Log.e(TAG, "Error creating alert history", e));
    }

    /**
     * Create SETTINGS subcollection under user
     */
    private void createSettingsSubcollection(String userId) {
        Log.d(TAG, "Creating SETTINGS subcollection...");
        
        Map<String, Object> settings = new HashMap<>();
        settings.put("volumeButtonSosEnabled", true);
        settings.put("notificationsEnabled", true);
        settings.put("locationTrackingEnabled", true);
        settings.put("updatedAt", Timestamp.now());
        
        // Permission preferences
        Map<String, Object> permissions = new HashMap<>();
        permissions.put("microphone", true);
        permissions.put("camera", true);
        permissions.put("location", true);
        permissions.put("phone", true);
        permissions.put("contacts", true);
        settings.put("permissionPreferences", permissions);
        
        // Notification preferences
        Map<String, Object> notifPrefs = new HashMap<>();
        notifPrefs.put("enableSMS", true);
        notifPrefs.put("enableCall", true);
        notifPrefs.put("enablePush", true);
        notifPrefs.put("enableEmail", false);
        settings.put("notificationPreferences", notifPrefs);
        
        // Privacy settings
        Map<String, Object> privacy = new HashMap<>();
        privacy.put("showProfileToNetwork", true);
        privacy.put("allowLocationSharing", true);
        privacy.put("dataStorageConsent", true);
        settings.put("privacySettings", privacy);
        
        // Sound settings
        Map<String, Object> sound = new HashMap<>();
        sound.put("sosAlertVolume", 100);
        sound.put("ringtone", "system_default");
        sound.put("vibrationEnabled", true);
        settings.put("soundSettings", sound);
        
        db.collection("users").document(userId).collection("settings")
            .document("settings_1").set(settings)
            .addOnSuccessListener(aVoid -> Log.d(TAG, "Settings created"))
            .addOnFailureListener(e -> Log.e(TAG, "Error creating settings", e));
    }

    /**
     * Create SOS_ALERTS collection with sample alert
     */
    private void createSOSAlertsCollection() {
        Log.d(TAG, "Creating SOS_ALERTS collection...");
        
        Map<String, Object> sosAlert = new HashMap<>();
        sosAlert.put("userId", "user123");
        sosAlert.put("userName", "Rajesh Kumar");
        sosAlert.put("userPhone", "9876543210");
        sosAlert.put("status", "RESOLVED");
        sosAlert.put("alertMessage", "EMERGENCY! Need help at my current location.");
        sosAlert.put("visibility", "CONTACTS_ONLY");
        
        // SOS Data
        Map<String, Object> sosData = new HashMap<>();
        sosData.put("triggeredAt", Timestamp.now());
        sosData.put("cancelledAt", null);
        sosData.put("duration", 0);
        sosAlert.put("sosData", sosData);
        
        // Location
        Map<String, Object> location = new HashMap<>();
        location.put("latitude", 28.5244);
        location.put("longitude", 77.1855);
        location.put("address", "Cyber House 2, Gurgaon");
        location.put("accuracy", 12.5);
        sosAlert.put("location", location);
        
        // Alert Details
        Map<String, Object> alertDetails = new HashMap<>();
        alertDetails.put("batteryLevel", 75);
        alertDetails.put("networkType", "4G");
        alertDetails.put("osVersion", "12");
        alertDetails.put("appVersion", "1.0");
        sosAlert.put("alertDetails", alertDetails);
        
        // Notifications
        Map<String, Object> notifications = new HashMap<>();
        notifications.put("totalContactsNotified", 1);
        notifications.put("audioCount", 1);
        notifications.put("smsCount", 1);
        sosAlert.put("notifications", notifications);
        
        // Responses and media
        sosAlert.put("responses", new ArrayList<>());
        sosAlert.put("media", new ArrayList<>());
        
        db.collection("sos_alerts").document("alert_abc123").set(sosAlert)
            .addOnSuccessListener(aVoid -> Log.d(TAG, "SOS alert created"))
            .addOnFailureListener(e -> Log.e(TAG, "Error creating SOS alert", e));
    }

    /**
     * Create NETWORK_ALERTS collection
     */
    private void createNetworkAlertsCollection() {
        Log.d(TAG, "Creating NETWORK_ALERTS collection...");
        
        Map<String, Object> networkAlert = new HashMap<>();
        networkAlert.put("userId", "user456");
        networkAlert.put("userName", "Priya Singh");
        networkAlert.put("alertType", "HARASSMENT");
        networkAlert.put("description", "Suspicious person near metro station");
        networkAlert.put("severity", "HIGH");
        networkAlert.put("status", "ACTIVE");
        networkAlert.put("visibility", "PUBLIC");
        networkAlert.put("regionRadius", 2);
        networkAlert.put("reportedAt", Timestamp.now());
        
        // Location
        Map<String, Object> location = new HashMap<>();
        location.put("latitude", 28.5270);
        location.put("longitude", 77.1860);
        location.put("address", "Delhi Metro - Cyber Hub");
        networkAlert.put("location", location);
        
        networkAlert.put("mediaUrls", new ArrayList<>());
        networkAlert.put("respondedBy", new ArrayList<>());
        networkAlert.put("verifiedBy", new ArrayList<>());
        
        db.collection("network_alerts").add(networkAlert)
            .addOnSuccessListener(docRef -> Log.d(TAG, "Network alert created: " + docRef.getId()))
            .addOnFailureListener(e -> Log.e(TAG, "Error creating network alert", e));
    }

    /**
     * Create DEVICE_TOKENS collection
     */
    private void createDeviceTokensCollection() {
        Log.d(TAG, "Creating DEVICE_TOKENS collection...");
        
        Map<String, Object> deviceToken = new HashMap<>();
        deviceToken.put("userId", "user123");
        deviceToken.put("token", "fcm_token_abc123xyz");
        deviceToken.put("deviceId", "device_abc123");
        deviceToken.put("deviceName", "OnePlus 9");
        deviceToken.put("osType", "Android");
        deviceToken.put("osVersion", "12");
        deviceToken.put("appVersion", "1.0");
        deviceToken.put("isActive", true);
        deviceToken.put("registeredAt", Timestamp.now());
        deviceToken.put("lastUsedAt", Timestamp.now());
        
        db.collection("device_tokens").add(deviceToken)
            .addOnSuccessListener(docRef -> Log.d(TAG, "Device token created"))
            .addOnFailureListener(e -> Log.e(TAG, "Error creating device token", e));
    }

    /**
     * Create ACTIVITY_LOGS collection
     */
    private void createActivityLogsCollection() {
        Log.d(TAG, "Creating ACTIVITY_LOGS collection...");
        
        Map<String, Object> activityLog = new HashMap<>();
        activityLog.put("userId", "user123");
        activityLog.put("action", "APP_INITIALIZED");
        activityLog.put("description", "App initialized with Firebase collections");
        activityLog.put("status", "SUCCESS");
        activityLog.put("ipAddress", "0.0.0.0");
        activityLog.put("deviceId", "device_abc123");
        activityLog.put("timestamp", Timestamp.now());
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("alertId", "");
        metadata.put("location", "");
        activityLog.put("metadata", metadata);
        
        db.collection("activity_logs").add(activityLog)
            .addOnSuccessListener(docRef -> Log.d(TAG, "Activity log created"))
            .addOnFailureListener(e -> Log.e(TAG, "Error creating activity log", e));
    }

    /**
     * Create FEEDBACK collection
     */
    private void createFeedbackCollection() {
        Log.d(TAG, "Creating FEEDBACK collection...");
        
        Map<String, Object> feedback = new HashMap<>();
        feedback.put("userId", "user123");
        feedback.put("feedbackType", "FEATURE_REQUEST");
        feedback.put("title", "Real-time tracking feature");
        feedback.put("description", "Would like real-time location tracking during SOS");
        feedback.put("rating", 4);
        feedback.put("appVersion", "1.0");
        feedback.put("osVersion", "12");
        feedback.put("status", "OPEN");
        feedback.put("createdAt", Timestamp.now());
        feedback.put("adminNotes", "");
        
        db.collection("feedback").add(feedback)
            .addOnSuccessListener(docRef -> Log.d(TAG, "Feedback created"))
            .addOnFailureListener(e -> Log.e(TAG, "Error creating feedback", e));
    }

    // ========== REALTIME DATABASE STRUCTURE ==========

    /**
     * Create Realtime Database structure for location tracking
     */
    private void createRealtimeDatabaseStructure() {
        Log.d(TAG, "Creating REALTIME DATABASE structure...");
        
        // Create active_locations node
        Map<String, Object> userLocation = new HashMap<>();
        userLocation.put("latitude", 28.5244);
        userLocation.put("longitude", 77.1855);
        userLocation.put("accuracy", 12.5);
        userLocation.put("altitude", 215.5);
        userLocation.put("heading", 45.0);
        userLocation.put("speed", 0.0);
        userLocation.put("timestamp", System.currentTimeMillis());
        userLocation.put("alertId", "alert_abc123");
        userLocation.put("isActive", false);
        userLocation.put("updateFrequency", 5);
        userLocation.put("provider", "GPS");
        userLocation.put("batteryLevel", 72);
        userLocation.put("accuracy_status", "HIGH");
        
        realtimeDb.getReference("active_locations/user123")
            .setValue(userLocation)
            .addOnSuccessListener(aVoid -> Log.d(TAG, "Active location created"))
            .addOnFailureListener(e -> Log.e(TAG, "Error creating active location", e));
        
        // Create contacted_locations node
        Map<String, Object> contactLocation = new HashMap<>();
        contactLocation.put("userId", "user123");
        contactLocation.put("latitude", 28.5240);
        contactLocation.put("longitude", 77.1850);
        contactLocation.put("timestamp", System.currentTimeMillis());
        contactLocation.put("alertId", "alert_abc123");
        contactLocation.put("status", "STOPPED");
        
        realtimeDb.getReference("contacted_locations/contact_1")
            .setValue(contactLocation)
            .addOnSuccessListener(aVoid -> Log.d(TAG, "Contacted location created"))
            .addOnFailureListener(e -> Log.e(TAG, "Error creating contacted location", e));
    }

    // ========== HELPER METHODS ==========

    /**
     * Check if Firebase has already been initialized
     */
    private boolean isAlreadyInitialized() {
        return context.getSharedPreferences("aapraksha_prefs", Context.MODE_PRIVATE)
            .getBoolean(INIT_PREFERENCE, false);
    }

    /**
     * Mark Firebase as initialized
     */
    private void markAsInitialized() {
        context.getSharedPreferences("aapraksha_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean(INIT_PREFERENCE, true)
            .apply();
    }
}
