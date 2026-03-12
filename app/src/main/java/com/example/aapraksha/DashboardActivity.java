package com.example.aapraksha;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class DashboardActivity extends AppCompatActivity {

    private View sosButton;
    private View sosPulse1, sosPulse2, sosPulse3;
    private TextView tvSystemStatus;
    private CardView cardEmergencyContacts, cardGps, cardVoiceTrigger;
    private View navHome, navSafeZones, navHistory, navSettings;
    
    private AnimatorSet pulseAnimator;
    private boolean isSosActivated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        initViews();
        startPulseAnimation();
        setupClickListeners();
    }

    private void initViews() {
        sosButton = findViewById(R.id.btn_sos);
        sosPulse1 = findViewById(R.id.sos_pulse_1);
        sosPulse2 = findViewById(R.id.sos_pulse_2);
        sosPulse3 = findViewById(R.id.sos_pulse_3);
        tvSystemStatus = findViewById(R.id.tv_system_status);
        
        cardEmergencyContacts = findViewById(R.id.card_emergency_contacts);
        cardGps = findViewById(R.id.card_gps);
        cardVoiceTrigger = findViewById(R.id.card_voice_trigger);
        
        navHome = findViewById(R.id.nav_home);
        navSafeZones = findViewById(R.id.nav_safe_zones);
        navHistory = findViewById(R.id.nav_history);
        navSettings = findViewById(R.id.nav_settings);
    }

    private void startPulseAnimation() {
        pulseAnimator = new AnimatorSet();
        
        // Pulse 1 animation
        AnimatorSet pulse1Set = createPulseAnimator(sosPulse1, 0);
        
        // Pulse 2 animation
        AnimatorSet pulse2Set = createPulseAnimator(sosPulse2, 600);
        
        // Pulse 3 animation
        AnimatorSet pulse3Set = createPulseAnimator(sosPulse3, 1200);
        
        pulseAnimator.playTogether(pulse1Set, pulse2Set, pulse3Set);
        pulseAnimator.start();
    }

    private AnimatorSet createPulseAnimator(View view, long startDelay) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.8f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.8f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, "alpha", 0.6f, 0f);
        
        scaleX.setDuration(1800);
        scaleY.setDuration(1800);
        alpha.setDuration(1800);
        
        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);
        alpha.setRepeatCount(ValueAnimator.INFINITE);
        
        scaleX.setStartDelay(startDelay);
        scaleY.setStartDelay(startDelay);
        alpha.setStartDelay(startDelay);
        
        AnimatorSet set = new AnimatorSet();
        set.playTogether(scaleX, scaleY, alpha);
        set.setInterpolator(new AccelerateInterpolator());
        
        return set;
    }

    private void setupClickListeners() {
        sosButton.setOnClickListener(v -> activateSos());
        
        cardEmergencyContacts.setOnClickListener(v -> 
            Toast.makeText(this, "Emergency Contacts", Toast.LENGTH_SHORT).show());
        
        cardGps.setOnClickListener(v -> 
            Toast.makeText(this, "Real-time GPS", Toast.LENGTH_SHORT).show());
        
        cardVoiceTrigger.setOnClickListener(v -> 
            Toast.makeText(this, "Voice Trigger", Toast.LENGTH_SHORT).show());
        
        navHome.setOnClickListener(v -> selectNavItem(navHome));
        navSafeZones.setOnClickListener(v -> selectNavItem(navSafeZones));
        navHistory.setOnClickListener(v -> selectNavItem(navHistory));
        navSettings.setOnClickListener(v -> selectNavItem(navSettings));
        
        // Home is selected by default
        selectNavItem(navHome);
    }

    private void activateSos() {
        if (isSosActivated) return;
        
        isSosActivated = true;
        
        // Stop pulse animation
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
        }
        
        // Create expanding circles that fade out
        createExpandingCircle(sosPulse1, 0);
        createExpandingCircle(sosPulse2, 150);
        createExpandingCircle(sosPulse3, 300);
        
        // Scale and rotate SOS button
        AnimatorSet sosButtonAnim = new AnimatorSet();
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(sosButton, "scaleX", 1f, 0.9f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(sosButton, "scaleY", 1f, 0.9f, 1f);
        ObjectAnimator rotate = ObjectAnimator.ofFloat(sosButton, "rotation", 0f, 360f);
        
        scaleX.setDuration(400);
        scaleY.setDuration(400);
        rotate.setDuration(400);
        
        sosButtonAnim.playTogether(scaleX, scaleY, rotate);
        sosButtonAnim.start();
        
        Toast.makeText(this, "SOS ACTIVATED!", Toast.LENGTH_LONG).show();
        
        // Reset after 3 seconds
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            isSosActivated = false;
            startPulseAnimation();
        }, 3000);
    }

    private void createExpandingCircle(View view, long delay) {
        view.setScaleX(1f);
        view.setScaleY(1f);
        view.setAlpha(0.8f);
        
        AnimatorSet expandSet = new AnimatorSet();
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 3f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 3f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, "alpha", 0.8f, 0f);
        
        scaleX.setDuration(1000);
        scaleY.setDuration(1000);
        alpha.setDuration(1000);
        
        expandSet.playTogether(scaleX, scaleY, alpha);
        expandSet.setInterpolator(new DecelerateInterpolator());
        expandSet.setStartDelay(delay);
        expandSet.start();
    }

    private void selectNavItem(View selectedView) {
        // Reset all nav items
        resetNavItem(navHome);
        resetNavItem(navSafeZones);
        resetNavItem(navHistory);
        resetNavItem(navSettings);
        
        // Highlight selected item
        ImageView icon = selectedView.findViewById(R.id.nav_icon);
        TextView label = selectedView.findViewById(R.id.nav_label);
        
        icon.setAlpha(1f);
        label.setAlpha(1f);
        label.setTextColor(getColor(R.color.electric_indigo_light));
    }

    private void resetNavItem(View navItem) {
        ImageView icon = navItem.findViewById(R.id.nav_icon);
        TextView label = navItem.findViewById(R.id.nav_label);
        
        icon.setAlpha(0.6f);
        label.setAlpha(0.6f);
        label.setTextColor(getColor(R.color.slate_grey));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
        }
    }
}
