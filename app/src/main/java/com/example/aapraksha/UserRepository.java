package com.example.aapraksha;

import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * UserRepository - Handles all user-related Firestore operations
 * Automatically creates user profile and subcollections on signup
 */
public class UserRepository {
    private static final String TAG = "UserRepository";
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    public UserRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    /**
     * Create user profile automatically when user signs up
     * Also creates emergency_contacts, alert_history, and settings subcollections
     * 
     * @param fullName User's full name
     * @param email User's email
     * @param phone User's phone number
     * @param emergencyPin 4-digit emergency PIN
     * @param onSuccess Callback on success
     * @param onFailure Callback on failure
     */
    public void createUserProfile(String fullName, String email, String phone, 
                                   String emergencyPin, OnCompleteListener onSuccess, 
                                   OnErrorListener onFailure) {
        
        String userId = auth.getCurrentUser().getUid(); // Get authenticated user ID
        
        Log.d(TAG, "Creating user profile for: " + userId);
        
        // Use WriteBatch for atomic operations
        WriteBatch batch = db.batch();
        
        // 1. Create main user document
        Map<String, Object> userData = new HashMap<>();
        userData.put("email", email);
        userData.put("phone", phone);
        userData.put("fullName", fullName);
        userData.put("accountStatus", "ACTIVE");
        userData.put("emergencyPin", emergencyPin); // Should be encrypted
        userData.put("profilePhotoUrl", "");
        userData.put("memberSince", Timestamp.now());
        userData.put("createdAt", Timestamp.now());
        userData.put("updatedAt", Timestamp.now());
        userData.put("lastLoginAt", Timestamp.now());
        
        // SOS Status
        Map<String, Object> sosStatus = new HashMap<>();
        sosStatus.put("isActive", false);
        sosStatus.put("triggeredAt", null);
        sosStatus.put("lastSosAlertId", "");
        sosStatus.put("pinAttempts", 0);
        userData.put("sosStatus", sosStatus);
        
        batch.set(db.collection("users").document(userId), userData);
        
        // 2. Create emergency_contacts subcollection (empty initially)
        createEmergencyContactsSubcollection(batch, userId);
        
        // 3. Create alert_history subcollection (empty initially)
        createAlertHistorySubcollection(batch, userId);
        
        // 4. Create settings subcollection (with defaults)
        createSettingsSubcollection(batch, userId);
        
        // Commit batch
        batch.commit()
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "User profile created successfully: " + userId);
                if (onSuccess != null) {
                    onSuccess.onComplete(userId);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error creating user profile", e);
                if (onFailure != null) {
                    onFailure.onError(e.getMessage());
                }
            });
    }

    /**
     * Create empty emergency_contacts subcollection with document structure
     */
    private void createEmergencyContactsSubcollection(WriteBatch batch, String userId) {
        Map<String, Object> emptyContact = new HashMap<>();
        emptyContact.put("placeholder", true); // Temporary placeholder
        
        batch.set(db.collection("users").document(userId)
            .collection("emergencyContacts").document("_init"), emptyContact);
    }

    /**
     * Create empty alert_history subcollection
     */
    private void createAlertHistorySubcollection(WriteBatch batch, String userId) {
        Map<String, Object> emptyHistory = new HashMap<>();
        emptyHistory.put("placeholder", true);
        
        batch.set(db.collection("users").document(userId)
            .collection("alert_history").document("_init"), emptyHistory);
    }

    /**
     * Create settings subcollection with default preferences
     */
    private void createSettingsSubcollection(WriteBatch batch, String userId) {
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
        
        batch.set(db.collection("users").document(userId)
            .collection("settings").document("settings_1"), settings);
    }

    /**
     * Add emergency contact for user
     */
    public void addEmergencyContact(String userId, String name, String phone, 
                                     String relation, boolean isPriority,
                                     OnCompleteListener onSuccess, OnErrorListener onFailure) {
        
        Log.d(TAG, "Adding emergency contact for user: " + userId);
        
        Map<String, Object> contact = new HashMap<>();
        contact.put("name", name);
        contact.put("phone", phone);
        contact.put("relation", relation);
        contact.put("email", "");
        contact.put("contactType", isPriority ? "PRIORITY" : "NORMAL");
        contact.put("isPriority", isPriority);
        contact.put("status", "ACTIVE");
        contact.put("addedAt", Timestamp.now());
        contact.put("lastNotifiedAt", null);
        contact.put("totalNotifications", 0);
        
        // Notification methods
        List<String> notificationMethods = new ArrayList<>();
        notificationMethods.add("CALL");
        notificationMethods.add("SMS");
        notificationMethods.add("PUSH_NOTIFICATION");
        contact.put("notificationMethod", notificationMethods);
        
        // Notification preferences
        Map<String, Object> notifPref = new HashMap<>();
        notifPref.put("enableImmediateNotification", true);
        notifPref.put("enableSMS", true);
        notifPref.put("enableCall", true);
        notifPref.put("enablePush", true);
        notifPref.put("enableEmail", false);
        contact.put("notificationPreference", notifPref);
        
        contact.put("responseHistory", new ArrayList<>());
        
        // Use document ID based on whether it's priority or not
        String contactId = isPriority ? "priority_contact" : "contact_" + System.currentTimeMillis();
        
        db.collection("users").document(userId)
            .collection("emergencyContacts").document(contactId)
            .set(contact)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Emergency contact added: " + contactId);
                if (onSuccess != null) {
                    onSuccess.onComplete(contactId);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error adding emergency contact", e);
                if (onFailure != null) {
                    onFailure.onError(e.getMessage());
                }
            });
    }

    /**
     * Update user profile
     */
    public void updateUserProfile(String userId, String fullName, String email, 
                                   String phone, OnCompleteListener onSuccess, 
                                   OnErrorListener onFailure) {
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", fullName);
        updates.put("email", email);
        updates.put("phone", phone);
        updates.put("updatedAt", Timestamp.now());
        
        db.collection("users").document(userId).update(updates)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "User profile updated: " + userId);
                if (onSuccess != null) {
                    onSuccess.onComplete(userId);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error updating user profile", e);
                if (onFailure != null) {
                    onFailure.onError(e.getMessage());
                }
            });
    }

    /**
     * Get user profile
     */
    public void getUserProfile(String userId, OnUserFetchListener listener) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    User user = documentSnapshot.toObject(User.class);
                    user.setUserId(userId);
                    listener.onSuccess(user);
                } else {
                    listener.onError("User not found");
                }
            })
            .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    /**
     * Delete user account
     */
    public void deleteUserAccount(String userId, OnCompleteListener onSuccess, 
                                   OnErrorListener onFailure) {
        
        db.collection("users").document(userId).delete()
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "User deleted: " + userId);
                if (onSuccess != null) {
                    onSuccess.onComplete(userId);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error deleting user", e);
                if (onFailure != null) {
                    onFailure.onError(e.getMessage());
                }
            });
    }

    /**
     * Check if user has any emergency contacts
     */
    public void checkHasEmergencyContacts(String userId, OnContactCheckListener listener) {
        db.collection("users").document(userId).collection("emergencyContacts").get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                boolean hasContacts = false;
                for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                    if (!doc.getId().equals("_init") && doc.exists()) {
                        hasContacts = true;
                        break;
                    }
                }
                if (listener != null) {
                    listener.onCheckComplete(hasContacts);
                }
            })
            .addOnFailureListener(e -> {
                if (listener != null) {
                    listener.onError(e.getMessage());
                }
            });
    }

    // ========== Callback Interfaces ==========

    public interface OnCompleteListener {
        void onComplete(String userId);
    }

    public interface OnErrorListener {
        void onError(String errorMessage);
    }

    public interface OnUserFetchListener {
        void onSuccess(User user);
        void onError(String errorMessage);
    }

    public interface OnContactCheckListener {
        void onCheckComplete(boolean hasContacts);
        void onError(String errorMessage);
    }
}
