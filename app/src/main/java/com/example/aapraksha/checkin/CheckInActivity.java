package com.example.aapraksha.checkin;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.aapraksha.R;
import com.example.aapraksha.SOSAlertRepository;
import com.example.aapraksha.SosTriggeredActivity;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * CheckInActivity — Enhanced "Are you okay?" confirmation screen
 * 
 * Phase 9 Implementation:
 * - Displays full anomaly context (type, reason, location, time)
 * - Shows 30-second countdown timer with visual progress bar
 * - Large "I'm Safe" button (green) to confirm safety
 * - "Send Help" button (red) to trigger SOS immediately
 * - Auto-triggers SOS if user doesn't respond within 30 seconds
 * - Vibrates on launch to alert user
 * 
 * Launched when:
 * - User taps check-in notification
 * - Anomaly detection requires user confirmation
 * - Audio threat or fall detected
 */
public class CheckInActivity extends AppCompatActivity {

    private static final String TAG = "CheckInActivity";
    private static final int COUNTDOWN_SECONDS = 30;

    private TextView tvAnomalyType, tvAnomalyReason, tvLocation, tvTime, tvCountdown;
    private ProgressBar progressCountdown;
    private MaterialButton btnImSafe, btnSendHelp;
    private CountDownTimer countdownTimer;

