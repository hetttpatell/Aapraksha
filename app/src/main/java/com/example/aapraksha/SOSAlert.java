package com.example.aapraksha;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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
    private SOSData sosData;
    
    // Location information
    private LocationData location;
    
    // Alert message
    private String alertMessage;
    private AlertDetails alertDetails;
    
    // Notifications sent
    private NotificationData notifications;
    
    // Contact responses
    private List<ContactResponse> responses;
    
    // Media attachments
    private List<MediaData> media;
    private VoiceRecording voiceRecording;
    
    // Cancellation info
    private CancellationData cancellation;
    
    // Visibility & metadata
    private String visibility; // PRIVATE, CONTACTS_ONLY, PUBLIC
    private MetadataInfo metadata;

    public SOSAlert() {
        this.sosData = new SOSData();
        this.location = new LocationData();
        this.alertDetails = new AlertDetails();
        this.notifications = new NotificationData();
        this.responses = new ArrayList<>();
        this.media = new ArrayList<>();
        this.voiceRecording = new VoiceRecording();
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

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public SOSData getSosData() { return sosData; }
    public void setSosData(SOSData sosData) { this.sosData = sosData; }

    public LocationData getLocation() { return location; }
    public void setLocation(LocationData location) { this.location = location; }

    public String getAlertMessage() { return alertMessage; }
    public void setAlertMessage(String alertMessage) { this.alertMessage = alertMessage; }

    public AlertDetails getAlertDetails() { return alertDetails; }
    public void setAlertDetails(AlertDetails alertDetails) { this.alertDetails = alertDetails; }

    public NotificationData getNotifications() { return notifications; }
    public void setNotifications(NotificationData notifications) { this.notifications = notifications; }

    public List<ContactResponse> getResponses() { return responses; }
    public void setResponses(List<ContactResponse> responses) { this.responses = responses; }

    public List<MediaData> getMedia() { return media; }
    public void setMedia(List<MediaData> media) { this.media = media; }

    public VoiceRecording getVoiceRecording() { return voiceRecording; }
    public void setVoiceRecording(VoiceRecording voiceRecording) { this.voiceRecording = voiceRecording; }

    public CancellationData getCancellation() { return cancellation; }
    public void setCancellation(CancellationData cancellation) { this.cancellation = cancellation; }

    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }

    public MetadataInfo getMetadata() { return metadata; }
    public void setMetadata(MetadataInfo metadata) { this.metadata = metadata; }

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
        private List<String> contactsNotifiedList;
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

        public List<String> getContactsNotifiedList() { return contactsNotifiedList; }
        public void setContactsNotifiedList(List<String> contactsNotifiedList) { this.contactsNotifiedList = contactsNotifiedList; }

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

    public static class VoiceRecording implements Serializable {
        private String url;
        private int duration; // seconds
        private Timestamp recordedAt;
        private String transcription;

        public VoiceRecording() {}

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }

        public int getDuration() { return duration; }
        public void setDuration(int duration) { this.duration = duration; }

        public Timestamp getRecordedAt() { return recordedAt; }
        public void setRecordedAt(Timestamp recordedAt) { this.recordedAt = recordedAt; }

        public String getTranscription() { return transcription; }
        public void setTranscription(String transcription) { this.transcription = transcription; }
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
        private String triggeredFrom; // BUTTON, VOLUME_KEY, VOICE, MANUAL
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
