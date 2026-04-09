package com.example.aapraksha.checkin;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.concurrent.TimeUnit;

/**
 * CheckInScheduler — Schedules periodic safety check-ins using WorkManager
 * 
 * Phase 9 Implementation:
 * - Schedules check-ins at user-configurable intervals (30 min, 1 hour, 2 hours)
 * - Only triggers when user is outside home geofence (smart timing)
 * - Cancels check-ins when user is at home
 * - Persists across device restarts
 * 
 * Usage:
 * - CheckInScheduler.schedule(context, intervalMinutes)
 * - CheckInScheduler.cancel(context)
 * - CheckInScheduler.isScheduled(context)
 */
public class CheckInScheduler {

    private static final String TAG = "CheckInScheduler";
    private static final String WORK_TAG = "aapraksha_scheduled_checkin";
    private static final String PREF_NAME = "checkin_scheduler_prefs";
    private static final String PREF_ENABLED = "checkin_enabled";
    private static final String PREF_INTERVAL = "checkin_interval_minutes";
    private static final String PREF_HOME_LAT = "home_latitude";
    private static final String PREF_HOME_LNG = "home_longitude";

    /**
     * Schedule periodic check-ins
     * 
     * @param context Application context
     * @param intervalMinutes Interval between check-ins (30, 60, or 120)
     */
    public static void schedule(Context context, int intervalMinutes) {
        if (intervalMinutes < 30) {
            intervalMinutes = 30; // Minimum 30 minutes
        }

        // Save preferences
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putBoolean(PREF_ENABLED, true)
                .putInt(PREF_INTERVAL, intervalMinutes)
                .apply();

        // Schedule work
        PeriodicWorkRequest checkInWorkRequest = new PeriodicWorkRequest.Builder(
                CheckInWorker.class,
                intervalMinutes, TimeUnit.MINUTES,
                15, TimeUnit.MINUTES // Flex interval
        )
                .addTag(WORK_TAG)
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_TAG,
                ExistingPeriodicWorkPolicy.UPDATE,
                checkInWorkRequest
        );

        Log.d(TAG, "Scheduled check-ins every " + intervalMinutes + " minutes");
    }

    /**
     * Cancel all scheduled check-ins
     */
    public static void cancel(Context context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(WORK_TAG);

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(PREF_ENABLED, false).apply();

        Log.d(TAG, "Cancelled all scheduled check-ins");
    }

    /**
     * Check if check-ins are currently scheduled
     */
    public static boolean isScheduled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(PREF_ENABLED, false);
    }

    /**
     * Set home location (safe zone)
     * Check-ins won't trigger when user is at home
     */
    public static void setHomeLocation(Context context, double latitude, double longitude) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putLong(PREF_HOME_LAT, Double.doubleToRawLongBits(latitude))
                .putLong(PREF_HOME_LNG, Double.doubleToRawLongBits(longitude))
                .apply();

        Log.d(TAG, "Home location set: " + latitude + ", " + longitude);
    }

    /**
     * Get home location
     */
    private static double[] getHomeLocation(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        long latBits = prefs.getLong(PREF_HOME_LAT, 0);
        long lngBits = prefs.getLong(PREF_HOME_LNG, 0);

        if (latBits == 0 && lngBits == 0) {
            return null; // No home location set
        }

        return new double[]{
                Double.longBitsToDouble(latBits),
                Double.longBitsToDouble(lngBits)
        };
    }

    /**
     * Check if user is at home (within 200 meters)
     */
    private static boolean isUserAtHome(Context context, double currentLat, double currentLng) {
        double[] homeLocation = getHomeLocation(context);
        if (homeLocation == null) {
            return false; // No home location set, proceed with check-in
        }

        double homeLat = homeLocation[0];
        double homeLng = homeLocation[1];

        // Calculate distance using Haversine formula
        double distance = calculateDistance(currentLat, currentLng, homeLat, homeLng);

        return distance < 200; // Within 200 meters of home
    }

    /**
     * Calculate distance between two coordinates in meters
     */
    private static double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        final int EARTH_RADIUS = 6371000; // meters

        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c;
    }

    /**
     * Worker that runs the scheduled check-in
     */
    public static class CheckInWorker extends Worker {

        private static final String TAG = "CheckInWorker";

        public CheckInWorker(@NonNull Context context, @NonNull WorkerParameters params) {
            super(context, params);
        }

        @NonNull
        @Override
        public Result doWork() {
            Log.d(TAG, "Scheduled check-in triggered");

            // Get current location
            FusedLocationProviderClient locationClient =
                    LocationServices.getFusedLocationProviderClient(getApplicationContext());

            try {
                locationClient.getLastLocation().addOnSuccessListener(location -> {
                    if (location != null) {
                        double lat = location.getLatitude();
                        double lng = location.getLongitude();

                        // Check if user is at home
                        if (isUserAtHome(getApplicationContext(), lat, lng)) {
                            Log.d(TAG, "User is at home — skipping check-in");
                            return;
                        }

                        // Log scheduled check-in
                        CheckInRepository.logScheduledCheckIn(getApplicationContext(), lat, lng);

                        // Show check-in notification
                        CheckInNotificationHelper.showCheckIn(
                                getApplicationContext(),
                                "SCHEDULED",
                                "Scheduled safety check-in — please confirm you're okay",
                                lat,
                                lng,
                                new CheckInNotificationHelper.OnCheckInResultListener() {
                                    @Override
                                    public void onConfirmedSafe(String source) {
                                        Log.d(TAG, "User responded to scheduled check-in");
                                    }

                                    @Override
                                    public void onNoResponse(String source) {
                                        Log.w(TAG, "No response to scheduled check-in — SOS triggered");
                                    }
                                }
                        );
                    } else {
                        Log.w(TAG, "Location unavailable — skipping check-in");
                    }
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get location for scheduled check-in", e);
                });
            } catch (SecurityException e) {
                Log.e(TAG, "No location permission", e);
            }

            return Result.success();
        }
    }
}
