package com.example.aapraksha.ai.routes;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.aapraksha.R;
import com.example.aapraksha.ai.danger.DangerZone;
import com.example.aapraksha.ai.danger.DangerZoneRepository;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.List;

public class SafeRouteActivity extends AppCompatActivity {

    private TextView tvStatus;
    private TextView tvRouteSummary;
    private TextView tvSafetyScore;
    private View btnBack;
    private View btnRecalculate;

    private FusedLocationProviderClient fusedLocationClient;
    private DangerZoneRepository dangerZoneRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_safe_route);

        tvStatus = findViewById(R.id.tvStatus);
        tvRouteSummary = findViewById(R.id.tvRouteSummary);
        tvSafetyScore = findViewById(R.id.tvSafetyScore);
        btnBack = findViewById(R.id.btnBack);
        btnRecalculate = findViewById(R.id.btnRecalculate);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        dangerZoneRepository = new DangerZoneRepository();

        btnBack.setOnClickListener(v -> finish());
        btnRecalculate.setOnClickListener(v -> loadSafeRouteEstimate());

        loadSafeRouteEstimate();
    }

    private void loadSafeRouteEstimate() {
        if (!hasLocationPermission()) {
            tvStatus.setText("Location permission required");
            tvRouteSummary.setText("Enable location to calculate safer route options.");
            return;
        }

        tvStatus.setText("Calculating route safety...");
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location == null) {
                        tvStatus.setText("Location unavailable");
                        tvRouteSummary.setText("Could not fetch current location. Please try again.");
                        return;
                    }
                    loadNearbyRiskAndScore(location.getLatitude(), location.getLongitude());
                })
                .addOnFailureListener(e -> {
                    tvStatus.setText("Route calculation failed");
                    tvRouteSummary.setText(e.getMessage() != null ? e.getMessage() : "Unknown location error");
                });
    }

    private void loadNearbyRiskAndScore(double latitude, double longitude) {
        dangerZoneRepository.getDangerZonesNearby(latitude, longitude, new DangerZoneRepository.OnDangerZonesLoadedListener() {
            @Override
            public void onLoaded(List<DangerZone> zones) {
                List<DangerZone> highRisk = new ArrayList<>();
                for (DangerZone zone : zones) {
                    if (zone != null && zone.getRank() >= DangerZone.RANK_HIGH_RISK) {
                        highRisk.add(zone);
                    }
                }

                int riskCount = highRisk.size();
                int safetyScore = Math.max(0, 100 - (riskCount * 15));
                String recommendation = safetyScore >= 75 ? "Recommended route is safe." :
                        safetyScore >= 50 ? "Use caution and stay on main roads." :
                                "High risk nearby. Prefer alternate transport.";

                tvStatus.setText("Safe Route Ready");
                tvRouteSummary.setText(
                        "Nearby high-risk zones: " + riskCount + "\n" +
                                recommendation + "\n\n" +
                                "Tip: avoid poorly lit roads and share live location."
                );
                tvSafetyScore.setText("Safety Score: " + safetyScore + "/100");
            }

            @Override
            public void onError(String error) {
                tvStatus.setText("Could not fetch danger zones");
                tvRouteSummary.setText(error != null ? error : "Unknown Firestore error");
                Toast.makeText(SafeRouteActivity.this, "Failed to load route risk data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
}

