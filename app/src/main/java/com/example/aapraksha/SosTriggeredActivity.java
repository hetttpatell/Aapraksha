package com.example.aapraksha;

import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class SosTriggeredActivity extends AppCompatActivity {

    private View btnSos;
    private View sosPulse1;
    private View sosPulse2;
    private View sosPulse3;
    private TextView tvCountdown;
    private TextView tvCountdownText;
    private TextView tvLocation;
    private TextView tvAlertMessage;
    private TextView tvAudioCount;
    private TextView tvVideoCount;
    private TextView tvMessageCount;
    private CardView btnCancelSos;

    private android.animation.AnimatorSet continuousPulseAnimator;
    private Handler countdownHandler;
    private Runnable countdownRunnable;
    private int countdown = 5;
    private Ringtone sosAlertSound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sos_triggered);

        initializeViews();
        setupBottomNav();
        startContinuousPulse();
        startCountdown();
        setupCancelButton();
    }

    private void initializeViews() {
        btnSos = findViewById(R.id.btn_sos);
        sosPulse1 = findViewById(R.id.sos_pulse_1);
        sosPulse2 = findViewById(R.id.sos_pulse_2);
        sosPulse3 = findViewById(R.id.sos_pulse_3);
        tvCountdown = findViewById(R.id.tv_countdown);
        tvCountdownText = findViewById(R.id.tv_countdown_text);
        tvLocation = findViewById(R.id.tv_location);
        tvAlertMessage = findViewById(R.id.tv_alert_message);
        tvAudioCount = findViewById(R.id.tv_audio_count);
        tvVideoCount = findViewById(R.id.tv_video_count);
        tvMessageCount = findViewById(R.id.tv_message_count);
        btnCancelSos = findViewById(R.id.btn_cancel_sos);

        // Set initial values
        tvLocation.setText("Cyber House 2, Gurgaon");
        tvAlertMessage.setText("Need emergency assistance at my current location. High priority alert triggered via Aapraksha. Tracking active.");
        tvAudioCount.setText("3 Notified");
        tvVideoCount.setText("2 Notified");
        tvMessageCount.setText("5 Notified");
    }

    private void startContinuousPulse() {
        if (sosPulse1 != null && sosPulse2 != null && sosPulse3 != null) {
            sosPulse1.setVisibility(View.VISIBLE);
            sosPulse2.setVisibility(View.VISIBLE);
            sosPulse3.setVisibility(View.VISIBLE);

            continuousPulseAnimator = new android.animation.AnimatorSet();

            android.animation.AnimatorSet anim1 = createContinuousPulseAnimator(sosPulse1, 0);
            android.animation.AnimatorSet anim2 = createContinuousPulseAnimator(sosPulse2, 600);
            android.animation.AnimatorSet anim3 = createContinuousPulseAnimator(sosPulse3, 1200);

            continuousPulseAnimator.playTogether(anim1, anim2, anim3);
            continuousPulseAnimator.start();
        }
    }

    private android.animation.AnimatorSet createContinuousPulseAnimator(View view, long delay) {
        android.animation.ObjectAnimator scaleX = android.animation.ObjectAnimator.ofFloat(view, "scaleX", 1f, 2.4f);
        android.animation.ObjectAnimator scaleY = android.animation.ObjectAnimator.ofFloat(view, "scaleY", 1f, 2.4f);
        android.animation.ObjectAnimator alpha = android.animation.ObjectAnimator.ofFloat(view, "alpha", 0.8f, 0f);

        scaleX.setDuration(1800);
        scaleY.setDuration(1800);
        alpha.setDuration(1800);

        scaleX.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        scaleY.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        alpha.setRepeatCount(android.animation.ValueAnimator.INFINITE);

        scaleX.setRepeatMode(android.animation.ValueAnimator.RESTART);
        scaleY.setRepeatMode(android.animation.ValueAnimator.RESTART);
        alpha.setRepeatMode(android.animation.ValueAnimator.RESTART);

        scaleX.setStartDelay(delay);
        scaleY.setStartDelay(delay);
        alpha.setStartDelay(delay);

        android.animation.AnimatorSet set = new android.animation.AnimatorSet();
        set.playTogether(scaleX, scaleY, alpha);
        set.setInterpolator(new android.view.animation.DecelerateInterpolator(2.0f));

        return set;
    }

    private void startCountdown() {
        countdownHandler = new Handler();
        countdownRunnable = new Runnable() {
            @Override
            public void run() {
                if (countdown > 0) {
                    tvCountdown.setText(String.format("%02d", countdown));
                    tvCountdownText.setText("Triggering alert in " + countdown + " seconds...");
                    countdown--;
                    countdownHandler.postDelayed(this, 1000);
                } else {
                    // Alert sent - Hide countdown number and play sound
                    tvCountdown.setVisibility(View.GONE);
                    playSosSound();
                    tvCountdownText.setText("Alert sent to emergency contacts!");
                    Toast.makeText(SosTriggeredActivity.this, "🚨 Emergency Alert Sent!", Toast.LENGTH_LONG).show();
                }
            }
        };
        countdownHandler.post(countdownRunnable);
    }
    
    private void playSosSound() {
        try {
            // Simple approach - use notification manager to play default sound
            Uri notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            sosAlertSound = RingtoneManager.getRingtone(this, notificationUri);
            
            if (sosAlertSound != null) {
                sosAlertSound.play();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupCancelButton() {
        btnCancelSos.setOnClickListener(v -> {
            // Cancel SOS
            if (countdownHandler != null && countdownRunnable != null) {
                countdownHandler.removeCallbacks(countdownRunnable);
            }
            
            Toast.makeText(this, "SOS Alert Cancelled", Toast.LENGTH_SHORT).show();
            
            // Navigate to Safe Screen instead of dashboard
            Intent intent = new Intent(SosTriggeredActivity.this, SafeScreenActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void setupBottomNav() {
        View navHome = findViewById(R.id.nav_home);
        View navSafeZones = findViewById(R.id.nav_safe_zones);
        View navHistory = findViewById(R.id.nav_history);
        View navSettings = findViewById(R.id.nav_settings);

        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                Intent intent = new Intent(SosTriggeredActivity.this, DashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            });
        }
        
        if (navSafeZones != null) {
            navSafeZones.setOnClickListener(v -> {
                Intent intent = new Intent(SosTriggeredActivity.this, NetworkAlertsActivity.class);
                startActivity(intent);
            });
        }
        
        if (navHistory != null) {
            navHistory.setOnClickListener(v -> {
                Intent intent = new Intent(SosTriggeredActivity.this, HistoryActivity.class);
                startActivity(intent);
            });
        }
        
        if (navSettings != null) {
            navSettings.setOnClickListener(v -> {
                Intent intent = new Intent(SosTriggeredActivity.this, SettingsActivity.class);
                startActivity(intent);
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (continuousPulseAnimator != null) {
            continuousPulseAnimator.cancel();
        }
        if (countdownHandler != null && countdownRunnable != null) {
            countdownHandler.removeCallbacks(countdownRunnable);
        }
        if (sosAlertSound != null) {
            if (sosAlertSound.isPlaying()) {
                sosAlertSound.stop();
            }
            sosAlertSound = null;
        }
    }

    @Override
    public void onBackPressed() {
        // Disable back button during SOS
        Toast.makeText(this, "Please cancel SOS to go back", Toast.LENGTH_SHORT).show();
    }
}
