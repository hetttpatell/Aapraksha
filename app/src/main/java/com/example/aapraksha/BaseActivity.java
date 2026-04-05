package com.example.aapraksha;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * BaseActivity — extended by every Activity in AapRaksha.
 *
 * Detects when the user holds ANY volume button (up OR down) for 3 seconds
 * and launches SosTriggeredActivity — identical to pressing the SOS button
 * on the dashboard. No special permissions required; works on every screen
 * whenever the app is in the foreground.
 */
public class BaseActivity extends AppCompatActivity {

    private static final String TAG       = "BaseActivity";
    private static final long   HOLD_MS   = 3000L; // 3 seconds

    private final Handler  volumeHandler = new Handler(Looper.getMainLooper());
    private       boolean  sosScheduled  = false;

    private final Runnable sosRunnable = () -> {
        sosScheduled = false;
        Log.d(TAG, "Volume long-press SOS triggered");
        vibrateConfirmation();
        launchSOS();
    };

    // ─── Key event interception ───────────────────────────────────────────────

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
                || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {

            // Start the 3-second timer on first press (not on auto-repeat)
            if (event.getRepeatCount() == 0 && !sosScheduled) {
                sosScheduled = true;
                volumeHandler.postDelayed(sosRunnable, HOLD_MS);
                Log.d(TAG, "Volume held — SOS countdown started");
            }
            return true;   // consume → prevents volume bar from showing
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
                || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {

            // Released before 3 s — cancel the timer
            volumeHandler.removeCallbacks(sosRunnable);
            sosScheduled = false;
            Log.d(TAG, "Volume released — SOS countdown cancelled");
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Launches SosTriggeredActivity — same as pressing the SOS button on the dashboard.
     */
    private void launchSOS() {
        Intent intent = new Intent(this, SosTriggeredActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    /**
     * Vibrates twice to confirm SOS was triggered silently.
     */
    private void vibrateConfirmation() {
        try {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                long[] pattern = {0, 150, 80, 300};
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
            }
        } catch (Exception e) {
            Log.w(TAG, "Vibration failed: " + e.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        volumeHandler.removeCallbacks(sosRunnable);
    }
}
