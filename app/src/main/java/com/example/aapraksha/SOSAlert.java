package com.example.aapraksha;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.PropertyName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * SOSAlert model for Firestore 'sos_alerts' collection
 * Stores COMPLETE SOS alert details for alert page display
 */
public class SOSAlert implements Serializable {
    private String alertId;
    private String userId;
    private String userName;
    private String userPhone;
    private String userProfilePhoto;
    
    // Status tracking
    private String status; // TRIGGERED, ACTIVE, CANCELLED, RESOLVED, EXPIRED
    private String alertType; // SOS, CHECK_IN, MANUAL
    private String legacyAlertType; // legacy field "alertType"
    private SOSData sosData;
    
    // Direct Firestore flat timestamp fields
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private Timestamp cancelledAt;
    
    // Location information
    private LocationData location;
    
    // Alert message
    private String alertMessage;
    private String incidentReport;
    private AlertDetails alertDetails;
    
    // Firestore flat field for device info (stored as Map)
    private Map<String, Object> deviceInfo;
    
    // Firestore flat field for notifications (stored as List of Maps)
    private List<Object> notificationsToContacts;
    
    // Notifications sent (structured)
    private NotificationData notifications;
    
    // Contact responses
    private List<ContactResponse> responses;
    
    // Media attachments
    private List<MediaData> media;
    
    // Cancellation info
    private CancellationData cancellation;
    
    // Visibility & metadata
    private String visibility; // PRIVATE, CONTACTS_ONLY, PUBLIC
    private MetadataInfo metadata;
    
    // Audio recording data (separate from location)
    private Map<String, Object> audioData;
    
    // SOS status metadata (trigger count, visibility, etc.)
    private Map<String, Object> sosStatus;

    // ===== Danger Intelligence Layer Fields =====
    // These fields enable zone-based danger scoring by the Cloud Function.
    // Existing Firestore documents may not have these fields — always null-check.
    private String geohash;           // GeoFire geohash (precision 6 = ~1.2km zone)
    private String timeOfDay;         // "MORNING", "AFTERNOON", "EVENING", "NIGHT"
    private boolean resolved;         // true when SOS is resolved (not just cancelled)
    private int durationSeconds;      // total SOS duration for severity scoring

    public SOSAlert() {
        this.sosData = new SOSData();
        this.location = new LocationData();
        this.alertDetails = new AlertDetails();
        this.notifications = new NotificationData();
        this.responses = new ArrayList<>();
        this.media = new ArrayList<>();
        this.cancellation = new CancellationData();
        this.metadata = new MetadataInfo();
    }

    // ========== Getters & Setters ==========

    public String getAlertId() { return alertId; }
    public void setAlertId(String alertId) { this.alertId = alertId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserPhone() { return userPhone; }
    public void setUserPhone(String userPhone) { this.userPhone = userPhone; }

    public String getUserProfilePhoto() { return userProfilePhoto; }
    public void setUserProfilePhoto(String userProfilePhoto) { this.userProfilePhoto = userProfilePhoto; }

    @PropertyName("status")
    public String getStatus() { return status; }
    
    @PropertyName("status")
    public void setStatus(String status) { this.status = status; }

    @PropertyName("type")
    public String getAlertType() {
        return alertType != null ? alertType : legacyAlertType;
    }
    
    @PropertyName("type")
    public void setAlertType(String alertType) { this.alertType = alertType; }

    @PropertyName("alertType")
    public String getLegacyAlertType() {
        return legacyAlertType;
    }

    @PropertyName("alertType")
    public void setLegacyAlertType(String legacyAlertType) {
        this.legacyAlertType = legacyAlertType;
    }
    

    public SOSData getSosData() { return sosData; }
    public void setSosData(SOSData sosData) { this.sosData = sosData; }

    public LocationData getLocation() { return location; }
    public void setLocation(LocationData location) { this.location = location; }

    public String getAlertMessage() { return alertMessage; }
    public void setAlertMessage(String alertMessage) { this.alertMessage = alertMessage; }

    public String getIncidentReport() { return incidentReport; }
    public void setIncidentReport(String incidentReport) { this.incidentReport = incidentReport; }

    public AlertDetails getAlertDetails() { return alertDetails; }
    public void setAlertDetails(AlertDetails alertDetails) { this.alertDetails = alertDetails; }

    public NotificationData getNotifications() { return notifications; }
    public void setNotifications(NotificationData notifications) { this.notifications = notifications; }

    public List<ContactResponse> getResponses() { return responses; }
    public void setResponses(List<ContactResponse> responses) { this.responses = responses; }

    public List<MediaData> getMedia() { return media; }
    public void setMedia(List<MediaData> media) { this.media = media; }

    public CancellationData getCancellation() { return cancellation; }
    public void setCancellation(CancellationData cancellation) { this.cancellation = cancellation; }

    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }

