package com.example.aapraksha;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * AlertDetailPageActivity - Shows complete alert details
 * Includes location, response statistics, contact responses, device info
 */
public class AlertDetailPageActivity extends AppCompatActivity {
    private static final String TAG = "AlertDetailPageActivity";

    private String alertId;
    private SOSAlert currentAlert;
    private AlertHistory currentAlertHistory;

    // Views
    private ImageButton backButton;
    private ImageButton shareButton;
    private ProgressBar loadingProgress;
    
    // Header
    private ImageView userPhoto;
    private TextView userName;
    private TextView alertTimestamp;
    private TextView alertStatus;
    
    // Location Section
    private ImageView mapPreview;
    private TextView locationAddress;
    private TextView locationAccuracy;
    
    // Statistics
    private TextView totalNotified;
    private TextView respondedCount;
    private TextView failedCount;
    
    // Contact Responses
    private RecyclerView contactResponsesRecycler;
    private ContactResponseAdapter contactResponseAdapter;
    private TextView noResponsesMessage;
    
    // Device Info
    private TextView deviceBattery;
    private TextView deviceNetwork;
    private TextView deviceSignal;
    
    private SOSAlertRepository sosAlertRepository;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert_detail_page);

        extractIntentData();
        initializeViews();
        setupRepositories();
        setupRecyclerView();
        setupClickListeners();
        loadAlertDetails();
    }

    private void extractIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            alertId = intent.getStringExtra("alertId");
        }
    }

    private void initializeViews() {
        backButton = findViewById(R.id.back_button);
        shareButton = findViewById(R.id.share_button);
        loadingProgress = findViewById(R.id.loading_progress);
        
        userPhoto = findViewById(R.id.user_photo);
        userName = findViewById(R.id.user_name);
        alertTimestamp = findViewById(R.id.alert_timestamp);
        alertStatus = findViewById(R.id.alert_status);
        
        mapPreview = findViewById(R.id.map_preview);
        locationAddress = findViewById(R.id.location_address);
        locationAccuracy = findViewById(R.id.location_accuracy);
        
        totalNotified = findViewById(R.id.total_notified);
        respondedCount = findViewById(R.id.responded_count);
        failedCount = findViewById(R.id.failed_count);
        
        contactResponsesRecycler = findViewById(R.id.contact_responses_recycler);
        noResponsesMessage = findViewById(R.id.no_responses_message);
        
        deviceBattery = findViewById(R.id.device_battery);
        deviceNetwork = findViewById(R.id.device_network);
        deviceSignal = findViewById(R.id.device_signal);
    }

    private void setupRepositories() {
        sosAlertRepository = new SOSAlertRepository();
    }

    private void setupRecyclerView() {
        contactResponsesRecycler.setLayoutManager(new LinearLayoutManager(this));
        contactResponseAdapter = new ContactResponseAdapter(this, new ArrayList<>());
        contactResponsesRecycler.setAdapter(contactResponseAdapter);
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> onBackPressed());
        shareButton.setOnClickListener(v -> shareAlert());
    }

    private void loadAlertDetails() {
        showLoadingProgress();
        
        sosAlertRepository.getAlertById(alertId, new SOSAlertRepository.OnAlertFetchListener() {
            @Override
            public void onSuccess(SOSAlert alert) {
                currentAlert = alert;
                hideLoadingProgress();
                displayAlertDetails(alert);
            }

            @Override
            public void onError(String errorMessage) {
                hideLoadingProgress();
                showErrorDialog(errorMessage);
            }
        });
    }

    private void displayAlertDetails(SOSAlert alert) {
        // Display Header
        if (alert.getUserName() != null) {
            userName.setText(alert.getUserName());
        }
        
        if (alert.getCreatedAt() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            alertTimestamp.setText(sdf.format(alert.getCreatedAt().toDate()));
        }
        
        if (alert.getStatus() != null) {
            alertStatus.setText(alert.getStatus());
            setStatusColor(alertStatus, alert.getStatus());
        }
        
        // Display Location
        if (alert.getLocation() != null) {
            if (alert.getLocation().getAddress() != null) {
                locationAddress.setText(alert.getLocation().getAddress());
            }
            locationAccuracy.setText(String.format("Accuracy: %.1fm", alert.getLocation().getAccuracy()));
        }
        
        // Display Statistics (from notificationsToContacts)
        int total = 0;
        int responded = 0;
        int failed = 0;
        
        if (alert.getNotificationsToContacts() != null && !alert.getNotificationsToContacts().isEmpty()) {
            total = alert.getNotificationsToContacts().size();
            // Suppress unchecked cast warning for this block
            @SuppressWarnings("unchecked")
            List<Object> notifications = (List<Object>) alert.getNotificationsToContacts();
            for (Object notif : notifications) {
                if (notif instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> notifMap = (Map<String, Object>) notif;
                    String status = (String) notifMap.get("responseStatus");
                    if ("REACHED".equals(status) || "ACKNOWLEDGED".equals(status)) {
                        responded++;
                    }
                }
            }
            failed = Math.max(0, total - responded);
        }
        
        totalNotified.setText(String.valueOf(total));
        respondedCount.setText(String.valueOf(responded));
        failedCount.setText(String.valueOf(failed));
        
        // Display Device Info
        SOSAlert.AlertDetails deviceInfo = alert.getDeviceInfoAsDetails();
        if (deviceInfo != null) {
            deviceBattery.setText(deviceInfo.getBatteryLevel() + "%");
            deviceNetwork.setText(deviceInfo.getNetworkType() != null ?
                    deviceInfo.getNetworkType() : "Unknown");
            deviceSignal.setText(getSignalStrengthLabel(deviceInfo.getSignalStrength()));
        }
    }

    private void displayContactResponses(List<AlertHistory.ContactNotificationInfo.NotificationDetail> responses) {
        if (responses == null || responses.isEmpty()) {
            noResponsesMessage.setVisibility(View.VISIBLE);
            contactResponsesRecycler.setVisibility(View.GONE);
        } else {
            noResponsesMessage.setVisibility(View.GONE);
            contactResponsesRecycler.setVisibility(View.VISIBLE);
            contactResponseAdapter = new ContactResponseAdapter(this, responses);
            contactResponsesRecycler.setAdapter(contactResponseAdapter);
        }
    }

    private void shareAlert() {
        try {
            StringBuilder shareText = new StringBuilder();
            shareText.append("Alert Details:\n");
            shareText.append("User: ").append(currentAlert.getUserName()).append("\n");
            shareText.append("Status: ").append(currentAlert.getStatus()).append("\n");
            
            if (currentAlert.getLocation() != null) {
                shareText.append("Location: ").append(currentAlert.getLocation().getAddress()).append("\n");
                shareText.append("Coordinates: ").append(currentAlert.getLocation().getLatitude())
                        .append(", ").append(currentAlert.getLocation().getLongitude()).append("\n");
            }
            
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText.toString());
            startActivity(Intent.createChooser(shareIntent, "Share Alert"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showLoadingProgress() {
        loadingProgress.setVisibility(View.VISIBLE);
    }

    private void hideLoadingProgress() {
        loadingProgress.setVisibility(View.GONE);
    }

    private void showErrorDialog(String message) {
        android.widget.Toast.makeText(this, "Error: " + message, android.widget.Toast.LENGTH_SHORT).show();
    }

    private void setStatusColor(TextView statusView, String status) {
        if ("ACTIVE".equals(status) || "TRIGGERED".equals(status)) {
            statusView.setTextColor(getResources().getColor(R.color.sos_red));
        } else if ("CANCELLED".equals(status)) {
            statusView.setTextColor(getResources().getColor(R.color.slate_grey));
        } else if ("RESOLVED".equals(status)) {
            statusView.setTextColor(getResources().getColor(R.color.electric_indigo));
        } else {
            statusView.setTextColor(getResources().getColor(R.color.midnight_blue));
        }
    }

    private String getSignalStrengthLabel(int strength) {
        if (strength >= 4) return "Excellent";
        if (strength >= 3) return "Good";
        if (strength >= 2) return "Fair";
        if (strength >= 1) return "Poor";
        return "No Signal";
    }
}
