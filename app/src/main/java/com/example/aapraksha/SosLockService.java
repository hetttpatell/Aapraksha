package com.example.aapraksha;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.List;

/**
 * SosLockService — Foreground service that prevents the user from leaving
 * the SOS screen while an alert is active.
 *
 * When the user presses the Home button, swipes from recents, or uses
 * gestures to navigate away, this service detects that
 * SosTriggeredActivity is no longer in the foreground and immediately
 * relaunches it.
 *
 * The service is stopped ONLY when the SOS is cancelled via PIN
 * verification (cancelSos → stopSosLockService).
 */
public class SosLockService extends Service {

    private static final String TAG = "SosLockService";
    private static final String CHANNEL_ID = "sos_lock_channel";
    private static final int NOTIFICATION_ID = 2001;
    private static final long CHECK_INTERVAL_MS = 500; // check every 500ms

    private Handler handler;
    private Runnable lockCheckRunnable;
    private boolean isRunning = false;

    // Static flag so SosTriggeredActivity can signal it's being intentionally finished
    public static volatile boolean sosActive = false;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        Log.d(TAG, "SosLockService created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isRunning) {
            isRunning = true;
            sosActive = true;
            createNotificationChannel();
            startForeground(NOTIFICATION_ID, buildNotification());
            startLockCheck();
            Log.d(TAG, "SosLockService started — SOS screen lock active");
        }
        return START_STICKY;
    }

    /**
     * Periodically checks if SosTriggeredActivity is in the foreground.
     * If not (user navigated away), relaunches it immediately.
     */
    private void startLockCheck() {
        lockCheckRunnable = new Runnable() {
            @Override
            public void run() {
                if (!sosActive) {
                    stopSelf();
                    return;
                }

                if (!isSosActivityForeground()) {
                    Log.w(TAG, "SOS screen not in foreground — relaunching!");
                    relaunchSosScreen();
                }

                handler.postDelayed(this, CHECK_INTERVAL_MS);
            }
        };
        handler.postDelayed(lockCheckRunnable, CHECK_INTERVAL_MS);
    }

    /**
     * Checks if SosTriggeredActivity is currently the top activity.
     */
    private boolean isSosActivityForeground() {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) return false;

        List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
        if (processes == null) return false;

        for (ActivityManager.RunningAppProcessInfo processInfo : processes) {
            if (processInfo.processName.equals(getPackageName())
                    && processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                return true;
            }
        }
        return false;
    }

    /**
     * Relaunches SosTriggeredActivity, bringing it back to the foreground.
     */
    private void relaunchSosScreen() {
        Intent intent = new Intent(this, SosTriggeredActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("relaunch", true);
        startActivity(intent);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "SOS Alert Active",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Keeps SOS screen active during emergency");
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification() {
        Intent tapIntent = new Intent(this, SosTriggeredActivity.class);
        tapIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("🚨 SOS ALERT ACTIVE")
                .setContentText("Emergency alert is active. Enter PIN to cancel.")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        sosActive = false;
        if (handler != null && lockCheckRunnable != null) {
            handler.removeCallbacks(lockCheckRunnable);
        }
        Log.d(TAG, "SosLockService destroyed — lock released");
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        // If the user swipes the app from recents, relaunch the SOS screen
        if (sosActive) {
            Log.w(TAG, "Task removed while SOS active — relaunching!");
            relaunchSosScreen();
        }
        super.onTaskRemoved(rootIntent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
