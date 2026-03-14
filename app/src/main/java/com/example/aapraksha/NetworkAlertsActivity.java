package com.example.aapraksha;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class NetworkAlertsActivity extends AppCompatActivity {

    private ImageView btnBack;
    private ImageView btnNotifications;
    private CardView tabActive;
    private CardView tabInactive;
    
    // Alert cards
    private CardView alertCard1;
    private CardView alertCard2;
    private CardView alertCard3;
    private CardView alertCard4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network_alerts);

        initializeViews();
        setupClickListeners();
        setupBottomNav();
        
        // Show active alerts by default
        filterAlerts(true);
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btn_back);
        btnNotifications = findViewById(R.id.btn_notifications);
        tabActive = findViewById(R.id.tab_active);
        tabInactive = findViewById(R.id.tab_inactive);
        
        // Alert cards
        alertCard1 = findViewById(R.id.alert_card_1);
        alertCard2 = findViewById(R.id.alert_card_2);
        alertCard3 = findViewById(R.id.alert_card_3);
        alertCard4 = findViewById(R.id.alert_card_4);
    }

    private void setupClickListeners() {
        // Back button
        btnBack.setOnClickListener(v -> finish());

        // Notifications button
        btnNotifications.setOnClickListener(v -> {
            Toast.makeText(this, "Notifications", Toast.LENGTH_SHORT).show();
        });

        // Filter tabs
        tabActive.setOnClickListener(v -> filterAlerts(true));
        tabInactive.setOnClickListener(v -> filterAlerts(false));

        // Alert action buttons
        setupAlertButtons();
    }
    
    private void filterAlerts(boolean showActive) {
        if (showActive) {
            // Show active alerts (1, 2, 3) and hide inactive (4)
            alertCard1.setVisibility(View.VISIBLE);
            alertCard2.setVisibility(View.VISIBLE);
            alertCard3.setVisibility(View.VISIBLE);
            alertCard4.setVisibility(View.GONE);
            
            // Update tab styling
            tabActive.setCardBackgroundColor(getResources().getColor(R.color.electric_indigo));
            tabInactive.setCardBackgroundColor(getResources().getColor(R.color.input_background_dark));
        } else {
            // Show inactive alerts (4) and hide active (1, 2, 3)
            alertCard1.setVisibility(View.GONE);
            alertCard2.setVisibility(View.GONE);
            alertCard3.setVisibility(View.GONE);
            alertCard4.setVisibility(View.VISIBLE);
            
            // Update tab styling
            tabActive.setCardBackgroundColor(getResources().getColor(R.color.input_background_dark));
            tabInactive.setCardBackgroundColor(getResources().getColor(R.color.electric_indigo));
        }
    }

    private void setupAlertButtons() {
        // Alert 1 - Active
        CardView btnRespond1 = findViewById(R.id.btn_respond_1);
        CardView btnVoice1 = findViewById(R.id.btn_voice_1);

        if (btnRespond1 != null) {
            btnRespond1.setOnClickListener(v -> {
                Toast.makeText(this, "Responding to emergency...", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnVoice1 != null) {
            btnVoice1.setOnClickListener(v -> {
                Toast.makeText(this, "Playing voice recording...", Toast.LENGTH_SHORT).show();
            });
        }

        // Alert 2 - Active
        CardView btnRespond2 = findViewById(R.id.btn_respond_2);
        CardView btnVoice2 = findViewById(R.id.btn_voice_2);

        if (btnRespond2 != null) {
            btnRespond2.setOnClickListener(v -> {
                Toast.makeText(this, "Responding to emergency...", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnVoice2 != null) {
            btnVoice2.setOnClickListener(v -> {
                Toast.makeText(this, "Playing voice recording...", Toast.LENGTH_SHORT).show();
            });
        }

        // Alert 3 - Active
        CardView btnRespond3 = findViewById(R.id.btn_respond_3);
        CardView btnVoice3 = findViewById(R.id.btn_voice_3);

        if (btnRespond3 != null) {
            btnRespond3.setOnClickListener(v -> {
                Toast.makeText(this, "Responding to emergency...", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnVoice3 != null) {
            btnVoice3.setOnClickListener(v -> {
                Toast.makeText(this, "Playing voice recording...", Toast.LENGTH_SHORT).show();
            });
        }

        // Alert 4 - Inactive
        CardView btnViewLocation4 = findViewById(R.id.btn_view_location_4);
        
        if (btnViewLocation4 != null) {
            btnViewLocation4.setOnClickListener(v -> {
                Toast.makeText(this, "Opening location on map...", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void setupBottomNav() {
        LinearLayout navHome = findViewById(R.id.nav_home);
        LinearLayout navSafeZones = findViewById(R.id.nav_safe_zones);
        LinearLayout navHistory = findViewById(R.id.nav_history);
        LinearLayout navSettings = findViewById(R.id.nav_settings);

        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                Intent intent = new Intent(NetworkAlertsActivity.this, DashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            });
        }

        if (navSafeZones != null) {
            navSafeZones.setOnClickListener(v -> {
                // Already on alerts screen
                Toast.makeText(this, "Alerts", Toast.LENGTH_SHORT).show();
            });
        }

        if (navHistory != null) {
            navHistory.setOnClickListener(v -> {
                Intent intent = new Intent(NetworkAlertsActivity.this, HistoryActivity.class);
                startActivity(intent);
            });
        }

        if (navSettings != null) {
            navSettings.setOnClickListener(v -> {
                Toast.makeText(this, "Settings coming soon", Toast.LENGTH_SHORT).show();
            });
        }
    }
}
