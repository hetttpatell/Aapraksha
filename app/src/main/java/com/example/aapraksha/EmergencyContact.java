package com.example.aapraksha;

import com.google.firebase.Timestamp;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * EmergencyContact model for Firestore subcollection: users/{userId}/emergency_contacts
 * Supports both PRIORITY (1 only) and NORMAL contact types
 */
public class EmergencyContact implements Serializable {
    private String contactId;
    private String name;
    private String phone;
    private String relation; // Father, Mother, Brother, Sister, Friend, Spouse, Colleague, Other
    private String email;
    
    // Contact Type - CRITICAL: Only 1 contact should be PRIORITY
    private String contactType; // PRIORITY or NORMAL
    private boolean isPriority; // true only for 1 contact
    
    // Notification Methods
    private List<String> notificationMethod; // SMS, CALL, PUSH_NOTIFICATION, EMAIL
    private NotificationPreference notificationPreference;
    
    // Contact Timeline
    private Timestamp addedAt;
    private Timestamp lastNotifiedAt;
    private int totalNotifications;
    
    // Response History
    private List<ContactResponse> responseHistory;
    private String status; // ACTIVE, BLOCKED, INACTIVE

    // Default constructor
    public EmergencyContact() {
        this.notificationMethod = new ArrayList<>();
        this.notificationPreference = new NotificationPreference();
        this.responseHistory = new ArrayList<>();
        this.status = "ACTIVE";
        this.totalNotifications = 0;
    }

    // Constructor with essential fields
    public EmergencyContact(String name, String phone, String relation) {
        this.name = name;
        this.phone = phone;
        this.relation = relation;
        this.contactType = "NORMAL"; // Default is NORMAL
        this.isPriority = false;
        this.notificationMethod = new ArrayList<>();
        this.notificationPreference = new NotificationPreference();
        this.responseHistory = new ArrayList<>();
        this.status = "ACTIVE";
        this.totalNotifications = 0;
    }

    // ========== Getters & Setters ==========
    public String getContactId() { return contactId; }
    public void setContactId(String contactId) { this.contactId = contactId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getRelation() { return relation; }
    public void setRelation(String relation) { this.relation = relation; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getContactType() { return contactType; }
    public void setContactType(String contactType) { this.contactType = contactType; }

    public boolean isPriority() { return isPriority; }
    public void setPriority(boolean priority) { isPriority = priority; }

    public List<String> getNotificationMethod() { return notificationMethod; }
    public void setNotificationMethod(List<String> notificationMethod) { this.notificationMethod = notificationMethod; }

    public NotificationPreference getNotificationPreference() { return notificationPreference; }
    public void setNotificationPreference(NotificationPreference notificationPreference) { 
        this.notificationPreference = notificationPreference; 
    }

    public Timestamp getAddedAt() { return addedAt; }
    public void setAddedAt(Timestamp addedAt) { this.addedAt = addedAt; }

    public Timestamp getLastNotifiedAt() { return lastNotifiedAt; }
    public void setLastNotifiedAt(Timestamp lastNotifiedAt) { this.lastNotifiedAt = lastNotifiedAt; }

    public int getTotalNotifications() { return totalNotifications; }
    public void setTotalNotifications(int totalNotifications) { this.totalNotifications = totalNotifications; }

    public List<ContactResponse> getResponseHistory() { return responseHistory; }
    public void setResponseHistory(List<ContactResponse> responseHistory) { this.responseHistory = responseHistory; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    /**
     * Inner class for notification preferences
     */
    public static class NotificationPreference implements Serializable {
        private boolean enableImmediateNotification; // Notify instantly on SOS
        private boolean enableSMS;
        private boolean enableCall;
        private boolean enablePush;
        private boolean enableEmail;

        public NotificationPreference() {
            this.enableImmediateNotification = true;
            this.enableSMS = true;
            this.enableCall = true;
            this.enablePush = true;
            this.enableEmail = false;
        }

        // Getters & Setters
        public boolean isEnableImmediateNotification() { return enableImmediateNotification; }
        public void setEnableImmediateNotification(boolean enable) { this.enableImmediateNotification = enable; }

        public boolean isEnableSMS() { return enableSMS; }
        public void setEnableSMS(boolean enable) { this.enableSMS = enable; }

        public boolean isEnableCall() { return enableCall; }
        public void setEnableCall(boolean enable) { this.enableCall = enable; }

        public boolean isEnablePush() { return enablePush; }
        public void setEnablePush(boolean enable) { this.enablePush = enable; }

        public boolean isEnableEmail() { return enableEmail; }
        public void setEnableEmail(boolean enable) { this.enableEmail = enable; }
    }

    /**
     * Inner class for contact response tracking
     */
    public static class ContactResponse implements Serializable {
        private String alertId;
        private Timestamp notifiedAt;
        private int responseTime; // seconds to respond
        private String responseStatus; // RECEIVED, ACKNOWLEDGED, CALLING, REACHED, NO_RESPONSE
        private Timestamp responseTimestamp;
        private String responseNotes;
        private double responseLatitude;
        private double responseLongitude;
        private Timestamp reachedAt;

        public ContactResponse() {}

        // Getters & Setters
        public String getAlertId() { return alertId; }
        public void setAlertId(String alertId) { this.alertId = alertId; }

        public Timestamp getNotifiedAt() { return notifiedAt; }
        public void setNotifiedAt(Timestamp notifiedAt) { this.notifiedAt = notifiedAt; }

        public int getResponseTime() { return responseTime; }
        public void setResponseTime(int responseTime) { this.responseTime = responseTime; }

        public String getResponseStatus() { return responseStatus; }
        public void setResponseStatus(String responseStatus) { this.responseStatus = responseStatus; }

        public Timestamp getResponseTimestamp() { return responseTimestamp; }
        public void setResponseTimestamp(Timestamp responseTimestamp) { this.responseTimestamp = responseTimestamp; }

        public String getResponseNotes() { return responseNotes; }
        public void setResponseNotes(String responseNotes) { this.responseNotes = responseNotes; }

        public double getResponseLatitude() { return responseLatitude; }
        public void setResponseLatitude(double responseLatitude) { this.responseLatitude = responseLatitude; }

        public double getResponseLongitude() { return responseLongitude; }
        public void setResponseLongitude(double responseLongitude) { this.responseLongitude = responseLongitude; }

        public Timestamp getReachedAt() { return reachedAt; }
        public void setReachedAt(Timestamp reachedAt) { this.reachedAt = reachedAt; }
    }

    @Override
    public String toString() {
        return "EmergencyContact{" +
                "name='" + name + '\'' +
                ", phone='" + phone + '\'' +
                ", contactType='" + contactType + '\'' +
                ", isPriority=" + isPriority +
                ", status='" + status + '\'' +
                '}';
    }
}
