package com.example.aapraksha;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.example.aapraksha.models.User;

public class DashboardActivity extends AppCompatActivity {
    private FusedLocationProviderClient fusedLocationProviderClient;
    private Handler sosHandler;
    private Runnable sosNavigationRunnable;
    private boolean isSosActive = false;
    private String currentAlertId;
    private android.animation.AnimatorSet continuousPulseAnimator;
    private View statusDot;
    private TextView tvSosStatus;
    private TextView tvUserName;
    private FirebaseAuth auth;
    private UserRepository userRepository;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        
        statusDot = findViewById(R.id.status_dot);
        tvSosStatus = findViewById(R.id.tv_sos_status);
        tvUserName = findViewById(R.id.username_text);
        
        auth = FirebaseAuth.getInstance();
        userRepository = new UserRepository();
        
        setupBottomNav();
        loadUserProfile();
    }
    private void loadUserProfile() {
        String userId = auth.getCurrentUser().getUid();
        userRepository.getUserProfile(userId, new UserRepository.OnUserFetchListener() {
            @Override
            public void onSuccess(User user) {
                if (tvUserName != null && user != null && user.getFullName() != null) {
                    tvUserName.setText("Welcome, " + user.getFullName());
                } else if (tvUserName != null) {
                    tvUserName.setText("Welcome");
                }
            }

            @Override
            public void onError(String errorMessage) {
                Log.e("DashboardActivity", "Failed to load user profile: " + errorMessage);
                if (tvUserName != null) {
                    tvUserName.setText("Welcome");
                }
            }
        });
    }
    
    private void deactivateSOS(View pulse1, View pulse2, View pulse3) {
        updateStatusIndicator(false);
        stopContinuousPulse(pulse1, pulse2, pulse3);
        
        // Cancel navigation if SOS is deactivated before screen navigation
        if (sosHandler != null && sosNavigationRunnable != null) {
            sosHandler.removeCallbacks(sosNavigationRunnable);
        }
        
        // Stop location tracking service
        if (currentAlertId != null) {
            Intent serviceIntent = new Intent(this, LocationTrackingService.class);
            stopService(serviceIntent);
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
        scaleX.setDuration(1200);
        scaleY.setDuration(1200);
        alpha.setDuration(1200);
        scaleX.setStartDelay(delay);
        scaleY.setStartDelay(delay);
        alpha.setStartDelay(delay);

        android.animation.AnimatorSet set = new android.animation.AnimatorSet();
        set.playTogether(scaleX, scaleY, alpha);
        set.setInterpolator(new android.view.animation.LinearInterpolator());
        return set;
    }
    
    private void stopContinuousPulse(View pulse1, View pulse2, View pulse3) {
        if (continuousPulseAnimator != null) {
            continuousPulseAnimator.cancel();
            continuousPulseAnimator = null;
        }
        
        if (pulse1 != null) {
            pulse1.setVisibility(View.GONE);
            pulse1.setScaleX(1f);
            pulse1.setScaleY(1f);
        }
        if (pulse2 != null) {
            pulse2.setVisibility(View.GONE);
            pulse2.setScaleX(1f);
            pulse2.setScaleY(1f);
        }
        if (pulse3 != null) {
            pulse3.setVisibility(View.GONE);
            pulse3.setScaleX(1f);
            pulse3.setScaleY(1f);
        }
    }
    
    private void setupBottomNav() {
        View navHome = findViewById(R.id.nav_home);
        View navSafeZones = findViewById(R.id.nav_safe_zones);
        View navHistory = findViewById(R.id.nav_history);
        View navSettings = findViewById(R.id.nav_settings);
        
        // Highlight current screen (Home)
        highlightNavItem(navHome, true);
        highlightNavItem(navSafeZones, false);
        highlightNavItem(navHistory, false);
        highlightNavItem(navSettings, false);
        
        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                Toast.makeText(this, "Home", Toast.LENGTH_SHORT).show();
            });
        }
        if (navSafeZones != null) {
            navSafeZones.setOnClickListener(v -> {
                Intent intent = new Intent(DashboardActivity.this, NetworkAlertsActivity.class);
                startActivity(intent);
            });
        }
        if (navHistory != null) {
            navHistory.setOnClickListener(v -> {
                Intent intent = new Intent(DashboardActivity.this, HistoryActivity.class);
                startActivity(intent);
            });
        }
        if (navSettings != null) {
            navSettings.setOnClickListener(v -> {
                Intent intent = new Intent(DashboardActivity.this, SettingsActivity.class);
                startActivity(intent);
            });
        }
    }
    
    private void highlightNavItem(View navItem, boolean isActive) {
        if (navItem == null) return;
        
        try {
            ImageView icon = (ImageView) ((android.view.ViewGroup) navItem).getChildAt(0);
            TextView label = (TextView) ((android.view.ViewGroup) navItem).getChildAt(1);
            
            if (isActive) {
                navItem.setAlpha(1f);
                if (icon != null) icon.setColorFilter(getColor(R.color.electric_indigo_light));
                if (label != null) {
                    label.setTextColor(getColor(R.color.electric_indigo_light));
                    label.setTypeface(null, android.graphics.Typeface.BOLD);
                }
            } else {
                navItem.setAlpha(0.5f);
                if (icon != null) icon.setColorFilter(getColor(R.color.slate_grey));
                if (label != null) {
                    label.setTextColor(getColor(R.color.slate_grey));
                    label.setTypeface(null, android.graphics.Typeface.NORMAL);
                }
            }
        } catch (Exception e) {
            Log.e("DashboardActivity", "Error highlighting nav item: " + e.getMessage());
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (continuousPulseAnimator != null) {
            continuousPulseAnimator.cancel();
        }
        if (sosHandler != null && sosNavigationRunnable != null) {
            sosHandler.removeCallbacks(sosNavigationRunnable);
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Reset SOS state when leaving dashboard
        isSosActive = false;
    }
}
