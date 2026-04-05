package com.example.aapraksha;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

/**
 * AlertsActivity - Displays live SOS alerts from other users
 * Real-time updates via Firestore listeners
 */
public class AlertsActivity extends BaseActivity implements AlertService.AlertServiceCallback {
    private static final String TAG = "AlertsActivity";

    private RecyclerView alertsRecycler;
    private AlertListAdapter alertAdapter;
    private ProgressBar loadingProgress;
    private LinearLayout emptyState;
    private LinearLayout errorState;
    private TextView errorMessage;
    private Button retryButton;
    private ImageButton backButton;
    private ImageButton refreshButton;

    private AlertService alertService;
    private SOSAlertRepository sosAlertRepository;
    private ListenerRegistration liveAlertsListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alerts_live);

        initializeViews();
        setupRepositories();
        setupRecyclerView();
        setupClickListeners();
        loadLiveAlerts();
    }

    private void initializeViews() {
        alertsRecycler = findViewById(R.id.alerts_recycler);
        loadingProgress = findViewById(R.id.loading_progress);
        emptyState = findViewById(R.id.empty_state);
        errorState = findViewById(R.id.error_state);
        errorMessage = findViewById(R.id.error_message);
        retryButton = findViewById(R.id.retry_button);
        backButton = findViewById(R.id.back_button);
        refreshButton = findViewById(R.id.refresh_button);
    }

    private void setupRepositories() {
        sosAlertRepository = new SOSAlertRepository();
        alertService = new AlertService(sosAlertRepository);
        alertService.setCallback(this);
    }

    private void setupRecyclerView() {
        alertsRecycler.setLayoutManager(new LinearLayoutManager(this));
        alertAdapter = new AlertListAdapter(this, new ArrayList<>(), new AlertListAdapter.OnAlertClickListener() {
            @Override
            public void onAlertClicked(SOSAlert alert) {
                navigateToAlertDetail(alert);
            }

            @Override
            public void onRespondNowClicked(SOSAlert alert) {
                navigateToResponseOptions(alert);
            }
        });
        alertsRecycler.setAdapter(alertAdapter);
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> onBackPressed());
        
        refreshButton.setOnClickListener(v -> {
            hideErrorState();
            loadLiveAlerts();
        });

        retryButton.setOnClickListener(v -> {
            hideErrorState();
            loadLiveAlerts();
        });
    }

    private void loadLiveAlerts() {
        showLoadingProgress();
        alertService.startLiveAlertsListener();
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
        Intent intent = new Intent(this, ResponseOptionsActivity.class);
        intent.putExtra("alertId", alert.getAlertId());
        intent.putExtra("latitude", alert.getLocation().getLatitude());
        intent.putExtra("longitude", alert.getLocation().getLongitude());
        intent.putExtra("userName", alert.getUserName());
        startActivity(intent);
    }

    @Override
    public void onLiveAlertsUpdated(List<SOSAlert> alerts) {
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

    @Override
    public void onUserAlertsUpdated(List<SOSAlert> alerts) {
        // Not used in this activity
    }

    @Override
    public void onError(String errorMessage, String type) {
        if ("LIVE_ALERTS".equals(type)) {
            showErrorState(errorMessage);
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
