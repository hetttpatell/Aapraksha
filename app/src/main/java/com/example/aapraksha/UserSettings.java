package com.example.aapraksha;

import com.google.firebase.Timestamp;
import java.io.Serializable;

/**
 * UserSettings model for Firestore subcollection: users/{userId}/settings
 * Stores user preferences, permissions, and notification settings
 */
public class UserSettings implements Serializable {
    private String settingId;
    private String userId;
    
    // SOS & Alert Settings
    private boolean volumeButtonSosEnabled; // Can trigger SOS with volume button
    private boolean notificationsEnabled;
    private boolean locationTrackingEnabled;
    
    // Permission Preferences
    private PermissionPreferences permissionPreferences;
    
    // Notification Preferences
    private NotificationPreferences notificationPreferences;
    
    // Privacy Settings
    private PrivacySettings privacySettings;
    
    // Sound Settings
    private SoundSettings soundSettings;
    
    // Update timestamp
    private Timestamp updatedAt;

    public UserSettings() {
        this.volumeButtonSosEnabled = true;
        this.notificationsEnabled = true;
        this.locationTrackingEnabled = true;
        this.permissionPreferences = new PermissionPreferences();
        this.notificationPreferences = new NotificationPreferences();
        this.privacySettings = new PrivacySettings();
        this.soundSettings = new SoundSettings();
    }

    // ========== Getters & Setters ==========
    public String getSettingId() { return settingId; }
    public void setSettingId(String settingId) { this.settingId = settingId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public boolean isVolumButtonSosEnabled() { return volumeButtonSosEnabled; }
    public void setVolumButtonSosEnabled(boolean volumeButtonSosEnabled) { this.volumeButtonSosEnabled = volumeButtonSosEnabled; }

    public boolean isNotificationsEnabled() { return notificationsEnabled; }
    public void setNotificationsEnabled(boolean notificationsEnabled) { this.notificationsEnabled = notificationsEnabled; }

    public boolean isLocationTrackingEnabled() { return locationTrackingEnabled; }
    public void setLocationTrackingEnabled(boolean locationTrackingEnabled) { this.locationTrackingEnabled = locationTrackingEnabled; }

    public PermissionPreferences getPermissionPreferences() { return permissionPreferences; }
    public void setPermissionPreferences(PermissionPreferences permissionPreferences) { this.permissionPreferences = permissionPreferences; }

    public NotificationPreferences getNotificationPreferences() { return notificationPreferences; }
    public void setNotificationPreferences(NotificationPreferences notificationPreferences) { this.notificationPreferences = notificationPreferences; }

    public PrivacySettings getPrivacySettings() { return privacySettings; }
    public void setPrivacySettings(PrivacySettings privacySettings) { this.privacySettings = privacySettings; }

    public SoundSettings getSoundSettings() { return soundSettings; }
    public void setSoundSettings(SoundSettings soundSettings) { this.soundSettings = soundSettings; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    // ========== Inner Classes ==========

    public static class PermissionPreferences implements Serializable {
        private boolean microphone;
        private boolean camera;
        private boolean location;
        private boolean phone;
        private boolean contacts;

        public PermissionPreferences() {
            this.microphone = true;
            this.camera = true;
            this.location = true;
            this.phone = true;
            this.contacts = true;
        }

        public boolean isMicrophone() { return microphone; }
        public void setMicrophone(boolean microphone) { this.microphone = microphone; }

        public boolean isCamera() { return camera; }
        public void setCamera(boolean camera) { this.camera = camera; }

        public boolean isLocation() { return location; }
        public void setLocation(boolean location) { this.location = location; }

        public boolean isPhone() { return phone; }
        public void setPhone(boolean phone) { this.phone = phone; }

        public boolean isContacts() { return contacts; }
        public void setContacts(boolean contacts) { this.contacts = contacts; }
    }

    public static class NotificationPreferences implements Serializable {
        private boolean enableSMS;
        private boolean enableCall;
        private boolean enablePush;
        private boolean enableEmail;

        public NotificationPreferences() {
            this.enableSMS = true;
            this.enableCall = true;
            this.enablePush = true;
            this.enableEmail = false;
        }

        public boolean isEnableSMS() { return enableSMS; }
        public void setEnableSMS(boolean enableSMS) { this.enableSMS = enableSMS; }

        public boolean isEnableCall() { return enableCall; }
        public void setEnableCall(boolean enableCall) { this.enableCall = enableCall; }

        public boolean isEnablePush() { return enablePush; }
        public void setEnablePush(boolean enablePush) { this.enablePush = enablePush; }

        public boolean isEnableEmail() { return enableEmail; }
        public void setEnableEmail(boolean enableEmail) { this.enableEmail = enableEmail; }
    }

    public static class PrivacySettings implements Serializable {
        private boolean showProfileToNetwork;
        private boolean allowLocationSharing;
        private boolean dataStorageConsent;

        public PrivacySettings() {
            this.showProfileToNetwork = true;
            this.allowLocationSharing = true;
            this.dataStorageConsent = true;
        }

        public boolean isShowProfileToNetwork() { return showProfileToNetwork; }
        public void setShowProfileToNetwork(boolean showProfileToNetwork) { this.showProfileToNetwork = showProfileToNetwork; }

        public boolean isAllowLocationSharing() { return allowLocationSharing; }
        public void setAllowLocationSharing(boolean allowLocationSharing) { this.allowLocationSharing = allowLocationSharing; }

        public boolean isDataStorageConsent() { return dataStorageConsent; }
        public void setDataStorageConsent(boolean dataStorageConsent) { this.dataStorageConsent = dataStorageConsent; }
    }

    public static class SoundSettings implements Serializable {
        private int sosAlertVolume; // 0-100
        private String ringtone; // system ringtone identifier
        private boolean vibrationEnabled;

        public SoundSettings() {
            this.sosAlertVolume = 100;
            this.ringtone = "system_default";
            this.vibrationEnabled = true;
        }

        public int getSosAlertVolume() { return sosAlertVolume; }
        public void setSosAlertVolume(int sosAlertVolume) { this.sosAlertVolume = sosAlertVolume; }

        public String getRingtone() { return ringtone; }
        public void setRingtone(String ringtone) { this.ringtone = ringtone; }

        public boolean isVibrationEnabled() { return vibrationEnabled; }
        public void setVibrationEnabled(boolean vibrationEnabled) { this.vibrationEnabled = vibrationEnabled; }
    }

    @Override
    public String toString() {
        return "UserSettings{" +
                "settingId='" + settingId + '\'' +
                ", volumeButtonSosEnabled=" + volumeButtonSosEnabled +
                ", notificationsEnabled=" + notificationsEnabled +
                '}';
    }
}
