package com.example.aapraksha;

import com.google.firebase.Timestamp;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * AlertHistory model for Firestore subcollection: users/{userId}/alert_history
 * Stores COMPLETE DETAILS for history page display
 */
public class AlertHistory implements Serializable {
    private String historyId;
    private String alertId; // reference to sos_alerts
    private String alertType; // SOS, CHECK_IN, MANUAL
    private String alertStatus; // TRIGGERED, ACTIVE, CANCELLED, RESOLVED, EXPIRED, ABANDONED
    
    // Timing information
    private TimingInfo timing;
    
    // Location details
    private LocationInfo location;
    
    // Contact notifications
    private ContactNotificationInfo contactsNotified;
    
    // Alert summary
    private AlertSummaryInfo alertSummary;
    
    // Cancellation details
    private CancellationInfo cancellation;
    
    // Device information at time of alert
    private DeviceInfoAtAlert deviceInfo;
    
    // Media attachments
    private AttachmentInfo attachments;
    
    // Additional info
    private String notes;
    private boolean reportedToAuthorities;
    private String authorityCase;
    private List<String> sharedWith;
    private List<String> tags; // ACCIDENT, HARASSMENT, THEFT, etc.

    public AlertHistory() {
        this.timing = new TimingInfo();
        this.location = new LocationInfo();
        this.contactsNotified = new ContactNotificationInfo();
        this.alertSummary = new AlertSummaryInfo();
        this.cancellation = new CancellationInfo();
        this.deviceInfo = new DeviceInfoAtAlert();
        this.attachments = new AttachmentInfo();
        this.sharedWith = new ArrayList<>();
        this.tags = new ArrayList<>();
    }

    // ========== Getters & Setters ==========
    public String getHistoryId() { return historyId; }
    public void setHistoryId(String historyId) { this.historyId = historyId; }

    public String getAlertId() { return alertId; }
    public void setAlertId(String alertId) { this.alertId = alertId; }

    public String getAlertType() { return alertType; }
    public void setAlertType(String alertType) { this.alertType = alertType; }

    public String getAlertStatus() { return alertStatus; }
    public void setAlertStatus(String alertStatus) { this.alertStatus = alertStatus; }

    public TimingInfo getTiming() { return timing; }
    public void setTiming(TimingInfo timing) { this.timing = timing; }

    public LocationInfo getLocation() { return location; }
    public void setLocation(LocationInfo location) { this.location = location; }

    public ContactNotificationInfo getContactsNotified() { return contactsNotified; }
    public void setContactsNotified(ContactNotificationInfo contactsNotified) { this.contactsNotified = contactsNotified; }

    public AlertSummaryInfo getAlertSummary() { return alertSummary; }
    public void setAlertSummary(AlertSummaryInfo alertSummary) { this.alertSummary = alertSummary; }

    public CancellationInfo getCancellation() { return cancellation; }
    public void setCancellation(CancellationInfo cancellation) { this.cancellation = cancellation; }

    public DeviceInfoAtAlert getDeviceInfo() { return deviceInfo; }
    public void setDeviceInfo(DeviceInfoAtAlert deviceInfo) { this.deviceInfo = deviceInfo; }

