package com.example.aapraksha;

import com.google.firebase.Timestamp;
import java.io.Serializable;

/**
 * User model for Firestore 'users' collection
 * Represents complete user profile with SOS status
 */
public class User implements Serializable {
    private String userId;
    private String email;
    private String phone;
    private String fullName;
    private String profilePhotoUrl;
    private String accountStatus; // ACTIVE, DEACTIVATED, SUSPENDED
    
    // SOS & Security
    private String emergencyPin; // Encrypted 4-digit PIN
    private SOSStatus sosStatus;
    
    // Timestamps
    private Timestamp memberSince;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private Timestamp lastLoginAt;

    // Default constructor for Firestore
    public User() {
        this.sosStatus = new SOSStatus();
        this.accountStatus = "ACTIVE";
    }

    // Constructor with basic info
    public User(String email, String phone, String fullName) {
        this.email = email;
        this.phone = phone;
        this.fullName = fullName;
        this.sosStatus = new SOSStatus();
        this.accountStatus = "ACTIVE";
    }

    // ========== Getters & Setters ==========
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getProfilePhotoUrl() { return profilePhotoUrl; }
    public void setProfilePhotoUrl(String profilePhotoUrl) { this.profilePhotoUrl = profilePhotoUrl; }

    public String getAccountStatus() { return accountStatus; }
    public void setAccountStatus(String accountStatus) { this.accountStatus = accountStatus; }

    public String getEmergencyPin() { return emergencyPin; }
    public void setEmergencyPin(String emergencyPin) { this.emergencyPin = emergencyPin; }

    public SOSStatus getSosStatus() { return sosStatus; }
    public void setSosStatus(SOSStatus sosStatus) { this.sosStatus = sosStatus; }

    public Timestamp getMemberSince() { return memberSince; }
    public void setMemberSince(Timestamp memberSince) { this.memberSince = memberSince; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public Timestamp getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(Timestamp lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    /**
     * Inner class for SOS Status tracking
     */
    public static class SOSStatus implements Serializable {
        private boolean isActive; // true when SOS is triggered
        private Timestamp triggeredAt;
        private String lastSosAlertId;
        private int pinAttempts; // count of PIN attempts

        public SOSStatus() {
            this.isActive = false;
            this.pinAttempts = 0;
        }

        // Getters & Setters
        public boolean isActive() { return isActive; }
        public void setActive(boolean active) { isActive = active; }

        public Timestamp getTriggeredAt() { return triggeredAt; }
        public void setTriggeredAt(Timestamp triggeredAt) { this.triggeredAt = triggeredAt; }

        public String getLastSosAlertId() { return lastSosAlertId; }
        public void setLastSosAlertId(String lastSosAlertId) { this.lastSosAlertId = lastSosAlertId; }

        public int getPinAttempts() { return pinAttempts; }
        public void setPinAttempts(int pinAttempts) { this.pinAttempts = pinAttempts; }

        @Override
        public String toString() {
            return "SOSStatus{" +
                    "isActive=" + isActive +
                    ", triggeredAt=" + triggeredAt +
                    ", lastSosAlertId='" + lastSosAlertId + '\'' +
                    ", pinAttempts=" + pinAttempts +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", fullName='" + fullName + '\'' +
                ", accountStatus='" + accountStatus + '\'' +
                '}';
    }
}
