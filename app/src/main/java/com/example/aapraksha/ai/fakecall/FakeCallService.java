package com.example.aapraksha.ai.fakecall;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.aapraksha.R;

import java.util.ArrayDeque;
import java.util.Deque;

public class FakeCallService extends Service implements SensorEventListener {

    private static final String TAG = "FakeCallService";
    private static final String CHANNEL_ID = "fake_call_protection_channel";
    private static final int NOTIFICATION_ID = 1017;
    private static final long WINDOW_MS = 2500;
    private static final float SHAKE_THRESHOLD = 11.5f;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private final Deque<Long> shakeTimestamps = new ArrayDeque<>();

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (accelerometer != null) {
                Log.d(TAG, "Accelerometer ready: " + accelerometer.getName());
            } else {
                Log.e(TAG, "Accelerometer sensor not available.");
            }
        } else {
            Log.e(TAG, "SensorManager is null.");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = buildNotification();
        if (notification == null) {
            Log.e(TAG, "Unable to start fake-call protection: notification build failed");
            stopSelf();
            return START_NOT_STICKY;
        }

        try {
            startForeground(NOTIFICATION_ID, notification);
        } catch (Exception e) {
            Log.e(TAG, "Unable to start fake-call protection in foreground", e);
            stopSelf();
            return START_NOT_STICKY;
        }

        if (sensorManager != null && accelerometer != null) {
            boolean registered = sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
            if (!registered) {
                Log.e(TAG, "Failed to register accelerometer listener");
            }
        } else {
            Log.w(TAG, "Accelerometer not available");
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sensorManager != null) sensorManager.unregisterListener(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        float magnitude = (float) Math.sqrt(x * x + y * y + z * z);
        float delta = Math.abs(magnitude - SensorManager.GRAVITY_EARTH);

        if (delta >= SHAKE_THRESHOLD) {
            long now = SystemClock.elapsedRealtime();
            shakeTimestamps.addLast(now);
            while (!shakeTimestamps.isEmpty() && (now - shakeTimestamps.peekFirst()) > WINDOW_MS) {
                shakeTimestamps.removeFirst();
            }
            if (shakeTimestamps.size() >= 3) {
                shakeTimestamps.clear();
                launchFakeCall();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void launchFakeCall() {
        Intent fakeCallIntent = new Intent(this, FakeCallActivity.class);
        fakeCallIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        fakeCallIntent.putExtra(FakeCallActivity.EXTRA_CALLER_NAME, "Maa");
        try {
            startActivity(fakeCallIntent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch FakeCallActivity", e);
        }
    }

    private Notification buildNotification() {
        try {
            Intent openIntent = new Intent(this, FakeCallActivity.class);
            openIntent.putExtra(FakeCallActivity.EXTRA_CALLER_NAME, "Maa");
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    openIntent,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                            ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
                            : PendingIntent.FLAG_UPDATE_CURRENT
            );

            return new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_menu_call)
                    .setContentTitle("Aapraksha Fake Call Protection")
                    .setContentText("Shake phone 3 times to trigger a distraction call")
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setOngoing(true)
                    .setContentIntent(pendingIntent)
                    .build();
        } catch (Exception e) {
            Log.e(TAG, "Failed to build fake-call notification", e);
            return null;
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Fake Call Protection",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}

