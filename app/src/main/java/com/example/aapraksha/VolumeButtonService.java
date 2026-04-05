package com.example.aapraksha;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

/**
 * Accessibility service to detect a volume button held for 3 seconds
 * to trigger a full SOS — identical to pressing the SOS button on the dashboard.
 *
 * KEY DESIGN NOTES:
 * ─────────────────
 * • Accessibility services are managed entirely by the Android system.
 *   They do NOT need startForeground(); Android keeps them alive
 *   independently of the app process.
 * • Do NOT call startForeground() here — on Android 14+ (API 34) it will
 *   crash with MissingForegroundServiceTypeException because this service
 *   has no foregroundServiceType in the manifest (and shouldn't have one).
 * • The service works even when the app is swiped from Recents,
 *   the screen is off, or the device is locked — as long as the user
 *   has enabled it in Settings → Accessibility.
 *
 * Requires: Enabled in device Accessibility Settings.
 */
public class VolumeButtonService extends AccessibilityService {

    private static final String TAG = "VolumeButtonService";
    private static final long LONG_PRESS_DELAY = 3000; // 3 seconds

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable sosRunnable;
    private PowerManager.WakeLock wakeLock;

    private boolean sosTriggered = false;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo info = getServiceInfo();
        if (info != null) {
            info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
            setServiceInfo(info);
        }

        // No startForeground() here!
        // Accessibility services are system-managed and persist on their own.
        Log.d(TAG, "VolumeButtonService connected — ready to detect volume SOS");
    }

    @Override
    public boolean onKeyEvent(KeyEvent event) {
        int action  = event.getAction();
        int keyCode = event.getKeyCode();

        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            switch (action) {
                case KeyEvent.ACTION_DOWN:
                    sosTriggered = false;
                    if (sosRunnable != null) {
                        handler.removeCallbacks(sosRunnable);
                    }

                    // Acquire wake lock to ensure the 3-second timer fires
                    // even if the CPU is asleep (screen off)
                    acquireWakeLock();

                    sosRunnable = () -> {
                        sosTriggered = true;
                        vibrateConfirmation();
                        triggerSOS();
                    };
                    handler.postDelayed(sosRunnable, LONG_PRESS_DELAY);
                    Log.d(TAG, "Volume button held — SOS countdown started");
                    break;

                case KeyEvent.ACTION_UP:
                    if (!sosTriggered && sosRunnable != null) {
                        handler.removeCallbacks(sosRunnable);
                    }
                    releaseWakeLock();
                    sosTriggered = false;
                    break;
            }
            // Consume event if long press triggered to prevent system volume change
            if (sosTriggered) {
                return true;
            }
        }
        return super.onKeyEvent(event);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  WAKE LOCK — keeps CPU awake for the 3-second countdown
    // ═══════════════════════════════════════════════════════════════════

    private void acquireWakeLock() {
        releaseWakeLock(); // release any previous one first
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            wakeLock = pm.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "AapRaksha:VolumeSOSWakeLock");
            wakeLock.acquire(5000); // 5 seconds max (covers 3s delay + launch)
        }
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            try {
                wakeLock.release();
            } catch (Exception e) {
                Log.w(TAG, "WakeLock release error: " + e.getMessage());
            }
            wakeLock = null;
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  TRIGGER SOS — wakes the screen and launches SosTriggeredActivity
    // ═══════════════════════════════════════════════════════════════════

    private void triggerSOS() {
        Log.d(TAG, "Volume SOS triggered! Waking screen and launching SosTriggeredActivity.");

        // Wake the screen so the activity can be displayed
        wakeScreen();

        // Release the countdown wake lock (screen wake lock will keep things alive)
        releaseWakeLock();

        Intent intent = new Intent(this, SosTriggeredActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);

        // Reset state after launch
        sosRunnable = null;
    }

    /**
     * Turns the screen on if it's off.
     * SosTriggeredActivity already has setShowWhenLocked / setTurnScreenOn
     * but we need a brief wake lock to get the CPU & screen active
     * long enough for the activity to launch.
     */
    private void wakeScreen() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm == null) return;

        if (!pm.isInteractive()) {
            // Screen is off — acquire a screen-on wake lock briefly
            @SuppressWarnings("deprecation")
            PowerManager.WakeLock screenLock = pm.newWakeLock(
                    PowerManager.FULL_WAKE_LOCK
                            | PowerManager.ACQUIRE_CAUSES_WAKEUP
                            | PowerManager.ON_AFTER_RELEASE,
                    "AapRaksha:SOSScreenWake");
            screenLock.acquire(10000); // 10 seconds — SosTriggeredActivity takes over

            // Schedule release so we don't leak
            handler.postDelayed(() -> {
                if (screenLock.isHeld()) {
                    screenLock.release();
                }
            }, 10000);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  VIBRATION — confirms SOS was triggered
    // ═══════════════════════════════════════════════════════════════════

    /** Short double-pulse vibration to confirm SOS was triggered */
    private void vibrateConfirmation() {
        try {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                // Pattern: wait 0ms, vibrate 200ms, pause 100ms, vibrate 400ms
                long[] pattern = {0, 200, 100, 400};
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
            }
        } catch (Exception e) {
            Log.w(TAG, "Vibration failed: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  REQUIRED OVERRIDES
    // ═══════════════════════════════════════════════════════════════════

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Not needed for key event detection
    }

    @Override
    public void onInterrupt() {
        cancelTimer();
        Log.d(TAG, "VolumeButtonService interrupted");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cancelTimer();
        Log.d(TAG, "VolumeButtonService destroyed");
    }

    private void cancelTimer() {
        if (sosRunnable != null) {
            handler.removeCallbacks(sosRunnable);
            sosRunnable = null;
        }
        releaseWakeLock();
        sosTriggered = false;
    }
}
