package com.example.aapraksha;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.text.TextUtils;

import android.accessibilityservice.AccessibilityService;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.os.PowerManager;
import android.content.Context;

public class SettingsActivity extends BaseActivity {

    private static final String TAG = "SettingsActivity";

    // Profile views
    private TextView tvUserName, tvUserEmail, tvUserPhone, tvMemberSince, tvAccountStatus;
    private ImageView profileImage;

    // PIN views
    private TextView tvEmergencyPin;
    private ImageView btnTogglePinVisibility;
    private boolean isPinVisible = false;
    private String actualPin = "";

    // Safety
    private Switch switchVolumeSos;
    private TextView tvVolumeSosStatus;
    private View rowVolumeSos;

    // Permission switches
    private Switch switchPermLocation, switchPermSms, switchPermCamera,
            switchPermMic, switchPermContacts, switchPermCall;
    private TextView tvPermLocationStatus, tvPermSmsStatus, tvPermCameraStatus,
            tvPermMicStatus, tvPermContactsStatus, tvPermCallStatus;

    // Permission rows for tap-to-grant
    private View rowPermLocation, rowPermSms, rowPermCamera,
            rowPermMic, rowPermContacts, rowPermCall;

    // Firebase
    private FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Firebase
        auth = FirebaseAuth.getInstance();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // Bind profile views
        tvUserName       = findViewById(R.id.tv_user_name);
        tvUserEmail      = findViewById(R.id.tv_user_email);
        tvUserPhone      = findViewById(R.id.tv_user_phone);
        tvMemberSince    = findViewById(R.id.tv_member_since);
        tvAccountStatus  = findViewById(R.id.tv_account_status);
        profileImage     = findViewById(R.id.profile_image);

        // Bind PIN views
        tvEmergencyPin         = findViewById(R.id.tv_emergency_pin);
        btnTogglePinVisibility = findViewById(R.id.btn_toggle_pin_visibility);

        // Bind safety switch
        switchVolumeSos = findViewById(R.id.switch_volume_sos);

        // Bind permission switches
        switchPermLocation = findViewById(R.id.switch_perm_location);
        switchPermSms      = findViewById(R.id.switch_perm_sms);
        switchPermCamera   = findViewById(R.id.switch_perm_camera);
        switchPermMic      = findViewById(R.id.switch_perm_mic);
        switchPermContacts = findViewById(R.id.switch_perm_contacts);
        switchPermCall     = findViewById(R.id.switch_perm_call);

        // Bind permission status labels
        tvPermLocationStatus = findViewById(R.id.tv_perm_location_status);
        tvPermSmsStatus      = findViewById(R.id.tv_perm_sms_status);
        tvPermCameraStatus   = findViewById(R.id.tv_perm_camera_status);
        tvPermMicStatus      = findViewById(R.id.tv_perm_mic_status);
        tvPermContactsStatus = findViewById(R.id.tv_perm_contacts_status);
        tvPermCallStatus     = findViewById(R.id.tv_perm_call_status);

        // Bind permission rows
        rowPermLocation = findViewById(R.id.row_perm_location);
        rowPermSms      = findViewById(R.id.row_perm_sms);
        rowPermCamera   = findViewById(R.id.row_perm_camera);
        rowPermMic      = findViewById(R.id.row_perm_microphone);
        rowPermContacts = findViewById(R.id.row_perm_contacts);
        rowPermCall     = findViewById(R.id.row_perm_call);

        // Bind Volume SOS status
        tvVolumeSosStatus = findViewById(R.id.tv_volume_sos_status);
        rowVolumeSos      = findViewById(R.id.row_volume_sos);

