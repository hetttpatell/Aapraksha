package com.example.aapraksha.ai.anomaly;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.example.aapraksha.GeoHashUtil;
import com.example.aapraksha.checkin.CheckInNotificationHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AnomalyDetectionService extends Service {
    private static final String TAG = "AnomalyDetectService";
    private static final String CHANNEL_ID = "AnomalyDetectionChannel";
    private static final int NOTIFICATION_ID = 1003;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private AppDatabase db;
    private ExecutorService executor;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        db = AppDatabase.getDatabase(this);
        executor = Executors.newSingleThreadExecutor();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    analyzeLocation(location);
                }
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "AnomalyDetectionService starting...");

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Aapraksha AI")
                .setContentText("Monitoring routine for unexpected anomalies")
                .setSmallIcon(android.R.drawable.ic_dialog_map)
                .build();

        startForeground(NOTIFICATION_ID, notification);

        startLocationUpdates();

        return START_STICKY;
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Missing location permissions");
            return;
        }

        // Check location every 15 minutes to save battery, but detect anomalies
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 15 * 60 * 1000)
                .setMinUpdateDistanceMeters(100)
                .build();

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void analyzeLocation(Location location) {
        executor.execute(() -> {
            Calendar calendar = Calendar.getInstance();
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
            
            // Generate geohash of length 6 (approx 1.2km x 600m area)
            String currentGeohash = GeoHashUtil.encode(location.getLatitude(), location.getLongitude(), 6);

            RoutineProfile profile = db.routineDao().getProfile(currentGeohash, dayOfWeek, hourOfDay);

            if (profile == null) {
                // New location for this time!
                Log.w(TAG, "Anomaly detected: Unusual location for this time.");
                
                // Save it for future learning
                RoutineProfile newProfile = new RoutineProfile();
                newProfile.latitude = location.getLatitude();
                newProfile.longitude = location.getLongitude();
                newProfile.geohash = currentGeohash;
                newProfile.dayOfWeek = dayOfWeek;
                newProfile.hourOfDay = hourOfDay;
                newProfile.visitCount = 1;
                newProfile.lastVisitMs = System.currentTimeMillis();
                newProfile.locationType = "UNKNOWN";
                db.routineDao().insert(newProfile);

                // Ask the user if they are okay if it's late night (e.g. 10 PM - 5 AM)
                if (hourOfDay >= 22 || hourOfDay <= 5) {
                    CheckInNotificationHelper.showCheckIn(
                            AnomalyDetectionService.this,
                            CheckInNotificationHelper.SOURCE_ANOMALY,
                            "You are at a new location late at night. Are you okay?",
                            null
                    );
                }
            } else {
                // Known location, update frequency
                profile.visitCount++;
                profile.lastVisitMs = System.currentTimeMillis();
                if (profile.visitCount > 10) {
                    profile.locationType = "FREQUENT";
                }
                db.routineDao().update(profile);
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "AnomalyDetectionService destroying...");
        if (fusedLocationClient != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Anomaly Detection Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }
}
