package com.example.aapraksha;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private Switch switchVolumeSos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Back button
        ImageView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        // Volume SOS toggle
        switchVolumeSos = findViewById(R.id.switch_volume_sos);
        switchVolumeSos.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Toast.makeText(SettingsActivity.this, "Volume Button SOS Enabled", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(SettingsActivity.this, "Volume Button SOS Disabled", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Emergency Contacts
        findViewById(R.id.card_emergency).setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, EmergencyContactsActivity.class);
            startActivity(intent);
        });

        // Privacy & Permissions
        findViewById(R.id.card_privacy).setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, PermissionsActivity.class);
            startActivity(intent);
        });

        // Logout button
        findViewById(R.id.btn_logout).setOnClickListener(v -> showLogoutDialog());

        // Setup bottom navigation
        setupBottomNav();
    }

    private void setupBottomNav() {
        View navHome = findViewById(R.id.nav_home);
        View navSafeZones = findViewById(R.id.nav_safe_zones);
        View navHistory = findViewById(R.id.nav_history);
        View navSettings = findViewById(R.id.nav_settings);
        
        // Highlight current screen (Settings)
        highlightNavItem(navHome, false);
        highlightNavItem(navSafeZones, false);
        highlightNavItem(navHistory, false);
        highlightNavItem(navSettings, true);
        
        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                Intent intent = new Intent(SettingsActivity.this, DashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            });
        }
        
        if (navSafeZones != null) {
            navSafeZones.setOnClickListener(v -> {
                Toast.makeText(this, "Safe Zones", Toast.LENGTH_SHORT).show();
            });
        }
        
        if (navHistory != null) {
            navHistory.setOnClickListener(v -> {
                Toast.makeText(this, "History", Toast.LENGTH_SHORT).show();
            });
        }
        
        // Settings icon is already highlighted (current screen)
    }
    
    private void highlightNavItem(View navItem, boolean isActive) {
        if (navItem == null) return;
        
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
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout from AapRaksha?")
            .setPositiveButton("Logout", (dialog, which) -> {
                Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
}
