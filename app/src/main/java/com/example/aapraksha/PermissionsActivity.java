package com.example.aapraksha;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class PermissionsActivity extends AppCompatActivity {

    private Switch switchMic, switchCamera, switchLocation, switchCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permissions);

        // Back button
        ImageView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        // Initialize switches
        switchMic = findViewById(R.id.switch_mic);
        switchCamera = findViewById(R.id.switch_camera);
        switchLocation = findViewById(R.id.switch_location);
        switchCall = findViewById(R.id.switch_call);

        // Set default states (all enabled)
        switchMic.setChecked(true);
        switchCamera.setChecked(true);
        switchLocation.setChecked(true);
        switchCall.setChecked(true);

        // Mic permission toggle
        switchMic.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Toast.makeText(PermissionsActivity.this, "Microphone Permission Enabled", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(PermissionsActivity.this, "Microphone Permission Disabled", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Camera permission toggle
        switchCamera.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Toast.makeText(PermissionsActivity.this, "Camera Permission Enabled", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(PermissionsActivity.this, "Camera Permission Disabled", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Location permission toggle
        switchLocation.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Toast.makeText(PermissionsActivity.this, "Location Permission Enabled", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(PermissionsActivity.this, "Location Permission Disabled", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Call permission toggle
        switchCall.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Toast.makeText(PermissionsActivity.this, "Call Permission Enabled", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(PermissionsActivity.this, "Call Permission Disabled", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Setup bottom navigation
        setupBottomNav();
    }

    private void setupBottomNav() {
        View navHome = findViewById(R.id.nav_home);
        View navSafeZones = findViewById(R.id.nav_safe_zones);
        View navHistory = findViewById(R.id.nav_history);
        View navSettings = findViewById(R.id.nav_settings);
        
        // Highlight Settings (parent screen)
        highlightNavItem(navHome, false);
        highlightNavItem(navSafeZones, false);
        highlightNavItem(navHistory, false);
        highlightNavItem(navSettings, true);
        
        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                Intent intent = new Intent(PermissionsActivity.this, DashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            });
        }
        
        if (navSettings != null) {
            navSettings.setOnClickListener(v -> finish());
        }
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
}