    public AttachmentInfo getAttachments() { return attachments; }
    public void setAttachments(AttachmentInfo attachments) { this.attachments = attachments; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public boolean isReportedToAuthorities() { return reportedToAuthorities; }
    public void setReportedToAuthorities(boolean reportedToAuthorities) { this.reportedToAuthorities = reportedToAuthorities; }

    public String getAuthorityCase() { return authorityCase; }
    public void setAuthorityCase(String authorityCase) { this.authorityCase = authorityCase; }

    public List<String> getSharedWith() { return sharedWith; }
    public void setSharedWith(List<String> sharedWith) { this.sharedWith = sharedWith; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    // ========== Inner Classes ==========

    public static class TimingInfo implements Serializable {
        private Timestamp triggeredAt;
        private Timestamp cancelledAt;
        private Timestamp resolvedAt;
        private Timestamp expiresAt;
        private int totalDuration; // seconds alert was active

        public TimingInfo() {}

        public Timestamp getTriggeredAt() { return triggeredAt; }
        public void setTriggeredAt(Timestamp triggeredAt) { this.triggeredAt = triggeredAt; }

        public Timestamp getCancelledAt() { return cancelledAt; }
        public void setCancelledAt(Timestamp cancelledAt) { this.cancelledAt = cancelledAt; }

        public Timestamp getResolvedAt() { return resolvedAt; }
        public void setResolvedAt(Timestamp resolvedAt) { this.resolvedAt = resolvedAt; }

        public Timestamp getExpiresAt() { return expiresAt; }
        public void setExpiresAt(Timestamp expiresAt) { this.expiresAt = expiresAt; }

        public int getTotalDuration() { return totalDuration; }
        public void setTotalDuration(int totalDuration) { this.totalDuration = totalDuration; }
    }

    public static class LocationInfo implements Serializable {
        private double latitude;
        private double longitude;
        private String address;
        private String area; // city/region
        private double accuracy;
        private String geoHash;

        public LocationInfo() {}

        public double getLatitude() { return latitude; }
        public void setLatitude(double latitude) { this.latitude = latitude; }

        public double getLongitude() { return longitude; }
        public void setLongitude(double longitude) { this.longitude = longitude; }

        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }

        public String getArea() { return area; }
        public void setArea(String area) { this.area = area; }

        public double getAccuracy() { return accuracy; }
        public void setAccuracy(double accuracy) { this.accuracy = accuracy; }

        public String getGeoHash() { return geoHash; }
        public void setGeoHash(String geoHash) { this.geoHash = geoHash; }
    }

    public static class ContactNotificationInfo implements Serializable {
        private int totalCount;
        private int respondedCount;
        private int notRespondedCount;
        private List<NotificationDetail> notificationsList;
        private boolean priorityContactResponded;

        public ContactNotificationInfo() {
            this.notificationsList = new ArrayList<>();
        }

        public int getTotalCount() { return totalCount; }
        public void setTotalCount(int totalCount) { this.totalCount = totalCount; }

        public int getRespondedCount() { return respondedCount; }
        public void setRespondedCount(int respondedCount) { this.respondedCount = respondedCount; }

        public int getNotRespondedCount() { return notRespondedCount; }
        public void setNotRespondedCount(int notRespondedCount) { this.notRespondedCount = notRespondedCount; }

        public List<NotificationDetail> getNotificationsList() { return notificationsList; }
        public void setNotificationsList(List<NotificationDetail> notificationsList) { this.notificationsList = notificationsList; }

        public boolean isPriorityContactResponded() { return priorityContactResponded; }
        public void setPriorityContactResponded(boolean priorityContactResponded) { this.priorityContactResponded = priorityContactResponded; }

        public static class NotificationDetail implements Serializable {
            private String contactName;
            private String contactPhone;
            private Timestamp notifiedAt;
            private String responseStatus;
            private int responseTime;
            private boolean reached;

            public NotificationDetail() {}

            public String getContactName() { return contactName; }
            public void setContactName(String contactName) { this.contactName = contactName; }

            public String getContactPhone() { return contactPhone; }
            public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

            public Timestamp getNotifiedAt() { return notifiedAt; }
            public void setNotifiedAt(Timestamp notifiedAt) { this.notifiedAt = notifiedAt; }

            public String getResponseStatus() { return responseStatus; }
            public void setResponseStatus(String responseStatus) { this.responseStatus = responseStatus; }

            public int getResponseTime() { return responseTime; }
            public void setResponseTime(int responseTime) { this.responseTime = responseTime; }

            public boolean isReached() { return reached; }
            public void setReached(boolean reached) { this.reached = reached; }
        }
    }

    public static class AlertSummaryInfo implements Serializable {
        private String message;
        private String reason;
        private String triggeredFrom; // BUTTON, VOLUME_KEY
        private boolean testAlert;

        public AlertSummaryInfo() {}

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }

        public String getTriggeredFrom() { return triggeredFrom; }
        public void setTriggeredFrom(String triggeredFrom) { this.triggeredFrom = triggeredFrom; }

        public boolean isTestAlert() { return testAlert; }
        public void setTestAlert(boolean testAlert) { this.testAlert = testAlert; }
    }

