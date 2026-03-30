package com.example.aapraksha;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

/**
 * Accessibility service to detect volume button long press for SOS trigger
 * Requires: Add to AndroidManifest.xml and enable in Accessibility settings
 */
public class VolumeButtonService extends AccessibilityService {

    private static final long LONG_PRESS_DELAY = 3000; // 3 seconds
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable longPressRunnable;
    private boolean isLongPressTriggered = false;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Not used for key events
    }

    @Override
    public void onInterrupt() {
        // Service interrupted
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo info = getServiceInfo();
        if (info != null) {
            info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
            setServiceInfo(info);
        }
    }

    @Override
    public boolean onKeyEvent(KeyEvent event) {
        int action = event.getAction();
        int keyCode = event.getKeyCode();

        // Detect volume button long press
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            switch (action) {
                case KeyEvent.ACTION_DOWN:
                    isLongPressTriggered = false;
                    if (longPressRunnable != null) {
                        handler.removeCallbacks(longPressRunnable);
                    }
                    longPressRunnable = () -> {
                        isLongPressTriggered = true;
                        showSOSTriggeredScreen();
                    };
                    handler.postDelayed(longPressRunnable, LONG_PRESS_DELAY);
                    break;

                case KeyEvent.ACTION_UP:
                    if (!isLongPressTriggered && longPressRunnable != null) {
                        handler.removeCallbacks(longPressRunnable);
                    }
                    isLongPressTriggered = false;
                    break;
            }
            // Consume event if long press triggered to prevent system volume change
            if (isLongPressTriggered) {
                return true;
            }
        }
        return super.onKeyEvent(event);
    }

    private void showSOSTriggeredScreen() {
        Intent intent = new Intent(this, SosTriggeredActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }
}
