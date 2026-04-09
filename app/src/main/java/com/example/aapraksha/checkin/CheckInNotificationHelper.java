package com.example.aapraksha.checkin;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.aapraksha.R;
import com.example.aapraksha.SosTriggeredActivity;

/**
 * CheckInNotificationHelper — Silent check-in notification system
 * 
 * Used by the Danger Intelligence Layer and Anomaly Detection to verify
 * user safety when a potential threat is detected.
 * 
 * Flow:
 * 1. A threat/anomaly is detected (danger zone entry, unusual pattern, audio threat)
 * 2. This helper sends a vibration + notification: "Are you okay? Tap to confirm."
 * 3. User has RESPONSE_WINDOW_MS (30 seconds) to respond
 * 4. If user confirms → log the event, no SOS triggered
 * 5. If no response → auto-trigger SOS flow
 * 
 * This prevents false SOS triggers while still protecting the user.
 */
public class CheckInNotificationHelper {

    private static final String TAG = "CheckInHelper";

    public static final String CHANNEL_ID = "aapraksha_checkin";
    public static final String CHANNEL_NAME = "Safety Check-Ins";
    public static final int NOTIFICATION_ID = 7001;

    public static final long RESPONSE_WINDOW_MS = 30_000; // 30 seconds to respond

    // Actions for notification buttons
    public static final String ACTION_CONFIRM_SAFE = "com.example.aapraksha.ACTION_CONFIRM_SAFE";
    public static final String ACTION_TRIGGER_SOS = "com.example.aapraksha.ACTION_TRIGGER_SOS";

    /**
     * Trigger source types — what detected the potential threat
     */
    public static final String SOURCE_DANGER_ZONE = "DANGER_ZONE";
    public static final String SOURCE_ANOMALY = "ANOMALY";
    public static final String SOURCE_AUDIO_THREAT = "AUDIO_THREAT";
    public static final String SOURCE_FALL_DETECTED = "FALL_DETECTED";

    private static CountDownTimer activeTimer;
    private static boolean isCheckInActive = false;

    /**
     * Listener for check-in result
     */
    public interface OnCheckInResultListener {
        void onConfirmedSafe(String source);

        void onNoResponse(String source); // → trigger SOS
    }

    /**
     * Create the notification channel (required for Android 8.0+).
     * Call this once during app initialization.
     */
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Safety check-in notifications when potential threats are detected");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 200, 500}); // Urgent pattern
            channel.setBypassDnd(true); // Important: bypass Do Not Disturb