        // Back button
        ImageView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        // Edit Profile
        findViewById(R.id.btn_edit_profile).setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, EditProfileActivity.class);
            startActivity(intent);
        });

        // PIN toggle visibility (read-only display)
        btnTogglePinVisibility.setOnClickListener(v -> {
            if (isPinVisible) {
                tvEmergencyPin.setText(getMaskedPin(actualPin));
                btnTogglePinVisibility.setImageResource(R.drawable.ic_eye_off);
            } else {
                tvEmergencyPin.setText(actualPin.isEmpty() ? "••••" : actualPin);
                btnTogglePinVisibility.setImageResource(R.drawable.ic_eye_on);
            }
            isPinVisible = !isPinVisible;
        });

        // Volume SOS Row - Tapping navigates to Accessibility Settings
        rowVolumeSos.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                Toast.makeText(this, "Find 'AapRaksha' in the list and turn it ON", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Log.e(TAG, "Could not open accessibility settings", e);
                Toast.makeText(this, "Please open Accessibility Settings manually", Toast.LENGTH_SHORT).show();
            }
        });

        // Volume SOS Toggle (Legacy sync - keep Switch as display-only)
        switchVolumeSos.setClickable(false);
        switchVolumeSos.setFocusable(false);

        // Emergency Contacts
        findViewById(R.id.card_emergency).setOnClickListener(v ->
                startActivity(new Intent(SettingsActivity.this, EmergencyContactsActivity.class)));

        // Logout
        findViewById(R.id.btn_logout).setOnClickListener(v -> showLogoutDialog());

        // Bottom nav
        setupBottomNav();

        // Load all live data
        loadUserProfile();
        loadVolumeSosSetting();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh permission states every time user returns (they may have just granted one)
        refreshPermissions();
        refreshVolumeSosStatus();
    }

    // ─────────────────────────────────────────────────────────────
    //  VOLUME SOS STATUS (ACCESSIBILITY SERVICE)
    // ─────────────────────────────────────────────────────────────
    private void refreshVolumeSosStatus() {
        boolean isEnabled = isAccessibilityServiceEnabled(this, VolumeButtonService.class);
        switchVolumeSos.setChecked(isEnabled);

        if (isEnabled) {
            tvVolumeSosStatus.setText("Active — ready to detect SOS signal");
            tvVolumeSosStatus.setTextColor(0xFF4CAF50); // green
        } else {
            tvVolumeSosStatus.setText("Disabled — tap to configure in Phone Settings");
            tvVolumeSosStatus.setTextColor(0xFFFF7043); // orange-red
        }
        
        // Also check battery optimizations if enabled
        if (isEnabled) {
           checkBatteryOptimizationsSilent();
        }
    }

    private boolean isAccessibilityServiceEnabled(Context context, Class<? extends AccessibilityService> service) {
        String serviceId = context.getPackageName() + "/" + service.getName();
        String enabledServices = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);

        if (TextUtils.isEmpty(enabledServices)) return false;

        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
        splitter.setString(enabledServices);
        while (splitter.hasNext()) {
            if (splitter.next().equalsIgnoreCase(serviceId)) return true;
        }
        return false;
    }

    private void checkBatteryOptimizationsSilent() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                tvVolumeSosStatus.setText(tvVolumeSosStatus.getText() + "\n(Warning: Battery optimization may affect reliability)");
                tvVolumeSosStatus.setTextColor(0xFFFFC107); // amber
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  LOAD LIVE USER PROFILE FROM FIRESTORE
    // ─────────────────────────────────────────────────────────────
    private void loadUserProfile() {
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();

        FirebaseFirestore.getInstance().collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    // Full name
                    String name = doc.getString("fullName");
                    if (name == null || name.isEmpty()) name = doc.getString("name");
                    tvUserName.setText(name != null ? name : "—");

                    // Email – prefer Firestore, fall back to FirebaseAuth
                    String email = doc.getString("email");
                    if (email == null || email.isEmpty()) {
                        email = auth.getCurrentUser().getEmail();
                    }
                    tvUserEmail.setText(email != null ? email : "—");

                    // Phone
                    String phone = doc.getString("phoneNumber");
                    if (phone == null) phone = doc.getString("phone");
                    tvUserPhone.setText(phone != null ? phone : "—");

                    // Emergency PIN (read-only display)
                    String pin = doc.getString("emergencyPin");
                    if (pin != null && !pin.isEmpty()) {
                        actualPin = pin;
                        tvEmergencyPin.setText(getMaskedPin(pin));
                    } else {
                        tvEmergencyPin.setText("Not set");
                    }

                    // Account status
                    String status = doc.getString("accountStatus");
                    if ("ACTIVE".equalsIgnoreCase(status)) {
                        tvAccountStatus.setText("● ACTIVE");
                        tvAccountStatus.setTextColor(getColor(android.R.color.holo_green_light));
                    } else if (status != null) {
                        tvAccountStatus.setText("● " + status);
                        tvAccountStatus.setTextColor(getColor(android.R.color.holo_orange_light));
                    }

                    // Member since (createdAt timestamp)
                    com.google.firebase.Timestamp createdAt = doc.getTimestamp("createdAt");
                    if (createdAt == null) createdAt = doc.getTimestamp("memberSince");
                    if (createdAt != null) {
                        Date date = createdAt.toDate();
                        String formatted = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date);
                        tvMemberSince.setText(formatted);
                    } else {
                        tvMemberSince.setText("—");
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to load user profile", e));
    }

    // ─────────────────────────────────────────────────────────────
    //  VOLUME SOS SETTING
    // ─────────────────────────────────────────────────────────────
    private void loadVolumeSosSetting() {
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();

        FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .collection("settings").document("settings_1")
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Boolean enabled = doc.getBoolean("volumeButtonSosEnabled");
                        if (enabled != null) {
                            switchVolumeSos.setChecked(enabled);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to load settings", e));
    }   

    private void saveVolumeSosSetting(boolean enabled) {
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();

        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("volumeButtonSosEnabled", enabled);

        FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .collection("settings").document("settings_1")
                .set(data, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(v ->
                        Toast.makeText(this,
                                enabled ? "Volume SOS Enabled" : "Volume SOS Disabled",
                                Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to save setting", Toast.LENGTH_SHORT).show());
    }

    private void checkBatteryOptimizations() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                new AlertDialog.Builder(this)
                    .setTitle("Allow Background SOS")
                    .setMessage("To ensure the Volume Button SOS works 24/7 in the background, please disable battery optimizations for AapRaksha.")
                    .setPositiveButton("Enable", (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    })
                    .setNegativeButton("Later", null)
                    .show();
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  PERMISSIONS — live check & tap-to-grant
    // ─────────────────────────────────────────────────────────────
    private void refreshPermissions() {
        bindPermission(
                Manifest.permission.ACCESS_FINE_LOCATION,
                switchPermLocation, tvPermLocationStatus, rowPermLocation,
                "Granted — location tracking active",
                "Not granted — tap to enable"
        );
        bindPermission(
                Manifest.permission.SEND_SMS,
                switchPermSms, tvPermSmsStatus, rowPermSms,
                "Granted — SMS alerts active",
                "Not granted — tap to enable"
        );
        bindPermission(
                Manifest.permission.CAMERA,
                switchPermCamera, tvPermCameraStatus, rowPermCamera,
                "Granted — camera evidence active",
                "Not granted — tap to enable"
        );
        bindPermission(
                Manifest.permission.RECORD_AUDIO,
                switchPermMic, tvPermMicStatus, rowPermMic,
                "Granted — microphone active",
                "Not granted — tap to enable"
        );
        bindPermission(
                Manifest.permission.READ_CONTACTS,
                switchPermContacts, tvPermContactsStatus, rowPermContacts,
                "Granted — contacts access active",
                "Not granted — tap to enable"
        );
        bindPermission(
                Manifest.permission.CALL_PHONE,
                switchPermCall, tvPermCallStatus, rowPermCall,
                "Granted — auto-call active",
                "Not granted — tap to enable"
        );
    }

    private void bindPermission(String permission, Switch sw, TextView statusView,
                                View row, String grantedText, String deniedText) {
        boolean granted = ContextCompat.checkSelfPermission(this, permission)
                == PackageManager.PERMISSION_GRANTED;

        // Switch is always non-interactive (display-only)
        sw.setChecked(granted);
        sw.setEnabled(false);

        if (granted) {
            statusView.setText(grantedText);
            statusView.setTextColor(0xFF4CAF50); // green
            row.setClickable(false);
            row.setFocusable(false);
        } else {
            statusView.setText(deniedText);
            statusView.setTextColor(0xFFFF7043); // orange-red
            row.setClickable(true);
            row.setFocusable(true);
            row.setOnClickListener(v -> openAppSettings());
        }
    }

    /** Opens the device app-settings page so user can grant denied permissions */
    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", getPackageName(), null));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        Toast.makeText(this, "Grant the permission then return to AapRaksha", Toast.LENGTH_LONG).show();
    }

    // ─────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────

    /** Returns "••••" for any 4-char PIN */
    private String getMaskedPin(String pin) {
        if (pin == null || pin.isEmpty()) return "••••";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pin.length(); i++) sb.append("•");
        return sb.toString();
    }

    // ─────────────────────────────────────────────────────────────
    //  BOTTOM NAV
    // ─────────────────────────────────────────────────────────────
    private void setupBottomNav() {
        View navHome     = findViewById(R.id.nav_home);
        View navSafe     = findViewById(R.id.nav_safe_zones);
        View navHistory  = findViewById(R.id.nav_history);
        View navSettings = findViewById(R.id.nav_settings);

        highlightNavItem(navHome, false);
        highlightNavItem(navSafe, false);
        highlightNavItem(navHistory, false);
        highlightNavItem(navSettings, true);

        if (navHome != null) navHome.setOnClickListener(v -> {
            Intent i = new Intent(this, DashboardActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
            finish();
        });
        if (navSafe != null) navSafe.setOnClickListener(v ->
                startActivity(new Intent(this, AlertsActivity.class)));
        if (navHistory != null) navHistory.setOnClickListener(v ->
                startActivity(new Intent(this, HistoryActivity.class)));
    }

    private void highlightNavItem(View navItem, boolean isActive) {
        if (navItem == null) return;
        ImageView icon  = (ImageView) ((android.view.ViewGroup) navItem).getChildAt(0);
        TextView  label = (TextView)  ((android.view.ViewGroup) navItem).getChildAt(1);

        if (isActive) {
            navItem.setAlpha(1f);
            if (icon  != null) icon.setColorFilter(getColor(R.color.electric_indigo_light));
            if (label != null) {
                label.setTextColor(getColor(R.color.electric_indigo_light));
                label.setTypeface(null, android.graphics.Typeface.BOLD);
            }
        } else {
            navItem.setAlpha(0.5f);
            if (icon  != null) icon.setColorFilter(getColor(R.color.slate_grey));
            if (label != null) {
                label.setTextColor(getColor(R.color.slate_grey));
                label.setTypeface(null, android.graphics.Typeface.NORMAL);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  LOGOUT
    // ─────────────────────────────────────────────────────────────
    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout from AapRaksha?")
                .setPositiveButton("Logout", (dialog, which) -> performLogout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performLogout() {
        Log.d(TAG, "Logging out user");
        auth.signOut();
        googleSignInClient.signOut().addOnCompleteListener(task -> {
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, SplashActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
