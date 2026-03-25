package com.example.aapraksha;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class LocationTrackingService extends Service implements LocationListener {
    private static final String TAG = "LocationTrackingService";
    private static final long UPDATE_INTERVAL = 5000; // 5 seconds
    private static final float MIN_DISTANCE = 0; // 0 meters
    
    private LocationManager locationManager;
    private FirebaseDatabase realtimeDb;
    private FirebaseAuth auth;
    private SOSAlertRepository sosAlertRepository;
    private String currentAlertId;
    private String userId;
    private boolean isTracking = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "LocationTrackingService created");
        
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        realtimeDb = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();
        sosAlertRepository = new SOSAlertRepository();
        
        if (auth.getCurrentUser() != null) {
            userId = auth.getCurrentUser().getUid();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "LocationTrackingService started");
        
        if (intent != null && intent.getStringExtra("alertId") != null) {
            currentAlertId = intent.getStringExtra("alertId");
            startLocationTracking();
        }
        
        return START_STICKY; // Service will be restarted if killed
    }

    /**
     * Start tracking user's real-time location
     */
    private void startLocationTracking() {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                
                Log.d(TAG, "Starting location tracking for alert: " + currentAlertId);
                isTracking = true;
                
                // Request updates from GPS provider
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        UPDATE_INTERVAL,
                        MIN_DISTANCE,
                        this
                );
                
                // Also use network provider as fallback
                locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        UPDATE_INTERVAL,
                        MIN_DISTANCE,
                        this
                );
                
                // Get last known location immediately
                Location lastLocation = getLastKnownLocation();
                if (lastLocation != null) {
                    sendLocationToFirebase(lastLocation);
                }
                
            } else {
                Log.w(TAG, "Location permission not granted");
                stopLocationTracking();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error starting location tracking: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Get last known location from either GPS or Network
     */
    private Location getLastKnownLocation() {
        try {
            Location gpsLocation = null;
            Location networkLocation = null;
            
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                
                gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
            
            if (gpsLocation != null && networkLocation != null) {
                // Return most recent location
                return gpsLocation.getTime() > networkLocation.getTime() ? gpsLocation : networkLocation;
            }
            return gpsLocation != null ? gpsLocation : networkLocation;
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting last known location: " + e.getMessage());
            return null;
        }
    }

    /**
     * Send location to Firebase (Firestore + Realtime DB)
     */
    private void sendLocationToFirebase(Location location) {
        if (location == null || currentAlertId == null || userId == null) {
            return;
        }
        
        try {
            // Build location data
            Map<String, Object> locationData = new HashMap<>();
            locationData.put("latitude", location.getLatitude());
            locationData.put("longitude", location.getLongitude());
            locationData.put("accuracy", location.getAccuracy());
            locationData.put("altitude", location.getAltitude());
            locationData.put("bearing", location.getBearing());
            locationData.put("speed", location.getSpeed());
            locationData.put("timestamp", System.currentTimeMillis());
            locationData.put("provider", location.getProvider());

            // Send to Firestore (for persistence)
            sosAlertRepository.updateAlertLocation(
                    currentAlertId,
                    location.getLatitude(),
                    location.getLongitude(),
                    location.getAccuracy(),
                    new SOSAlertRepository.OnAlertStatusChangedListener() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "Location updated in Firestore");
                        }

                        @Override
                        public void onError(String errorMessage) {
                            Log.e(TAG, "Failed to update location: " + errorMessage);
                        }
                    }
            );

            // Send to Realtime Database (for real-time sync)
            DatabaseReference locRef = realtimeDb
                    .getReference("realtime_locations")
                    .child(userId)
                    .child(currentAlertId);
            
            locRef.setValue(locationData)
                    .addOnSuccessListener(aVoid -> 
                            Log.d(TAG, "Location sent to Realtime DB"))
                    .addOnFailureListener(e -> 
                            Log.e(TAG, "Failed to send location to Realtime DB: " + e.getMessage()));

        } catch (Exception e) {
            Log.e(TAG, "Error sending location to Firebase: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Stop location tracking
     */
    private void stopLocationTracking() {
        try {
            if (locationManager != null && isTracking) {
                locationManager.removeUpdates(this);
                isTracking = false;
                Log.d(TAG, "Location tracking stopped");
                
                // Clean up Realtime DB entries
                if (userId != null && currentAlertId != null) {
                    DatabaseReference locRef = realtimeDb
                            .getReference("realtime_locations")
                            .child(userId)
                            .child(currentAlertId);
                    locRef.removeValue();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error stopping location tracking: " + e.getMessage());
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "Location changed: " + location.getLatitude() + ", " + location.getLongitude());
        sendLocationToFirebase(location);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d(TAG, "Location provider status changed: " + provider + " status: " + status);
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d(TAG, "Location provider enabled: " + provider);
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d(TAG, "Location provider disabled: " + provider);
        // Try to use other provider
        if (LocationManager.GPS_PROVIDER.equals(provider)) {
            Log.d(TAG, "GPS disabled, trying network provider");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "LocationTrackingService destroyed");
        stopLocationTracking();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