            NotificationManager notificationManager =
                    context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private static boolean canPostNotifications(Context context) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        if (!notificationManager.areNotificationsEnabled()) {
            Log.w(TAG, "Notifications are disabled for this app");
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted");
                return false;
            }
        }

        return true;
    }

    /**
     * Show a check-in notification with a response window.
     * 
     * @param context Application context
     * @param source  What triggered the check-in (DANGER_ZONE, ANOMALY, etc.)
     * @param message Custom message to display
     * @param listener Callback for the result
     */
    public static void showCheckIn(Context context, String source, String message,
                                   OnCheckInResultListener listener) {
        showCheckIn(context, source, message, 0.0, 0.0, listener);
    }

    /**
     * Show a check-in notification with location data (Phase 9 enhanced version).
     * 
     * @param context Application context
     * @param source  What triggered the check-in
     * @param message Custom message to display
     * @param latitude User's current latitude
     * @param longitude User's current longitude
     * @param listener Callback for the result
     */
    public static void showCheckIn(Context context, String source, String message,
                                   double latitude, double longitude,
                                   OnCheckInResultListener listener) {
        if (isCheckInActive) {
            Log.w(TAG, "Check-in already active — ignoring duplicate");
            return;
        }

        createNotificationChannel(context);
        if (!canPostNotifications(context)) {
            Log.w(TAG, "Check-in notification suppressed due to missing permission or disabled settings");
            return;
        }

        isCheckInActive = true;

        // "I'm Safe" action
        Intent safeIntent = new Intent(context, CheckInActivity.class);
        safeIntent.setAction(ACTION_CONFIRM_SAFE);
        safeIntent.putExtra("source", source);
        safeIntent.putExtra("anomaly_reason", message);
        safeIntent.putExtra("latitude", latitude);
        safeIntent.putExtra("longitude", longitude);
        safeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent safePendingIntent = PendingIntent.getActivity(
                context, 0, safeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // "Help Me" action — goes directly to SOS
        Intent sosIntent = new Intent(context, SosTriggeredActivity.class);
        sosIntent.setAction(ACTION_TRIGGER_SOS);
        sosIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent sosPendingIntent = PendingIntent.getActivity(
                context, 1, sosIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build notification
        String title = getNotificationTitle(source);
        String body = message != null ? message : "Are you okay? Tap to let us know.";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body +
                        "\n\n⏰ Responding in 30 seconds or SOS will be triggered automatically."))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setOngoing(true) // Can't be swiped away
                .setContentIntent(safePendingIntent) // Tap = confirm safe
                .addAction(0, "✅ I'm Safe", safePendingIntent)
                .addAction(0, "🚨 Send SOS", sosPendingIntent)
                .setVibrate(new long[]{0, 800, 400, 800});

        boolean notified = false;
        try {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(NOTIFICATION_ID, builder.build());
            Log.d(TAG, "Check-in notification shown — source: " + source);
            notified = true;
        } catch (SecurityException e) {
            Log.e(TAG, "No notification permission", e);
        }

        if (!notified) {
            isCheckInActive = false;
            return;
        }

        // Start the response countdown
        startResponseTimer(context, source, listener);
    }

    /**
     * Start a countdown timer. If user doesn't respond in RESPONSE_WINDOW_MS,
     * trigger the SOS flow automatically.
     */
    private static void startResponseTimer(Context context, String source,
                                           OnCheckInResultListener listener) {
        cancelActiveTimer();

        activeTimer = new CountDownTimer(RESPONSE_WINDOW_MS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                // Could update notification with remaining time if needed
            }

            @Override
            public void onFinish() {
                Log.w(TAG, "Check-in timed out — NO RESPONSE for source: " + source);
                isCheckInActive = false;
                dismissNotification(context);

                if (listener != null) {
                    listener.onNoResponse(source);
                }

                // Auto-trigger SOS
                Intent sosIntent = new Intent(context, SosTriggeredActivity.class);
                sosIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                context.startActivity(sosIntent);
            }
        }.start();
    }

    /**
     * Call this when the user confirms they're safe (from CheckInActivity).
     */
    public static void confirmSafe(Context context, String source, OnCheckInResultListener listener) {
        Log.d(TAG, "User confirmed safe — source: " + source);
        isCheckInActive = false;
        cancelActiveTimer();
        dismissNotification(context);

        if (listener != null) {
            listener.onConfirmedSafe(source);
        }
    }

    /**
     * Cancel any active check-in timer
     */
    public static void cancelActiveTimer() {
        if (activeTimer != null) {
            activeTimer.cancel();
            activeTimer = null;
        }
    }

    /**
     * Dismiss the check-in notification
     */
    public static void dismissNotification(Context context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID);
    }

    /**
     * Get appropriate notification title based on trigger source
     */
    private static String getNotificationTitle(String source) {
        if (source == null) return "⚠️ Safety Check-In";

        switch (source) {
            case SOURCE_DANGER_ZONE:
                return "⚠️ You've entered a high-risk area";
            case SOURCE_ANOMALY:
                return "🔍 Unusual activity detected";
            case SOURCE_AUDIO_THREAT:
                return "🔊 Concerning sound detected nearby";
            case SOURCE_FALL_DETECTED:
                return "📱 Fall or impact detected";
            default:
                return "⚠️ Safety Check-In";
        }
    }

    /**
     * Check if a check-in is currently active
     */
    public static boolean isCheckInActive() {
        return isCheckInActive;
    }
}
