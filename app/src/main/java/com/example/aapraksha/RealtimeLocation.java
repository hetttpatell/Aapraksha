package com.example.aapraksha;

import com.google.firebase.Timestamp;
import java.io.Serializable;

/**
 * RealtimeLocation model for Firebase Realtime Database
 * Tracks real-time GPS location during active SOS alerts
 */
public class RealtimeLocation implements Serializable {
    private double latitude;
    private double longitude;
    private double accuracy; // GPS accuracy in meters
    private double altitude; // elevation
    private double heading; // direction 0-360 degrees
    private double speed; // km/h
    private long timestamp; // milliseconds since epoch
    
    private String alertId; // reference to active SOS
    private boolean isActive; // true only if SOS is active
    private int updateFrequency; // seconds between updates
    private String provider; // GPS, NETWORK, FUSED
    private int batteryLevel; // 0-100
    private String accuracyStatus; // HIGH, MEDIUM, LOW

    public RealtimeLocation() {}

    // Constructor with essential location data
    public RealtimeLocation(double latitude, double longitude, double accuracy) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracy = accuracy;
        this.timestamp = System.currentTimeMillis();
        this.batteryLevel = 100;
        this.accuracyStatus = "HIGH";
    }

    // ========== Getters & Setters ==========
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public double getAccuracy() { return accuracy; }
    public void setAccuracy(double accuracy) { this.accuracy = accuracy; }

    public double getAltitude() { return altitude; }
    public void setAltitude(double altitude) { this.altitude = altitude; }

    public double getHeading() { return heading; }
    public void setHeading(double heading) { this.heading = heading; }

    public double getSpeed() { return speed; }
    public void setSpeed(double speed) { this.speed = speed; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getAlertId() { return alertId; }
    public void setAlertId(String alertId) { this.alertId = alertId; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public int getUpdateFrequency() { return updateFrequency; }
    public void setUpdateFrequency(int updateFrequency) { this.updateFrequency = updateFrequency; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public int getBatteryLevel() { return batteryLevel; }
    public void setBatteryLevel(int batteryLevel) { this.batteryLevel = batteryLevel; }

    public String getAccuracyStatus() { return accuracyStatus; }
    public void setAccuracyStatus(String accuracyStatus) { this.accuracyStatus = accuracyStatus; }

    @Override
    public String toString() {
        return "RealtimeLocation{" +
                "latitude=" + latitude +
                ", longitude=" + longitude +
                ", accuracy=" + accuracy +
                ", alertId='" + alertId + '\'' +
                ", isActive=" + isActive +
                ", timestamp=" + timestamp +
                '}';
    }
}

/**
 * ContactedLocation model for Firebase Realtime Database
 * Tracks response contact's real-time location during SOS
 */
class ContactedLocation implements Serializable {
    private String contactId;
    private String userId; // user being helped
    private double latitude;
    private double longitude;
    private long timestamp;
    private String alertId;
    private String status; // IN_TRANSIT, REACHED, STOPPED

    public ContactedLocation() {}

    // ========== Getters & Setters ==========
    public String getContactId() { return contactId; }
    public void setContactId(String contactId) { this.contactId = contactId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getAlertId() { return alertId; }
    public void setAlertId(String alertId) { this.alertId = alertId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return "ContactedLocation{" +
                "contactId='" + contactId + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", status='" + status + '\'' +
                '}';
    }
}
