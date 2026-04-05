package com.example.aapraksha;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

/**
 * NetworkAlertsActivity - Displays live SOS alerts from other users in the network
 * Real-time updates via Firestore listeners
 * Filters: Active / Resolved
 */
public class NetworkAlertsActivity extends BaseActivity implements AlertService.AlertServiceCallback {
    private static final String TAG = "NetworkAlertsActivity";

    private ImageView btnBack;
    private ImageView btnNotifications;
    private CardView tabActive;
    private CardView tabInactive;
    private TextView tabActiveText;
    private TextView tabInactiveText;

    private RecyclerView alertsRecycler;
    private NetworkAlertListAdapter alertAdapter;
    private ProgressBar loadingProgress;
    private LinearLayout emptyState;
    private TextView emptyStateMessage;
    private LinearLayout errorState;
    private TextView errorMessage;
    private CardView retryButton;

    // Bottom Nav
    private LinearLayout navHome;
    private LinearLayout navSafeZones;
    private LinearLayout navHistory;
    private LinearLayout navSettings;

    private AlertService alertService;
    private SOSAlertRepository sosAlertRepository;
    private String currentFilter = "ACTIVE"; // ACTIVE or RESOLVED
    
    // Store all alerts for filtering
    private List<SOSAlert> allAlerts = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network_alerts);

        initializeViews();
        setupRepositories();
        setupRecyclerView();
        setupClickListeners();
        setupBottomNav();
        loadLiveAlerts();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btn_back);
        btnNotifications = findViewById(R.id.btn_notifications);
        tabActive = findViewById(R.id.tab_active);
        tabInactive = findViewById(R.id.tab_inactive);
        tabActiveText = findViewById(R.id.tab_active_text);
        tabInactiveText = findViewById(R.id.tab_inactive_text);

        alertsRecycler = findViewById(R.id.alerts_recycler);
        loadingProgress = findViewById(R.id.loading_progress);
        emptyState = findViewById(R.id.empty_state);
        emptyStateMessage = findViewById(R.id.empty_state_message);
        errorState = findViewById(R.id.error_state);
        errorMessage = findViewById(R.id.error_message);
        retryButton = findViewById(R.id.retry_button);

        navHome = findViewById(R.id.nav_home);
        navSafeZones = findViewById(R.id.nav_safe_zones);
        navHistory = findViewById(R.id.nav_history);
        navSettings = findViewById(R.id.nav_settings);
    }

    private void setupRepositories() {
        sosAlertRepository = new SOSAlertRepository();
        alertService = new AlertService(sosAlertRepository);
        alertService.setCallback(this);
    }

    private void setupRecyclerView() {
        alertsRecycler.setLayoutManager(new LinearLayoutManager(this));
        alertAdapter = new NetworkAlertListAdapter(this, new ArrayList<>(), new NetworkAlertListAdapter.OnNetworkAlertClickListener() {
            @Override
            public void onAlertClicked(SOSAlert alert) {
                navigateToResponseOptions(alert);
            }
        });
        alertsRecycler.setAdapter(alertAdapter);
    }

    private void setupClickListeners() {
        // Back button
        btnBack.setOnClickListener(v -> finish());

        // Notifications button
        btnNotifications.setOnClickListener(v -> {
            Toast.makeText(this, "Notifications", Toast.LENGTH_SHORT).show();
        });

        // Filter tabs
        tabActive.setOnClickListener(v -> {
            currentFilter = "ACTIVE";
            updateFilterTabStyles();
            filterAndDisplayAlerts();
        });

        tabInactive.setOnClickListener(v -> {
            currentFilter = "RESOLVED";
            updateFilterTabStyles();
            filterAndDisplayAlerts();
        });

        // Retry button
        retryButton.setOnClickListener(v -> {
            hideErrorState();
            loadLiveAlerts();
        });
    }

    private void updateFilterTabStyles() {
        if ("ACTIVE".equals(currentFilter)) {
            tabActive.setCardBackgroundColor(getResources().getColor(R.color.electric_indigo));
            tabActiveText.setTextColor(getResources().getColor(R.color.text_on_primary));
            tabActiveText.setTypeface(null, android.graphics.Typeface.BOLD);
            
            tabInactive.setCardBackgroundColor(getResources().getColor(R.color.input_background_dark));
            tabInactiveText.setTextColor(getResources().getColor(R.color.slate_grey));
            tabInactiveText.setTypeface(null, android.graphics.Typeface.NORMAL);
        } else {
            tabInactive.setCardBackgroundColor(getResources().getColor(R.color.electric_indigo));
            tabInactiveText.setTextColor(getResources().getColor(R.color.text_on_primary));
            tabInactiveText.setTypeface(null, android.graphics.Typeface.BOLD);
            
            tabActive.setCardBackgroundColor(getResources().getColor(R.color.input_background_dark));
            tabActiveText.setTextColor(getResources().getColor(R.color.slate_grey));
            tabActiveText.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
    }

    private void loadLiveAlerts() {
        showLoadingProgress();
        alertService.startLiveAlertsListener();
    }

    private void filterAndDisplayAlerts() {
        List<SOSAlert> filtered = new ArrayList<>();
        for (SOSAlert alert : allAlerts) {
            if ("ACTIVE".equals(currentFilter)) {
                if ("ACTIVE".equals(alert.getStatus()) || "TRIGGERED".equals(alert.getStatus())) {
                    filtered.add(alert);
                }
            } else {
                if ("RESOLVED".equals(alert.getStatus()) || "CANCELLED".equals(alert.getStatus())) {
                    filtered.add(alert);
                }
            }
        }
        displayAlerts(filtered);
    }

    private void displayAlerts(List<SOSAlert> alerts) {
        hideLoadingProgress();
        if (alerts.isEmpty()) {
            showEmptyState();
        } else {
            alertsRecycler.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
            errorState.setVisibility(View.GONE);
            alertAdapter.updateAlerts(alerts);
        }
    }

    private void showLoadingProgress() {
        loadingProgress.setVisibility(View.VISIBLE);
        alertsRecycler.setVisibility(View.GONE);
        emptyState.setVisibility(View.GONE);
        errorState.setVisibility(View.GONE);
    }

    private void hideLoadingProgress() {
        loadingProgress.setVisibility(View.GONE);
    }

    private void showEmptyState() {
        hideLoadingProgress();
        alertsRecycler.setVisibility(View.GONE);
        emptyState.setVisibility(View.VISIBLE);
        errorState.setVisibility(View.GONE);
        
        if ("ACTIVE".equals(currentFilter)) {
            emptyStateMessage.setText("No one needs help right now");
        } else {
            emptyStateMessage.setText("No resolved alerts found");
        }
    }

    private void showErrorState(String message) {
        hideLoadingProgress();
        alertsRecycler.setVisibility(View.GONE);
        emptyState.setVisibility(View.GONE);
        errorState.setVisibility(View.VISIBLE);
        errorMessage.setText(message);
    }

    private void hideErrorState() {
        errorState.setVisibility(View.GONE);
    }

    private void navigateToAlertDetail(SOSAlert alert) {
        Intent intent = new Intent(this, AlertDetailPageActivity.class);
        intent.putExtra("alertId", alert.getAlertId());
        startActivity(intent);
    }

    private void navigateToResponseOptions(SOSAlert alert) {
        if (alert.getLocation() != null) {
            Intent intent = new Intent(this, ResponseOptionsActivity.class);
            intent.putExtra("alertId", alert.getAlertId());
            intent.putExtra("latitude", alert.getLocation().getLatitude());
            intent.putExtra("longitude", alert.getLocation().getLongitude());
            intent.putExtra("userName", alert.getUserName());
            intent.putExtra("address", alert.getLocation().getAddress());
            intent.putExtra("userPhone", alert.getUserPhone());
            intent.putExtra("status", alert.getStatus());
            if (alert.getCreatedAt() != null) {
                intent.putExtra("timestamp", alert.getCreatedAt().toDate().getTime());
            }
            startActivity(intent);
        } else {
            Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onLiveAlertsUpdated(List<SOSAlert> alerts) {
        runOnUiThread(() -> {
            allAlerts = new ArrayList<>(alerts);
            filterAndDisplayAlerts();
        });
    }

    @Override
    public void onUserAlertsUpdated(List<SOSAlert> alerts) {
        // Not used in network alerts
    }

    @Override
    public void onError(String errorMessage, String type) {
        if ("LIVE_ALERTS".equals(type)) {
            runOnUiThread(() -> showErrorState(errorMessage));
        }
    }

    private void setupBottomNav() {
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
                Intent intent = new Intent(NetworkAlertsActivity.this, SettingsActivity.class);
                startActivity(intent);
            });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!alertService.isLiveAlertsListenerActive()) {
            alertService.startLiveAlertsListener();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        alertService.stopLiveAlertsListener();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        alertService.stopAllListeners();
    }
}
