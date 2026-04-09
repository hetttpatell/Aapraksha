package com.example.aapraksha.ai;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import com.example.aapraksha.ai.fakecall.FakeCallService;

/**
 * Boot Receiver — restarts AI services after device reboot.
 * 
 * Currently a placeholder for Phase 2+ when services like 
 * AudioThreatService and AnomalyDetectionService need to be
 * restarted automatically after a reboot.
 * 
 * Services are only restarted if the user had them enabled
 * in their settings (checked via SharedPreferences).
 */
public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "Device booted — checking which AI services to restart");
            try {
                SharedPreferences prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
                boolean fakeCallEnabled = prefs.getBoolean("fake_call_shake_enabled", true);
                if (fakeCallEnabled) {
                    Intent fakeCallIntent = new Intent(context, FakeCallService.class);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(fakeCallIntent);
                    } else {
                        context.startService(fakeCallIntent);
                    }
                    Log.d(TAG, "FakeCallService restarted after boot");
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to restart FakeCallService after boot", e);
            }
        }
    }
}