    private String source;
    private String anomalyReason;
    private double latitude;
    private double longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_in);

        initializeViews();
        extractIntentData();
        setupViews();
        setupButtons();
        startCountdown();
        vibrateAlert();
    }

    private void initializeViews() {
        tvAnomalyType = findViewById(R.id.tvAnomalyType);
        tvAnomalyReason = findViewById(R.id.tvAnomalyReason);
        tvLocation = findViewById(R.id.tvLocation);
        tvTime = findViewById(R.id.tvTime);
        tvCountdown = findViewById(R.id.tvCountdown);
        progressCountdown = findViewById(R.id.progressCountdown);
        btnImSafe = findViewById(R.id.btnImSafe);
        btnSendHelp = findViewById(R.id.btnSendHelp);
    }

    private void extractIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            source = intent.getStringExtra("source");
            anomalyReason = intent.getStringExtra("anomaly_reason");
            latitude = intent.getDoubleExtra("latitude", 0.0);
            longitude = intent.getDoubleExtra("longitude", 0.0);
        }

        if (source == null) source = "UNKNOWN";
        if (anomalyReason == null) anomalyReason = "Safety check-in required";
    }

    private void setupViews() {
        // Set anomaly type based on source
        String anomalyTypeText = getAnomalyTypeText(source);
        tvAnomalyType.setText(anomalyTypeText);

        // Set anomaly reason
        tvAnomalyReason.setText(anomalyReason);

        // Set location
        if (latitude != 0.0 && longitude != 0.0) {
            loadLocationAddress(latitude, longitude);
        } else {
            tvLocation.setText("Location unavailable");
        }

        // Set time
        String currentTime = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
        tvTime.setText(currentTime);

        // Setup progress bar
        progressCountdown.setMax(COUNTDOWN_SECONDS);
        progressCountdown.setProgress(COUNTDOWN_SECONDS);
    }

    private String getAnomalyTypeText(String source) {
        switch (source) {
            case CheckInNotificationHelper.SOURCE_DANGER_ZONE:
                return "⚠️ High-Risk Area Detected";
            case CheckInNotificationHelper.SOURCE_ANOMALY:
                return "🔍 Unusual Activity Pattern";
            case CheckInNotificationHelper.SOURCE_AUDIO_THREAT:
                return "🔊 Audio Threat Detected";
            case CheckInNotificationHelper.SOURCE_FALL_DETECTED:
                return "📱 Fall or Impact Detected";
            default:
                return "⚠️ Safety Check-In Required";
        }
    }

    private void loadLocationAddress(double lat, double lng) {
        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
                
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    String addressText = address.getAddressLine(0);
                    if (addressText == null) {
                        addressText = address.getLocality() + ", " + address.getAdminArea();
                    }
                    
                    final String finalAddress = addressText;
                    runOnUiThread(() -> tvLocation.setText(finalAddress));
                } else {
                    runOnUiThread(() -> tvLocation.setText(String.format("%.4f, %.4f", lat, lng)));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting address", e);
                runOnUiThread(() -> tvLocation.setText(String.format("%.4f, %.4f", lat, lng)));
            }
        }).start();
    }

    private void setupButtons() {
        // I'm Safe button
        btnImSafe.setOnClickListener(v -> {
            Log.d(TAG, "User confirmed safe — source: " + source);
            cancelCountdown();
            CheckInNotificationHelper.confirmSafe(this, source, null);
            
            // Log to repository
            CheckInRepository.logCheckInResponse(
                this,
                source,
                "safe",
                true,
                getElapsedTime()
            );
            
            showSuccessAndFinish();
        });

        // Send Help button
        btnSendHelp.setOnClickListener(v -> {
            Log.w(TAG, "User requested help — triggering SOS — source: " + source);
            cancelCountdown();
            CheckInNotificationHelper.dismissNotification(this);
            
            // Log to repository
            CheckInRepository.logCheckInResponse(
                this,
                source,
                "help",
                true,
                getElapsedTime()
            );
            
            // Trigger SOS immediately
            triggerSOS();
        });
    }

    private void startCountdown() {
        countdownTimer = new CountDownTimer(COUNTDOWN_SECONDS * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secondsRemaining = (int) (millisUntilFinished / 1000);
                tvCountdown.setText(String.valueOf(secondsRemaining));
                progressCountdown.setProgress(secondsRemaining);
                
                // Vibrate at 10 seconds and 5 seconds remaining
                if (secondsRemaining == 10 || secondsRemaining == 5) {
                    vibrateShort();
                }
            }

            @Override
            public void onFinish() {
                tvCountdown.setText("0");
                progressCountdown.setProgress(0);
                Log.w(TAG, "Check-in timed out — NO RESPONSE — auto-triggering SOS");
                
                // Log timeout to repository
                CheckInRepository.logCheckInResponse(
                    CheckInActivity.this,
                    source,
                    "timeout",
                    false,
                    COUNTDOWN_SECONDS
                );
                
                // Auto-trigger SOS
                autoTriggerSOS();
            }
        }.start();
    }

    private void cancelCountdown() {
        if (countdownTimer != null) {
            countdownTimer.cancel();
            countdownTimer = null;
        }
    }

    private int getElapsedTime() {
        if (progressCountdown != null) {
            int remaining = progressCountdown.getProgress();
            return COUNTDOWN_SECONDS - remaining;
        }
        return 0;
    }

    private void vibrateAlert() {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            // Three long vibrations
            long[] pattern = {0, 800, 200, 800, 200, 800};
            vibrator.vibrate(pattern, -1);
        }
    }

    private void vibrateShort() {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(200);
        }
    }

    private void showSuccessAndFinish() {
        // Update UI to show success
        btnImSafe.setText("✅ Confirmed Safe");
        btnImSafe.setEnabled(false);
        btnSendHelp.setVisibility(View.GONE);
        tvCountdown.setText("✓");
        tvCountdown.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        
        // Close after 1.5 seconds
        new android.os.Handler().postDelayed(this::finish, 1500);
    }

    private void triggerSOS() {
        // Launch SOS Triggered Activity
        Intent sosIntent = new Intent(this, SosTriggeredActivity.class);
        sosIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        sosIntent.putExtra("triggered_by", "check_in_manual");
        sosIntent.putExtra("source", source);
        startActivity(sosIntent);
        finish();
    }

    private void autoTriggerSOS() {
        // Launch SOS Triggered Activity with auto flag
        Intent sosIntent = new Intent(this, SosTriggeredActivity.class);
        sosIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        sosIntent.putExtra("triggered_by", "check_in_timeout");
        sosIntent.putExtra("source", source);
        sosIntent.putExtra("anomaly_reason", anomalyReason);
        startActivity(sosIntent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelCountdown();
    }

    @Override
    public void onBackPressed() {
        // Prevent back button during check-in
        // User must respond via buttons
        Log.d(TAG, "Back button pressed — blocked during check-in");
    }
}
