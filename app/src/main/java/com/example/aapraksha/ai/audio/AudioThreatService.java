package com.example.aapraksha.ai.audio;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.aapraksha.checkin.CheckInNotificationHelper;

public class AudioThreatService extends Service {
    private static final String TAG = "AudioThreatService";
    private static final String CHANNEL_ID = "AudioThreatServiceChannel";
    private static final int NOTIFICATION_ID = 1002;

    private AudioClassifierHelper audioHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "AudioThreatService starting...");

        // Run as a foreground service so it isn't killed while listening
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Aapraksha AI")
                .setContentText("Monitoring environment for audio threats (Screams, Gunshots, Glass)")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .build();

        startForeground(NOTIFICATION_ID, notification);

        if (audioHelper == null) {
            audioHelper = new AudioClassifierHelper(this);
        }

        audioHelper.startListening((threatType, confidence) -> {
            Log.w(TAG, "Detected audio threat: " + threatType + ". Triggering Check-In Protocol.");
            // Trigger the check-in mechanism from Phase 1
            CheckInNotificationHelper.showCheckIn(
                    this,
                    CheckInNotificationHelper.SOURCE_AUDIO_THREAT,
                    "Distress sound detected (" + threatType + "). Are you safe?",
                    null
            );
        });

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "AudioThreatService destroying...");
        if (audioHelper != null) {
            audioHelper.stopListening();
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
                    "Audio Threat Monitoring Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }
}