    public MetadataInfo getMetadata() { return metadata; }
    public void setMetadata(MetadataInfo metadata) { this.metadata = metadata; }
    
    public Map<String, Object> getAudioData() { return audioData; }
    public void setAudioData(Map<String, Object> audioData) { this.audioData = audioData; }

    // ========== Additional Getters for Compatibility ==========
    
    /**
     * Get creation timestamp - checks direct field first (Firestore flat),
     * then falls back to sosData.triggeredAt (structured model)
     */
    public Timestamp getCreatedAt() {
        if (createdAt != null) return createdAt;
        return sosData != null ? sosData.getTriggeredAt() : null;
    }

    /**
     * Set creation timestamp
     */
    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
        if (sosData == null) {
            sosData = new SOSData();
        }
        sosData.setTriggeredAt(createdAt);
    }
    
    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
    
    public Timestamp getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(Timestamp cancelledAt) { this.cancelledAt = cancelledAt; }

    /**
     * Get device information - checks flat Map first (Firestore), then alertDetails
     */
    public Map<String, Object> getDeviceInfo() {
        return deviceInfo;
    }
    
    /**
     * Set device information from Firestore flat Map
     */
    public void setDeviceInfo(Map<String, Object> deviceInfo) {
        this.deviceInfo = deviceInfo;
    }
    
    /**
     * Get structured device info from the flat Map
     */
    public AlertDetails getDeviceInfoAsDetails() {
        if (deviceInfo != null) {
            AlertDetails details = new AlertDetails();
            Object battery = deviceInfo.get("batteryLevel");
            if (battery instanceof Number) details.setBatteryLevel(((Number) battery).intValue());
            Object network = deviceInfo.get("networkType");
            if (network instanceof String) details.setNetworkType((String) network);
            Object signal = deviceInfo.get("signalStrength");
            if (signal instanceof Number) details.setSignalStrength(((Number) signal).intValue());
            return details;
        }
        return alertDetails;
    }

    /**
     * Get notifications to contacts as list.
     * Checks direct Firestore flat field first, then structured notifications.
     */
    public List<Object> getNotificationsToContacts() {
        if (notificationsToContacts != null && !notificationsToContacts.isEmpty()) {
            return notificationsToContacts;
        }
        if (notifications != null && notifications.getContactsNotifiedList() != null) {
            return new ArrayList<>(notifications.getContactsNotifiedList());
        }
        return new ArrayList<>();
    }

    /**
     * Set notifications to contacts
     */
    public void setNotificationsToContacts(List<Object> notificationsList) {
        this.notificationsToContacts = notificationsList;
    }

    /**
     * Get user-friendly status display text.
     * CANCELLED / RESOLVED / EXPIRED = "Inactive", everything else = "Active"
     */
    public String getStatusDisplayText() {
        if (status == null) return "Active";
        switch (status) {
            case "CANCELLED":
            case "RESOLVED":
            case "EXPIRED":
                return "Inactive";
            default:
                return "Active";
        }
    }

    /**
     * Check if alert is currently active
     */
    public boolean isActive() {
        return "ACTIVE".equals(status) || "TRIGGERED".equals(status);
    }

    public Map<String, Object> getSosStatus() {
        return sosStatus;
    }

    public void setSosStatus(Map<String, Object> sosStatus) {
        this.sosStatus = sosStatus;
    }

    // ===== Danger Intelligence Getters & Setters =====

    public String getGeohash() { return geohash; }
    public void setGeohash(String geohash) { this.geohash = geohash; }

    public String getTimeOfDay() { return timeOfDay; }
    public void setTimeOfDay(String timeOfDay) { this.timeOfDay = timeOfDay; }

    public boolean isResolved() { return resolved; }
    public void setResolved(boolean resolved) { this.resolved = resolved; }

    public int getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; }

    /**
     * Get the total number of SOS alerts triggered for this alert session or user
     */
    public int getNumberOfSOSTriggered() {
        if (sosStatus != null && sosStatus.containsKey("numberOfSOSTriggered")) {
            Object count = sosStatus.get("numberOfSOSTriggered");
            if (count instanceof Number) {
                return ((Number) count).intValue();
            }
        }
        return 0;
    }

    // ========== Inner Classes ==========

    public static class SOSData implements Serializable {
        private Timestamp triggeredAt;
        private Timestamp cancelledAt;
        private Timestamp resolvedAt;
        private Timestamp expiresAt;
        private int duration; // seconds
        private String cancellationPin;

        public SOSData() {}

        public Timestamp getTriggeredAt() { return triggeredAt; }
        public void setTriggeredAt(Timestamp triggeredAt) { this.triggeredAt = triggeredAt; }

        public Timestamp getCancelledAt() { return cancelledAt; }
        public void setCancelledAt(Timestamp cancelledAt) { this.cancelledAt = cancelledAt; }

        public Timestamp getResolvedAt() { return resolvedAt; }
        public void setResolvedAt(Timestamp resolvedAt) { this.resolvedAt = resolvedAt; }

        public Timestamp getExpiresAt() { return expiresAt; }
        public void setExpiresAt(Timestamp expiresAt) { this.expiresAt = expiresAt; }

        public int getDuration() { return duration; }
        public void setDuration(int duration) { this.duration = duration; }

        public String getCancellationPin() { return cancellationPin; }
        public void setCancellationPin(String cancellationPin) { this.cancellationPin = cancellationPin; }
    }

    public static class LocationData implements Serializable {
        private GeoPoint geoPoint;
        private double latitude;
        private double longitude;
        private String address;
        private double accuracy;
        private double heading;
        private double speed;

        public LocationData() {}

        public GeoPoint getGeoPoint() { return geoPoint; }
        public void setGeoPoint(GeoPoint geoPoint) { this.geoPoint = geoPoint; }

        public double getLatitude() { return latitude; }
        public void setLatitude(double latitude) { this.latitude = latitude; }

        public double getLongitude() { return longitude; }
        public void setLongitude(double longitude) { this.longitude = longitude; }

        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }

        public double getAccuracy() { return accuracy; }
        public void setAccuracy(double accuracy) { this.accuracy = accuracy; }

        public double getHeading() { return heading; }
        public void setHeading(double heading) { this.heading = heading; }

        public double getSpeed() { return speed; }
        public void setSpeed(double speed) { this.speed = speed; }
    }

    public static class AlertDetails implements Serializable {
        private int batteryLevel;
        private String networkType;
        private int signalStrength;
        private String deviceId;
        private String osVersion;
        private String appVersion;
        private boolean screenOn;

        public AlertDetails() {}

        public int getBatteryLevel() { return batteryLevel; }
        public void setBatteryLevel(int batteryLevel) { this.batteryLevel = batteryLevel; }

        public String getNetworkType() { return networkType; }
        public void setNetworkType(String networkType) { this.networkType = networkType; }

        public int getSignalStrength() { return signalStrength; }
        public void setSignalStrength(int signalStrength) { this.signalStrength = signalStrength; }

        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

        public String getOsVersion() { return osVersion; }
        public void setOsVersion(String osVersion) { this.osVersion = osVersion; }

        public String getAppVersion() { return appVersion; }
        public void setAppVersion(String appVersion) { this.appVersion = appVersion; }

        public boolean isScreenOn() { return screenOn; }
        public void setScreenOn(boolean screenOn) { this.screenOn = screenOn; }
    }

    public static class NotificationData implements Serializable {
        private int totalContactsNotified;
        private List<Object> contactsNotifiedList;
        private int audioCount;
        private int smsCount;
        private int callCount;
        private int pushNotificationCount;
        private int emailCount;
        private List<Timestamp> notificationTimestamps;

        public NotificationData() {
            this.contactsNotifiedList = new ArrayList<>();
            this.notificationTimestamps = new ArrayList<>();
        }

        public int getTotalContactsNotified() { return totalContactsNotified; }
        public void setTotalContactsNotified(int totalContactsNotified) { this.totalContactsNotified = totalContactsNotified; }

        public List<Object> getContactsNotifiedList() { return contactsNotifiedList; }
        public void setContactsNotifiedList(List<Object> contactsNotifiedList) { this.contactsNotifiedList = contactsNotifiedList; }

        public int getAudioCount() { return audioCount; }
        public void setAudioCount(int audioCount) { this.audioCount = audioCount; }

        public int getSmsCount() { return smsCount; }
        public void setSmsCount(int smsCount) { this.smsCount = smsCount; }

        public int getCallCount() { return callCount; }
        public void setCallCount(int callCount) { this.callCount = callCount; }

        public int getPushNotificationCount() { return pushNotificationCount; }
        public void setPushNotificationCount(int pushNotificationCount) { this.pushNotificationCount = pushNotificationCount; }

        public int getEmailCount() { return emailCount; }
        public void setEmailCount(int emailCount) { this.emailCount = emailCount; }

        public List<Timestamp> getNotificationTimestamps() { return notificationTimestamps; }
        public void setNotificationTimestamps(List<Timestamp> notificationTimestamps) { this.notificationTimestamps = notificationTimestamps; }
    }

    public static class ContactResponse implements Serializable {
        private String contactId;
        private String contactName;
        private String contactPhone;
        private Timestamp notifiedAt;
        private int responseTime; // seconds
        private String responseStatus;
        private Timestamp responseTimestamp;
        private String responseNotes;
        private double responseLatitude;
        private double responseLongitude;
        private Timestamp reachedAt;

        public ContactResponse() {}

        public String getContactId() { return contactId; }
        public void setContactId(String contactId) { this.contactId = contactId; }

        public String getContactName() { return contactName; }
        public void setContactName(String contactName) { this.contactName = contactName; }

        public String getContactPhone() { return contactPhone; }
        public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

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

    public static class MediaData implements Serializable {
        private String type; // IMAGE, VIDEO, AUDIO
        private String url;
        private Timestamp capturedAt;
        private long size; // bytes
        private int duration; // for video/audio - seconds

        public MediaData() {}

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }

        public Timestamp getCapturedAt() { return capturedAt; }
        public void setCapturedAt(Timestamp capturedAt) { this.capturedAt = capturedAt; }

        public long getSize() { return size; }
        public void setSize(long size) { this.size = size; }

        public int getDuration() { return duration; }
        public void setDuration(int duration) { this.duration = duration; }
    }


    public static class CancellationData implements Serializable {
        private String status; // SELF_CANCELLED, PIN_REQUIRED, AUTO_CANCELLED
        private String cancelledBy;
        private Timestamp cancelledAt;
        private String cancellationReason;
        private boolean pinVerified;
        private int pinAttempts;

        public CancellationData() {}

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getCancelledBy() { return cancelledBy; }
        public void setCancelledBy(String cancelledBy) { this.cancelledBy = cancelledBy; }

        public Timestamp getCancelledAt() { return cancelledAt; }
        public void setCancelledAt(Timestamp cancelledAt) { this.cancelledAt = cancelledAt; }

        public String getCancellationReason() { return cancellationReason; }
        public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }

        public boolean isPinVerified() { return pinVerified; }
        public void setPinVerified(boolean pinVerified) { this.pinVerified = pinVerified; }

        public int getPinAttempts() { return pinAttempts; }
        public void setPinAttempts(int pinAttempts) { this.pinAttempts = pinAttempts; }
    }

    public static class MetadataInfo implements Serializable {
        private String triggeredFrom; // BUTTON, VOLUME_KEY, MANUAL
        private boolean testMode;
        private String notes;

        public MetadataInfo() {}

        public String getTriggeredFrom() { return triggeredFrom; }
        public void setTriggeredFrom(String triggeredFrom) { this.triggeredFrom = triggeredFrom; }

        public boolean isTestMode() { return testMode; }
        public void setTestMode(boolean testMode) { this.testMode = testMode; }

        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }

    @Override
    public String toString() {
        return "SOSAlert{" +
                "alertId='" + alertId + '\'' +
                ", userId='" + userId + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
