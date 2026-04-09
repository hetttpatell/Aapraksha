package com.example.aapraksha;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.os.PowerManager;
import android.content.Context;
import android.provider.Settings;
import android.net.Uri;

import android.content.SharedPreferences;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.example.aapraksha.ai.fakecall.FakeCallActivity;
import com.example.aapraksha.ai.fakecall.FakeCallService;
import com.example.aapraksha.ai.gemini.ChatActivity;
import com.example.aapraksha.ai.routes.SafeRouteActivity;

public class DashboardActivity extends BaseActivity {
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
    // Pulse views for animation
    private View sosPulse1;
    private View sosPulse2;
    private View sosPulse3;
    private View cardAiAssistant;
    private View cardGps;
    private View cardFakeCall;

    private boolean hasRequestedRuntimePermissions = false;
    private boolean isRequestingPermissions = false;
    private SharedPreferences prefs;

    private final androidx.activity.result.ActivityResultLauncher<String[]> requestPermissionsLauncher =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
                isRequestingPermissions = false;
                
                boolean allGranted = true;
                for (Boolean isGranted : permissions.values()) {
                    if (!isGranted) allGranted = false;
                }
                
                if (!allGranted) {
                    Toast.makeText(this, "Some permissions were denied. Certain SOS features won't work.", Toast.LENGTH_LONG).show();
                }
                
