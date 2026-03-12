package com.example.aapraksha;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class DashboardActivity extends AppCompatActivity {

    private boolean isSosActive = false;
    private android.animation.AnimatorSet continuousPulseAnimator;
    private View statusDot;
    private TextView tvSosStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            setContentView(R.layout.activity_dashboard);
            
            // Initialize views
            View sosButton = findViewById(R.id.btn_sos);
            View sosPulse1 = findViewById(R.id.sos_pulse_1);
            View sosPulse2 = findViewById(R.id.sos_pulse_2);
            View sosPulse3 = findViewById(R.id.sos_pulse_3);
            statusDot = findViewById(R.id.status_dot);
            tvSosStatus = findViewById(R.id.tv_sos_status);
            
            // Setup SOS button click with toggle
            if (sosButton != null) {
                sosButton.setOnClickListener(v -> {
                    toggleSOS(sosPulse1, sosPulse2, sosPulse3);
                });
            }
            
            // Setup feature cards
            View cardEmergency = findViewById(R.id.card_emergency_contacts);
            View cardGps = findViewById(R.id.card_gps);
            View cardVoice = findViewById(R.id.card_voice_trigger);
            
            if (cardEmergency != null) {
                cardEmergency.setOnClickListener(v -> {
                    Intent intent = new Intent(DashboardActivity.this, EmergencyContactsActivity.class);
                    startActivity(intent);
                });
            }
            
            if (cardGps != null) {
                cardGps.setOnClickListener(v -> 
                    Toast.makeText(this, "GPS Tracking", Toast.LENGTH_SHORT).show());
            }
            
            if (cardVoice != null) {
                cardVoice.setOnClickListener(v -> 
                    Toast.makeText(this, "Voice Trigger", Toast.LENGTH_SHORT).show());
            }
            
            // Setup bottom navigation
            setupBottomNav();
            
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading dashboard: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void toggleSOS(View pulse1, View pulse2, View pulse3) {
        isSosActive = !isSosActive;
        
        if (isSosActive) {
            // Activate SOS - Start continuous animation
            Toast.makeText(this, "🚨 SOS ACTIVATED! 🚨", Toast.LENGTH_LONG).show();
            updateStatusIndicator(true);
            startContinuousPulse(pulse1, pulse2, pulse3);
        } else {
            // Deactivate SOS - Stop animation
            Toast.makeText(this, "SOS Deactivated", Toast.LENGTH_SHORT).show();
            updateStatusIndicator(false);
            stopContinuousPulse(pulse1, pulse2, pulse3);
        }
    }
    
    private void updateStatusIndicator(boolean isActive) {
        if (isActive) {
            statusDot.setBackgroundResource(R.drawable.status_dot_active);
            tvSosStatus.setText("ACTIVE");
            tvSosStatus.setTextColor(getColor(R.color.sos_red));
        } else {
            statusDot.setBackgroundResource(R.drawable.status_dot_inactive);
            tvSosStatus.setText("INACTIVE");
            tvSosStatus.setTextColor(getColor(R.color.slate_grey));
        }
    }
    
    private void startContinuousPulse(View pulse1, View pulse2, View pulse3) {
        if (pulse1 != null && pulse2 != null && pulse3 != null) {
            pulse1.setVisibility(View.VISIBLE);
            pulse2.setVisibility(View.VISIBLE);
            pulse3.setVisibility(View.VISIBLE);
            
            continuousPulseAnimator = new android.animation.AnimatorSet();
            
            android.animation.AnimatorSet anim1 = createContinuousPulseAnimator(pulse1, 0);
            android.animation.AnimatorSet anim2 = createContinuousPulseAnimator(pulse2, 600);
            android.animation.AnimatorSet anim3 = createContinuousPulseAnimator(pulse3, 1200);
            
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
    
    private void stopContinuousPulse(View pulse1, View pulse2, View pulse3) {
        if (continuousPulseAnimator != null) {
            continuousPulseAnimator.cancel();
            continuousPulseAnimator = null;
        }
        
        if (pulse1 != null) {
            pulse1.setVisibility(View.INVISIBLE);
            pulse1.setScaleX(1f);
            pulse1.setScaleY(1f);
        }
        if (pulse2 != null) {
            pulse2.setVisibility(View.INVISIBLE);
            pulse2.setScaleX(1f);
            pulse2.setScaleY(1f);
        }
        if (pulse3 != null) {
            pulse3.setVisibility(View.INVISIBLE);
            pulse3.setScaleX(1f);
            pulse3.setScaleY(1f);
        }
    }
    
    private void setupBottomNav() {
        View navHome = findViewById(R.id.nav_home);
        View navSafeZones = findViewById(R.id.nav_safe_zones);
        View navHistory = findViewById(R.id.nav_history);
        View navSettings = findViewById(R.id.nav_settings);
        
        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                selectNavItem(navHome, navSafeZones, navHistory, navSettings);
                Toast.makeText(this, "Home", Toast.LENGTH_SHORT).show();
            });
        }
        if (navSafeZones != null) {
            navSafeZones.setOnClickListener(v -> {
                selectNavItem(navSafeZones, navHome, navHistory, navSettings);
                Toast.makeText(this, "Safe Zones", Toast.LENGTH_SHORT).show();
            });
        }
        if (navHistory != null) {
            navHistory.setOnClickListener(v -> {
                selectNavItem(navHistory, navHome, navSafeZones, navSettings);
                Toast.makeText(this, "History", Toast.LENGTH_SHORT).show();
            });
        }
        if (navSettings != null) {
            navSettings.setOnClickListener(v -> {
                selectNavItem(navSettings, navHome, navSafeZones, navHistory);
                Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show();
            });
        }
        
        // Select home by default
        if (navHome != null) {
            navHome.setSelected(true);
        }
    }
    
    private void selectNavItem(View selected, View... others) {
        selected.setAlpha(1f);
        for (View other : others) {
            if (other != null) {
                other.setAlpha(0.5f);
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (continuousPulseAnimator != null) {
            continuousPulseAnimator.cancel();
        }
    }
}
