package com.example.aapraksha;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class HistoryActivity extends AppCompatActivity {

    private ImageView btnBack;
    private ImageView btnFilter;
    private CardView tabAllAlerts;
    private CardView tabSos;
    private CardView tabCheckIns;
    private CardView btnViewDetails1;
    private CardView btnShare1;
    private CardView btnViewDetails2;
    private CardView btnShare2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        initializeViews();
        setupClickListeners();
        setupBottomNav();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btn_back);
        btnFilter = findViewById(R.id.btn_filter);
        tabAllAlerts = findViewById(R.id.tab_all_alerts);
        tabSos = findViewById(R.id.tab_sos);
        tabCheckIns = findViewById(R.id.tab_check_ins);
        btnViewDetails1 = findViewById(R.id.btn_view_details_1);
        btnShare1 = findViewById(R.id.btn_share_1);
        btnViewDetails2 = findViewById(R.id.btn_view_details_2);
        btnShare2 = findViewById(R.id.btn_share_2);
    }

    private void setupClickListeners() {
        // Back button
        btnBack.setOnClickListener(v -> finish());

        // Filter button
        btnFilter.setOnClickListener(v -> {
            Toast.makeText(this, "Filter options", Toast.LENGTH_SHORT).show();
        });

        // Filter tabs
        tabAllAlerts.setOnClickListener(v -> {
            Toast.makeText(this, "Showing all alerts", Toast.LENGTH_SHORT).show();
            // TODO: Filter logic
        });

        tabSos.setOnClickListener(v -> {
            Toast.makeText(this, "Showing SOS alerts only", Toast.LENGTH_SHORT).show();
            // TODO: Filter logic
        });

        tabCheckIns.setOnClickListener(v -> {
            Toast.makeText(this, "Showing check-ins only", Toast.LENGTH_SHORT).show();
            // TODO: Filter logic
        });

        // View Details buttons
        btnViewDetails1.setOnClickListener(v -> {
            Intent intent = new Intent(HistoryActivity.this, AlertDetailActivity.class);
            startActivity(intent);
        });

        btnViewDetails2.setOnClickListener(v -> {
            Intent intent = new Intent(HistoryActivity.this, AlertDetailActivity.class);
            startActivity(intent);
        });

        // Share buttons
        btnShare1.setOnClickListener(v -> {
            shareAlert("Emergency Alert - Mumbai location");
        });

        btnShare2.setOnClickListener(v -> {
            shareAlert("Silent Alert - New Delhi location");
        });
    }

    private void shareAlert(String message) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, message);
        startActivity(Intent.createChooser(shareIntent, "Share alert via"));
    }

    private void setupBottomNav() {
        LinearLayout navHome = findViewById(R.id.nav_home);
        LinearLayout navSafeZones = findViewById(R.id.nav_safe_zones);
        LinearLayout navHistory = findViewById(R.id.nav_history);
        LinearLayout navSettings = findViewById(R.id.nav_settings);

        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                Intent intent = new Intent(HistoryActivity.this, DashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            });
        }

        if (navSafeZones != null) {
            navSafeZones.setOnClickListener(v -> {
                Intent intent = new Intent(HistoryActivity.this, NetworkAlertsActivity.class);
                startActivity(intent);
            });
        }

        if (navHistory != null) {
            navHistory.setOnClickListener(v -> {
                // Already on history screen
                Toast.makeText(this, "History", Toast.LENGTH_SHORT).show();
            });
        }

        if (navSettings != null) {
            navSettings.setOnClickListener(v -> {
                Intent intent = new Intent(HistoryActivity.this, SettingsActivity.class);
                startActivity(intent);
            });
        }
    }
}
