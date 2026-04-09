package com.example.aapraksha;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

/**
 * HistoryActivity - Displays user's historical SOS alerts
 * Real-time updates via Firestore listeners
 * Filtering by alert type (All/SOS/Check-ins)
 */
public class HistoryActivity extends BaseActivity implements AlertService.AlertServiceCallback {
    private static final String TAG = "HistoryActivity";

    private RecyclerView historyRecycler;
    private HistoryListAdapter historyAdapter;
    private ProgressBar loadingProgress;
    private LinearLayout emptyState;
    private LinearLayout errorState;
    private TextView errorMessage;
    private CardView retryButton;
    private ImageView backButton;
    
    // Filter Tabs (CardViews matching original design)
    private CardView tabAllAlerts;
    private CardView tabSOS;
    private CardView tabCheckIns;
    private TextView tabAllAlertsText;
    private TextView tabSOSText;
    private TextView tabCheckInsText;

    // Bottom Nav
    private LinearLayout navHome;
    private LinearLayout navSafeZones;
    private LinearLayout navHistory;
    private LinearLayout navSettings;

    private AlertService alertService;
    private SOSAlertRepository sosAlertRepository;
    private FirebaseAuth auth;
    private String currentUserId;
    private String currentFilter = "ALL"; // ALL, SOS, CHECK_IN
    private List<SOSAlert> allLoadedAlerts = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        initializeFirebase();
        initializeViews();
        setupRepositories();
        setupRecyclerView();
        setupFilterTabs();
        setupClickListeners();
        setupBottomNav();
        loadHistoryAlerts();
    }

    private void initializeFirebase() {
        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            currentUserId = auth.getCurrentUser().getUid();
        }
    }

    private void initializeViews() {
        historyRecycler = findViewById(R.id.history_recycler);
        loadingProgress = findViewById(R.id.loading_progress);
        emptyState = findViewById(R.id.empty_state);
        errorState = findViewById(R.id.error_state);
        errorMessage = findViewById(R.id.error_message);
        retryButton = findViewById(R.id.retry_button);
        backButton = findViewById(R.id.btn_back);
        
        // Filter tabs
        tabAllAlerts = findViewById(R.id.tab_all_alerts);
        tabSOS = findViewById(R.id.tab_sos);
        tabCheckIns = findViewById(R.id.tab_check_ins);
        tabAllAlertsText = findViewById(R.id.tab_all_alerts_text);
        tabSOSText = findViewById(R.id.tab_sos_text);
        tabCheckInsText = findViewById(R.id.tab_check_ins_text);

        // Bottom nav
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
        historyRecycler.setLayoutManager(new LinearLayoutManager(this));
        historyAdapter = new HistoryListAdapter(this, new ArrayList<>(), new HistoryListAdapter.OnHistoryItemClickListener() {
            @Override
            public void onViewDetailsClicked(SOSAlert alert) {
                navigateToAlertDetail(alert);
            }

            @Override
            public void onShareClicked(SOSAlert alert) {
                shareAlert(alert);
            }
        });
        historyRecycler.setAdapter(historyAdapter);
    }

    private void setupFilterTabs() {
        tabAllAlerts.setOnClickListener(v -> {
            currentFilter = "ALL";
            updateFilterTabStyles();
            loadHistoryAlerts();
        });

        tabSOS.setOnClickListener(v -> {
            currentFilter = "SOS";
            updateFilterTabStyles();
            loadHistoryAlerts();
        });

        tabCheckIns.setOnClickListener(v -> {
            currentFilter = "CHECK_IN";
            updateFilterTabStyles();
            loadHistoryAlerts();
        });
    }

    private void updateFilterTabStyles() {
        // Reset all tabs to inactive
        tabAllAlerts.setCardBackgroundColor(getResources().getColor(R.color.input_background_dark));
        tabSOS.setCardBackgroundColor(getResources().getColor(R.color.input_background_dark));
        tabCheckIns.setCardBackgroundColor(getResources().getColor(R.color.input_background_dark));
        tabAllAlertsText.setTextColor(getResources().getColor(R.color.slate_grey));
        tabSOSText.setTextColor(getResources().getColor(R.color.slate_grey));
        tabCheckInsText.setTextColor(getResources().getColor(R.color.slate_grey));

        // Set active tab
        if ("ALL".equals(currentFilter)) {
            tabAllAlerts.setCardBackgroundColor(getResources().getColor(R.color.electric_indigo));
            tabAllAlertsText.setTextColor(getResources().getColor(R.color.text_on_primary));
        } else if ("SOS".equals(currentFilter)) {
            tabSOS.setCardBackgroundColor(getResources().getColor(R.color.electric_indigo));
            tabSOSText.setTextColor(getResources().getColor(R.color.text_on_primary));
        } else if ("CHECK_IN".equals(currentFilter)) {
            tabCheckIns.setCardBackgroundColor(getResources().getColor(R.color.electric_indigo));
            tabCheckInsText.setTextColor(getResources().getColor(R.color.text_on_primary));
        }
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> onBackPressed());
        
        retryButton.setOnClickListener(v -> {
            hideErrorState();
            loadHistoryAlerts();
        });
    }

    private void setupBottomNav() {
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
            });
        }

        if (navSettings != null) {
            navSettings.setOnClickListener(v -> {
                Intent intent = new Intent(HistoryActivity.this, SettingsActivity.class);
                startActivity(intent);
            });
        }
    }

    private void loadHistoryAlerts() {
        if (currentUserId == null) return;
        
        showLoadingProgress();
        
        if (!alertService.isUserAlertsListenerActive()) {
            // Start real-time listener if not already active
            alertService.startUserAlertsListener(currentUserId);
        } else {
            // Just apply filter to cached alerts
            applyFilter();
        }
    }

    private void applyFilter() {
        List<SOSAlert> filteredAlerts = new ArrayList<>();
        
        for (SOSAlert alert : allLoadedAlerts) {
            if ("ALL".equals(currentFilter)) {
                filteredAlerts.add(alert);
            } else if ("SOS".equals(currentFilter) && 
                       ("SOS".equals(alert.getAlertType()) || "EMERGENCY_SOS".equals(alert.getAlertType()))) {
                filteredAlerts.add(alert);
            } else if ("CHECK_IN".equals(currentFilter) && 
                       "CHECK_IN".equals(alert.getAlertType())) {
                filteredAlerts.add(alert);
            }
        }
        
        runOnUiThread(() -> {
            hideLoadingProgress();
            if (!filteredAlerts.isEmpty()) {
                historyRecycler.setVisibility(View.VISIBLE);
                emptyState.setVisibility(View.GONE);
                errorState.setVisibility(View.GONE);
                historyAdapter.updateAlerts(filteredAlerts);
            } else {
                showEmptyState();
            }
        });
    }

    private void displayAlerts(List<SOSAlert> alerts) {
        hideLoadingProgress();
        if (alerts.isEmpty()) {
            showEmptyState();
        } else {
            historyRecycler.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
            errorState.setVisibility(View.GONE);
            historyAdapter.updateAlerts(alerts);
        }
    }

    private void showLoadingProgress() {
        loadingProgress.setVisibility(View.VISIBLE);
        historyRecycler.setVisibility(View.GONE);
        emptyState.setVisibility(View.GONE);
        errorState.setVisibility(View.GONE);
    }

    private void hideLoadingProgress() {
        loadingProgress.setVisibility(View.GONE);
    }

    private void hideErrorState() {
        errorState.setVisibility(View.GONE);
        historyRecycler.setVisibility(View.VISIBLE);
    }

    private void showEmptyState() {
        hideLoadingProgress();
        historyRecycler.setVisibility(View.GONE);
        emptyState.setVisibility(View.VISIBLE);
        errorState.setVisibility(View.GONE);
    }

    private void showErrorState(String message) {
        hideLoadingProgress();
        historyRecycler.setVisibility(View.GONE);
        emptyState.setVisibility(View.GONE);
        errorState.setVisibility(View.VISIBLE);
        errorMessage.setText(message);
    }

    private void navigateToAlertDetail(SOSAlert alert) {
        Intent intent = new Intent(this, AlertDetailPageActivity.class);
        intent.putExtra("alertId", alert.getAlertId());
        startActivity(intent);
    }

    private void shareAlert(SOSAlert alert) {
        try {
            StringBuilder shareText = new StringBuilder();
            shareText.append("Alert Details:\n");
            shareText.append("Status: ").append(alert.getStatus()).append("\n");
            
            if (alert.getLocation() != null) {
                shareText.append("Location: ").append(alert.getLocation().getAddress()).append("\n");
            }
            
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText.toString());
            startActivity(Intent.createChooser(shareIntent, "Share Alert"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLiveAlertsUpdated(List<SOSAlert> alerts) {
        // Not used in history
    }

    @Override
    public void onUserAlertsUpdated(List<SOSAlert> alerts) {
        allLoadedAlerts = alerts;
        applyFilter();
    }

    @Override
    public void onError(String errorMessage, String type) {
        if ("USER_ALERTS".equals(type)) {
            runOnUiThread(() -> showErrorState(errorMessage));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (currentUserId != null && !alertService.isUserAlertsListenerActive()) {
            alertService.startUserAlertsListener(currentUserId);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        alertService.stopUserAlertsListener();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        alertService.stopAllListeners();
    }
}
