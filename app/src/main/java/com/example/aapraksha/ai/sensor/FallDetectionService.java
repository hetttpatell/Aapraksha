package com.example.aapraksha.ai.sensor;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.aapraksha.checkin.CheckInNotificationHelper;

public class FallDetectionService extends Service implements SensorEventListener {
    private static final String TAG = "FallDetectionService";
    private static final String CHANNEL_ID = "FallDetectionChannel";
    private static final int NOTIFICATION_ID = 1004;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    
    // Thresholds
    private static final double FALL_THRESHOLD_LOW = 3.0; // Free fall approx 0
    private static final double FALL_THRESHOLD_HIGH = 25.0; // Impact spike

    private boolean isFreeFallDetected = false;
    private long freeFallTime = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "FallDetectionService starting...");

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Aapraksha AI")
                .setContentText("Monitoring for sudden falls or impacts")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .build();

        startForeground(NOTIFICATION_ID, notification);

        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Log.e(TAG, "Accelerometer not available!");
        }

        return START_STICKY;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            double gForce = Math.sqrt(x * x + y * y + z * z);

            // Phase 1 of fall: Free fall (near 0g)
            if (gForce < FALL_THRESHOLD_LOW) {
                if (!isFreeFallDetected) {
                    isFreeFallDetected = true;
                    freeFallTime = System.currentTimeMillis();
                }
            } 
            // Phase 2 of fall: Impact (Spike in G-force)
            else if (gForce > FALL_THRESHOLD_HIGH && isFreeFallDetected) {
                long timeDiff = System.currentTimeMillis() - freeFallTime;
                
                // Impact usually happens within a short window after free fall
                if (timeDiff < 2000) { 
                    Log.w(TAG, "Fall Detected! G-Force spike: " + gForce);
                    triggerFallCheckIn();
                }
                
                // Reset state
                isFreeFallDetected = false;
            } else {
                // Not free fall, not impact, just reset if it's been too long
                if (isFreeFallDetected && (System.currentTimeMillis() - freeFallTime > 2000)) {
                    isFreeFallDetected = false;
                }
            }
        }
    }

    private void triggerFallCheckIn() {
        CheckInNotificationHelper.showCheckIn(
                this,
                CheckInNotificationHelper.SOURCE_FALL_DETECTED,
                "A sudden fall or impact was detected. Are you safe?",
                null
        );
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "FallDetectionService destroying...");
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
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
                    "Fall Detection Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }
}