                checkAndRequestPermissionsFlow();
            });
    
    // Tutorial views
    private View overlayTutorial;
    private View tutorialHighlightArea;
    private View cardEmergencyContacts;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        
        prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        
        statusDot = findViewById(R.id.status_dot);
        tvSosStatus = findViewById(R.id.tv_sos_status);
        tvUserName = findViewById(R.id.username_text);
        sosPulse1 = findViewById(R.id.sos_pulse_1);
        sosPulse2 = findViewById(R.id.sos_pulse_2);
        sosPulse3 = findViewById(R.id.sos_pulse_3);
        cardAiAssistant = findViewById(R.id.card_ai_assistant);
        cardGps = findViewById(R.id.card_gps);
        cardFakeCall = findViewById(R.id.card_fake_call);
        
        // Tutorial View Init
        overlayTutorial = findViewById(R.id.overlay_tutorial);
        tutorialHighlightArea = findViewById(R.id.tutorial_highlight_area);
        cardEmergencyContacts = findViewById(R.id.card_emergency_contacts);
        
        auth = FirebaseAuth.getInstance();
        userRepository = new UserRepository();
        
        setupBottomNav();
        setupQuickActions();
        startFakeCallProtectionServiceIfEnabled();
        loadUserProfile();
    }

    private void startFakeCallProtectionServiceIfEnabled() {
        boolean enabled = prefs.getBoolean("fake_call_shake_enabled", true);
        if (!enabled) return;

        Intent serviceIntent = new Intent(this, FakeCallService.class);
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        } catch (Exception e) {
            Log.e("DashboardActivity", "Failed to start FakeCallService", e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAndRequestPermissionsFlow();
    }

    private void checkAndRequestPermissionsFlow() {
        if (!hasAllRuntimePermissions() && !hasRequestedRuntimePermissions) {
            if (!isRequestingPermissions) {
                isRequestingPermissions = true;
                hasRequestedRuntimePermissions = true;
                requestPermissionsLauncher.launch(getRequiredPermissions());
            }
        } else if (!isIgnoringBatteryOptimizations() && !prefs.getBoolean("hasShownBatteryDialog", false)) {
            prefs.edit().putBoolean("hasShownBatteryDialog", true).apply();
            showBatteryOptimizationDialog();
        } else if (!isAccessibilityServiceEnabled() && !prefs.getBoolean("hasShownAccessibilityDialog", false)) {
            prefs.edit().putBoolean("hasShownAccessibilityDialog", true).apply();
            showAccessibilityDialog();
        } else {
            checkEmergencyContacts();
        }
    }

    private boolean hasAllRuntimePermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            for (String permission : getRequiredPermissions()) {
                if (androidx.core.content.ContextCompat.checkSelfPermission(this, permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    private String[] getRequiredPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            return new String[]{
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.SEND_SMS,
                    android.Manifest.permission.READ_CONTACTS,
                    android.Manifest.permission.CALL_PHONE,
                    android.Manifest.permission.CAMERA,
                    android.Manifest.permission.RECORD_AUDIO,
                    android.Manifest.permission.POST_NOTIFICATIONS
            };
        }

        return new String[]{
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.SEND_SMS,
                android.Manifest.permission.READ_CONTACTS,
                android.Manifest.permission.CALL_PHONE,
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.RECORD_AUDIO
        };
    }

    private boolean isIgnoringBatteryOptimizations() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            return pm != null && pm.isIgnoringBatteryOptimizations(getPackageName());
        }
        return true;
    }

    private void showBatteryOptimizationDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Allow Background SOS")
            .setMessage("To ensure the Volume Button SOS trigger works when the app is completely closed, you must disable battery optimizations for AapRaksha.")
            .setPositiveButton("Enable", (dialog, which) -> {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            })
            .setNegativeButton("Later", (dialog, which) -> {
                checkAndRequestPermissionsFlow(); // move to next
            })
            .setCancelable(false)
            .show();
    }
    
    private void showAccessibilityDialog() {
        new AlertDialog.Builder(this)
            .setTitle("CRITICAL: Enable Volume SOS")
            .setMessage("To trigger SOS using volume buttons EVEN when the app is closed, you MUST enable the AapRaksha SOS Accessibility Service in Settings.")
            .setPositiveButton("Enable Now", (dialog, which) -> {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(intent);
            })
            .setNegativeButton("Ignore", (dialog, which) -> {
                checkAndRequestPermissionsFlow(); // move to next
            })
            .setCancelable(false)
            .show();
    }
    
    private boolean isAccessibilityServiceEnabled() {
        int enabled = 0;
        try {
            enabled = Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) { }

        if (enabled == 1) {
            String services = Settings.Secure.getString(getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (services != null) {
                String expectedPrefix = getPackageName() + "/";
                
                android.text.TextUtils.SimpleStringSplitter splitter = new android.text.TextUtils.SimpleStringSplitter(':');
                splitter.setString(services);
                while (splitter.hasNext()) {
                    String service = splitter.next();
                    if (service.equalsIgnoreCase(expectedPrefix + getPackageName() + ".VolumeButtonService") ||
                        service.equalsIgnoreCase(expectedPrefix + ".VolumeButtonService")) {
                        return true;
                    }
                }
                
                // Fallback coarse check incase of weird formatting
                return services.contains(getPackageName() + "/.VolumeButtonService") || 
                       services.contains(getPackageName() + "/" + getPackageName() + ".VolumeButtonService");
            }
        }
        return false;
    }
    
    private void checkEmergencyContacts() {
        if (auth.getCurrentUser() == null) return;
        
        String userId = auth.getCurrentUser().getUid();
        userRepository.checkHasEmergencyContacts(userId, new UserRepository.OnContactCheckListener() {
            @Override
            public void onCheckComplete(boolean hasContacts) {
                if (!hasContacts) {
                    showAddContactsTutorial();
                } else {
                    if (overlayTutorial != null) {
                        overlayTutorial.setVisibility(View.GONE);
                    }
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                Log.e("DashboardActivity", "Error checking contacts: " + errorMessage);
            }
        });
    }

    private void showAddContactsTutorial() {
        if (overlayTutorial == null || cardEmergencyContacts == null) return;
        
        overlayTutorial.setVisibility(View.VISIBLE);
        
        // Accurate Synchronization: Align highlight area with the actual card dimensions and position
        cardEmergencyContacts.post(() -> {
            int[] cardLocation = new int[2];
            cardEmergencyContacts.getLocationOnScreen(cardLocation);
            
            int[] overlayLocation = new int[2];
            overlayTutorial.getLocationOnScreen(overlayLocation);
            
            // Match dimensions exactly
            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams params = (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) tutorialHighlightArea.getLayoutParams();
            params.width = cardEmergencyContacts.getWidth();
            params.height = cardEmergencyContacts.getHeight();
            
            // Set precise position using margins so that ConstraintLayout relative views move along with it
            params.leftMargin = cardLocation[0] - overlayLocation[0];
            params.topMargin = cardLocation[1] - overlayLocation[1];
            tutorialHighlightArea.setLayoutParams(params);
        });
        
        // Allow clicking the highlight area to go to contacts
        tutorialHighlightArea.setOnClickListener(v -> {
            overlayTutorial.setVisibility(View.GONE);
            Intent intent = new Intent(DashboardActivity.this, EmergencyContactsActivity.class);
            startActivity(intent);
        });
    }

    private void loadUserProfile() {
        if (auth.getCurrentUser() == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }
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
        android.animation.ObjectAnimator scaleX = android.animation.ObjectAnimator.ofFloat(view, "scaleX", 1f, 2.8f);
        android.animation.ObjectAnimator scaleY = android.animation.ObjectAnimator.ofFloat(view, "scaleY", 1f, 2.8f);
        android.animation.ObjectAnimator alpha = android.animation.ObjectAnimator.ofFloat(view, "alpha", 1f, 0f);
        
        scaleX.setDuration(2400);
        scaleY.setDuration(2400);
        alpha.setDuration(2400);
        
        scaleX.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        scaleY.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        alpha.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        
        scaleX.setStartDelay(delay);
        scaleY.setStartDelay(delay);
        alpha.setStartDelay(delay);

        android.animation.AnimatorSet set = new android.animation.AnimatorSet();
        set.playTogether(scaleX, scaleY, alpha);
        set.setInterpolator(new android.view.animation.DecelerateInterpolator());
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
        View btnSOS = findViewById(R.id.btn_sos);
        View cardEmergencyContacts = findViewById(R.id.card_emergency_contacts);
        
        // Setup SOS button
        if (btnSOS != null) {
            btnSOS.setOnClickListener(v -> {
                startContinuousPulse(sosPulse1, sosPulse2, sosPulse3);
                triggerSOS();
            });
        }
        
        // Setup emergency contacts button
        if (cardEmergencyContacts != null) {
            cardEmergencyContacts.setOnClickListener(v -> {
                Intent intent = new Intent(DashboardActivity.this, EmergencyContactsActivity.class);
                startActivity(intent);
            });
        }
        
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

    private void setupQuickActions() {
        if (cardGps != null) {
            cardGps.setOnClickListener(v -> {
                Intent intent = new Intent(DashboardActivity.this, SafeRouteActivity.class);
                startActivity(intent);
            });
        }
        if (cardFakeCall != null) {
            cardFakeCall.setOnClickListener(v -> {
                Intent intent = new Intent(DashboardActivity.this, FakeCallActivity.class);
                intent.putExtra(FakeCallActivity.EXTRA_CALLER_NAME, "Priya");
                startActivity(intent);
            });
        }
        if (cardAiAssistant != null) {
            cardAiAssistant.setOnClickListener(v -> {
                Intent intent = new Intent(DashboardActivity.this, ChatActivity.class);
                startActivity(intent);
            });
        }
    }
    
    private void triggerSOS() {
        Toast.makeText(this, "SOS Triggered", Toast.LENGTH_SHORT).show();
        // Add a short delay before launching SosTriggeredActivity
        new Handler().postDelayed(() -> {
            stopContinuousPulse(sosPulse1, sosPulse2, sosPulse3);
            Intent intent = new Intent(this, SosTriggeredActivity.class);
            startActivity(intent);
        }, 2000); // 2 seconds delay
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
