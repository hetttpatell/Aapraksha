package com.example.aapraksha;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * NetworkAlert model for Firestore 'network_alerts' collection
 * Community safety alerts visible to users in a region
 */
public class NetworkAlert implements Serializable {
    private String networkAlertId;
    private String userId;
    private String userName;
    
    // Alert information
    private String alertType; // CRIME, ACCIDENT, HARASSMENT, THEFT, OTHER
    private String description;
    
    // Location information
    private GeoPoint location;
    private double latitude;
    private double longitude;
    private String locationAddress;
    
    // Alert severity & status
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL
    private String status; // ACTIVE, RESOLVED, FALSE_ALARM
    
    // Timeline
    private Timestamp reportedAt;
    private Timestamp resolvedAt;
    
    // Media & Evidence
    private List<String> mediaUrls; // images/videos
    
    // Responses & Verification
    private List<String> respondedBy; // user IDs who responded
    private List<String> verifiedBy; // verified by user IDs
    
    // Visibility
    private String visibility; // PUBLIC, CONTACTS_ONLY, PRIVATE
    private int regionRadius; // km - alert relevance range

    public NetworkAlert() {
        this.mediaUrls = new ArrayList<>();
        this.respondedBy = new ArrayList<>();
        this.verifiedBy = new ArrayList<>();
    }

    // ========== Getters & Setters ==========
    public String getNetworkAlertId() { return networkAlertId; }
    public void setNetworkAlertId(String networkAlertId) { this.networkAlertId = networkAlertId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getAlertType() { return alertType; }
    public void setAlertType(String alertType) { this.alertType = alertType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public GeoPoint getLocation() { return location; }
    public void setLocation(GeoPoint location) { this.location = location; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getLocationAddress() { return locationAddress; }
    public void setLocationAddress(String locationAddress) { this.locationAddress = locationAddress; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getReportedAt() { return reportedAt; }
    public void setReportedAt(Timestamp reportedAt) { this.reportedAt = reportedAt; }

    public Timestamp getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Timestamp resolvedAt) { this.resolvedAt = resolvedAt; }

    public List<String> getMediaUrls() { return mediaUrls; }
    public void setMediaUrls(List<String> mediaUrls) { this.mediaUrls = mediaUrls; }

    public List<String> getRespondedBy() { return respondedBy; }
    public void setRespondedBy(List<String> respondedBy) { this.respondedBy = respondedBy; }

    public List<String> getVerifiedBy() { return verifiedBy; }
    public void setVerifiedBy(List<String> verifiedBy) { this.verifiedBy = verifiedBy; }

    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }

    public int getRegionRadius() { return regionRadius; }
    public void setRegionRadius(int regionRadius) { this.regionRadius = regionRadius; }

    @Override
    public String toString() {
        return "NetworkAlert{" +
                "networkAlertId='" + networkAlertId + '\'' +
                ", userName='" + userName + '\'' +
                ", alertType='" + alertType + '\'' +
                ", severity='" + severity + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}

/**
 * DeviceToken model for Firestore 'device_tokens' collection
 * Stores FCM tokens for push notifications
 */
class DeviceToken implements Serializable {
    private String tokenId;
    private String userId;
    private String token; // FCM token
    private String deviceId;
    private String deviceName;
    private String osType; // Android, iOS
    private String osVersion;
    private String appVersion;
    private boolean isActive;
    private Timestamp registeredAt;
    private Timestamp lastUsedAt;

    public DeviceToken() {}

    public DeviceToken(String userId, String token) {
        this.userId = userId;
        this.token = token;
        this.isActive = true;
        this.registeredAt = Timestamp.now();
    }

    // ========== Getters & Setters ==========
    public String getTokenId() { return tokenId; }
    public void setTokenId(String tokenId) { this.tokenId = tokenId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

    public String getOsType() { return osType; }
    public void setOsType(String osType) { this.osType = osType; }

    public String getOsVersion() { return osVersion; }
    public void setOsVersion(String osVersion) { this.osVersion = osVersion; }

    public String getAppVersion() { return appVersion; }
    public void setAppVersion(String appVersion) { this.appVersion = appVersion; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public Timestamp getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(Timestamp registeredAt) { this.registeredAt = registeredAt; }

    public Timestamp getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(Timestamp lastUsedAt) { this.lastUsedAt = lastUsedAt; }

    @Override
    public String toString() {
        return "DeviceToken{" +
                "userId='" + userId + '\'' +
                ", deviceName='" + deviceName + '\'' +
                ", osType='" + osType + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}

/**
 * ActivityLog model for Firestore 'activity_logs' collection
 * Audit trail for security and compliance
 */
class ActivityLog implements Serializable {
    private String logId;
    private String userId;
    private String action; // LOGIN, LOGOUT, SOS_TRIGGERED, SOS_CANCELLED, CONTACT_ADDED, etc.
    private String description;
    private String status; // SUCCESS, FAILURE
    private String ipAddress;
    private String deviceId;
    private Timestamp timestamp;
    private MetadataLog metadata;

    public ActivityLog() {
        this.metadata = new MetadataLog();
    }

    // ========== Getters & Setters ==========
    public String getLogId() { return logId; }
    public void setLogId(String logId) { this.logId = logId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    public MetadataLog getMetadata() { return metadata; }
    public void setMetadata(MetadataLog metadata) { this.metadata = metadata; }

    public static class MetadataLog implements Serializable {
        private String alertId;
        private String location;
        private String details;

        public MetadataLog() {}

        public String getAlertId() { return alertId; }
        public void setAlertId(String alertId) { this.alertId = alertId; }

        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }

        public String getDetails() { return details; }
        public void setDetails(String details) { this.details = details; }
    }

    @Override
    public String toString() {
        return "ActivityLog{" +
                "userId='" + userId + '\'' +
                ", action='" + action + '\'' +
                ", status='" + status + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}

/**
 * Feedback model for Firestore 'feedback' collection
 * User feedback and bug reports
 */
class Feedback implements Serializable {
    private String feedbackId;
    private String userId;
    private String feedbackType; // BUG_REPORT, FEATURE_REQUEST, GENERAL_FEEDBACK
    private String title;
    private String description;
    private int rating; // 1-5
    private String screenshotUrl;
    private String appVersion;
    private String osVersion;
    private Timestamp createdAt;
    private String status; // OPEN, IN_REVIEW, RESOLVED, CLOSED
    private String adminNotes;

    public Feedback() {}

    // ========== Getters & Setters ==========
    public String getFeedbackId() { return feedbackId; }
    public void setFeedbackId(String feedbackId) { this.feedbackId = feedbackId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getFeedbackType() { return feedbackType; }
    public void setFeedbackType(String feedbackType) { this.feedbackType = feedbackType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public String getScreenshotUrl() { return screenshotUrl; }
    public void setScreenshotUrl(String screenshotUrl) { this.screenshotUrl = screenshotUrl; }

    public String getAppVersion() { return appVersion; }
    public void setAppVersion(String appVersion) { this.appVersion = appVersion; }

    public String getOsVersion() { return osVersion; }
    public void setOsVersion(String osVersion) { this.osVersion = osVersion; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }

    @Override
    public String toString() {
        return "Feedback{" +
                "feedbackId='" + feedbackId + '\'' +
                ", feedbackType='" + feedbackType + '\'' +
                ", rating=" + rating +
                ", status='" + status + '\'' +
                '}';
    }
}