    public static class CancellationInfo implements Serializable {
        private boolean wasCancelled;
        private String cancelledBy;
        private int cancellationTime; // seconds after trigger
        private String cancellationReason;
        private boolean requiredPin;
        private boolean pinVerified;
        private int pinAttempts;

        public CancellationInfo() {}

        public boolean isWasCancelled() { return wasCancelled; }
        public void setWasCancelled(boolean wasCancelled) { this.wasCancelled = wasCancelled; }

        public String getCancelledBy() { return cancelledBy; }
        public void setCancelledBy(String cancelledBy) { this.cancelledBy = cancelledBy; }

        public int getCancellationTime() { return cancellationTime; }
        public void setCancellationTime(int cancellationTime) { this.cancellationTime = cancellationTime; }

        public String getCancellationReason() { return cancellationReason; }
        public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }

        public boolean isRequiredPin() { return requiredPin; }
        public void setRequiredPin(boolean requiredPin) { this.requiredPin = requiredPin; }

        public boolean isPinVerified() { return pinVerified; }
        public void setPinVerified(boolean pinVerified) { this.pinVerified = pinVerified; }

        public int getPinAttempts() { return pinAttempts; }
        public void setPinAttempts(int pinAttempts) { this.pinAttempts = pinAttempts; }
    }

    public static class DeviceInfoAtAlert implements Serializable {
        private String deviceId;
        private String osVersion;
        private String appVersion;
        private int batteryLevel;
        private String networkType;
        private int signalStrength;

        public DeviceInfoAtAlert() {}

        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

        public String getOsVersion() { return osVersion; }
        public void setOsVersion(String osVersion) { this.osVersion = osVersion; }

        public String getAppVersion() { return appVersion; }
        public void setAppVersion(String appVersion) { this.appVersion = appVersion; }

        public int getBatteryLevel() { return batteryLevel; }
        public void setBatteryLevel(int batteryLevel) { this.batteryLevel = batteryLevel; }

        public String getNetworkType() { return networkType; }
        public void setNetworkType(String networkType) { this.networkType = networkType; }

        public int getSignalStrength() { return signalStrength; }
        public void setSignalStrength(int signalStrength) { this.signalStrength = signalStrength; }
    }

    public static class AttachmentInfo implements Serializable {
        private int mediaCount;
        private int imageCount;
        private int videoCount;
        private int audioCount;
        private List<String> mediaUrls;

        public AttachmentInfo() {
            this.mediaUrls = new ArrayList<>();
        }

        public int getMediaCount() { return mediaCount; }
        public void setMediaCount(int mediaCount) { this.mediaCount = mediaCount; }

        public int getImageCount() { return imageCount; }
        public void setImageCount(int imageCount) { this.imageCount = imageCount; }

        public int getVideoCount() { return videoCount; }
        public void setVideoCount(int videoCount) { this.videoCount = videoCount; }

        public int getAudioCount() { return audioCount; }
        public void setAudioCount(int audioCount) { this.audioCount = audioCount; }

        public List<String> getMediaUrls() { return mediaUrls; }
        public void setMediaUrls(List<String> mediaUrls) { this.mediaUrls = mediaUrls; }
    }

    @Override
    public String toString() {
        return "AlertHistory{" +
                "historyId='" + historyId + '\'' +
                ", alertId='" + alertId + '\'' +
                ", alertType='" + alertType + '\'' +
                ", alertStatus='" + alertStatus + '\'' +
                '}';
    }
}
