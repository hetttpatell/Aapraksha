package com.example.aapraksha;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.util.Log;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ListenerRegistration;

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
    private TextView alertType;
    
    // Location Section
    private ImageView mapPreview;
    private TextView locationAddress;
    private TextView locationAccuracy;
    private TextView locationCoordinates;
    
    // Statistics (Deprecated/Removed as per UI redesign)
    // private TextView totalNotified, respondedCount, failedCount, totalSosAlerts;
    
    // Contact Responses
    private RecyclerView contactResponsesRecycler;
    private ContactResponseAdapter contactResponseAdapter;
    private TextView noResponsesMessage;
    
    // Buttons
    private View btnCallEmergency, btnNavigate;
    
    private SOSAlertRepository sosAlertRepository;
    private ListenerRegistration alertListener;
    private ListenerRegistration userListener;

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
        // Guard: if no alertId, finish activity
        if (alertId == null || alertId.isEmpty()) {
            android.widget.Toast.makeText(this, "Alert ID not found", android.widget.Toast.LENGTH_SHORT).show();
            finish();
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
        alertType = findViewById(R.id.alert_type);
        
        mapPreview = findViewById(R.id.map_preview);
        locationAddress = findViewById(R.id.location_address);
        locationAccuracy = findViewById(R.id.location_accuracy);
        locationCoordinates = findViewById(R.id.location_coordinates);
        
        // Statistics (Removed as per UI redesign)
        /*
        totalNotified = findViewById(R.id.notified_count);
        respondedCount = findViewById(R.id.responded_count);
        failedCount = findViewById(R.id.failed_count);
        totalSosAlerts = findViewById(R.id.total_sos_alerts);
        */
        
        contactResponsesRecycler = findViewById(R.id.contact_responses_recycler);
        noResponsesMessage = findViewById(R.id.no_responses_message);
        
        btnCallEmergency = findViewById(R.id.btn_call_emergency);
        btnNavigate = findViewById(R.id.btn_navigate);
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
        mapPreview.setOnClickListener(v -> openMap());
        
        if (btnCallEmergency != null) {
            btnCallEmergency.setOnClickListener(v -> callEmergency());
        }
        
        if (btnNavigate != null) {
            btnNavigate.setOnClickListener(v -> openMap());
        }
    }

    private void loadAlertDetails() {
        showLoadingProgress();
        
        alertListener = sosAlertRepository.listenToAlertById(alertId, new SOSAlertRepository.OnAlertFetchListener() {
            @Override
            public void onSuccess(SOSAlert alert) {
                currentAlert = alert;
                hideLoadingProgress();
                displayAlertDetails(alert);
                
                // Start listening to user details if not already doing so
                if (userListener == null && alert.getUserId() != null) {
                    listenToUserDetails(alert.getUserId());
                }
                
                // Extract and display contact responses
                if (alert.getNotificationsToContacts() != null && !alert.getNotificationsToContacts().isEmpty()) {
                    List<AlertHistory.ContactNotificationInfo.NotificationDetail> details = new ArrayList<>();
                    List<Object> notifications = alert.getNotificationsToContacts();
                    for (Object obj : notifications) {
                        if (obj instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> map = (Map<String, Object>) obj;
                            AlertHistory.ContactNotificationInfo.NotificationDetail detail = new AlertHistory.ContactNotificationInfo.NotificationDetail();
                            
                            // Handle both field name variants: 'name' (from createSOSAlert) and 'contactName'
                            String contactName = (String) map.get("contactName");
                            if (contactName == null) contactName = (String) map.get("name");
                            detail.setContactName(contactName);
                            
                            // Handle response status: check 'responseStatus' and 'notificationStatus'
                            String respStatus = (String) map.get("responseStatus");
                            if (respStatus == null) respStatus = (String) map.get("notificationStatus");
                            detail.setResponseStatus(respStatus);
                            
                            Object notifiedAt = map.get("notifiedAt");
                            if (notifiedAt instanceof Timestamp) {
                                detail.setNotifiedAt((Timestamp) notifiedAt);
                            }
                            
                            Object respTime = map.get("responseTime");
                            if (respTime instanceof Number) {
                                detail.setResponseTime(((Number) respTime).intValue());
                            }
                            details.add(detail);
                        }
                    }
                    displayContactResponses(details);
                } else {
                    // No contacts to show
                    displayContactResponses(new ArrayList<>());
                }
            }

            @Override
            public void onError(String errorMessage) {
                hideLoadingProgress();
                showErrorDialog(errorMessage);
            }
        });
    }

    private void listenToUserDetails(String userId) {
        userListener = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users").document(userId)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null || documentSnapshot == null || !documentSnapshot.exists()) return;
                    
                    String fullName = documentSnapshot.getString("fullName");
                    String photoUrl = documentSnapshot.getString("profilePhotoUrl");
                    
                    if (fullName != null) {
                        userName.setText(fullName);
                    }
                    
                    if (photoUrl != null && !photoUrl.isEmpty()) {
                        Glide.with(this)
                            .load(photoUrl)
                            .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                            .placeholder(R.drawable.ic_profile)
                            .into(userPhoto);
                    }
                });
    }

    private void displayAlertDetails(SOSAlert alert) {
        if (alert == null) {
            Log.e(TAG, "displayAlertDetails: Alert object is null");
            return;
        }

        try {
            Log.d(TAG, "displayAlertDetails: Updating UI for alert: " + alert.getAlertId());
            
            // 1. Header Information
            userName.setText(alert.getUserName() != null ? alert.getUserName() : "---");
            if (alert.getCreatedAt() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
                alertTimestamp.setText(sdf.format(alert.getCreatedAt().toDate()));
            } else {
                alertTimestamp.setText("---");
            }

            // 2. Status & Type
            String status = alert.getStatus() != null ? alert.getStatus() : "ACTIVE";
            alertStatus.setText(alert.getStatusDisplayText());
            setStatusColor(alertStatus, status);
            
            String typeStr = alert.getAlertType();
            if (typeStr != null) {
                typeStr = typeStr.replace("EMERGENCY_", "").replace("_", " ");
                alertType.setText(typeStr);
            } else {
                alertType.setText("EMERGENCY SOS");
            }

            // 3. Statistics (Removed as per UI redesign - individual responses still shown in list)
            Log.d(TAG, "Statistics calculation skipped as per UI redesign");

            // 4. Location Details & Map Preview
            if (alert.getLocation() != null) {
                locationAddress.setText(alert.getLocation().getAddress() != null ? 
                        alert.getLocation().getAddress() : "Location unavailable");
                locationAccuracy.setText(String.format(Locale.getDefault(), "Accuracy: %.1fm", alert.getLocation().getAccuracy()));
                
                double lat = alert.getLocation().getLatitude();
                double lng = alert.getLocation().getLongitude();
                locationCoordinates.setText(String.format(Locale.getDefault(), "Lat: %.6f, Lng: %.6f", lat, lng));
                
                if (lat != 0 || lng != 0) {
                    String mapUrl = String.format(Locale.US, 
                        "https://static-maps.yandex.ru/1.x/?lang=en_US&ll=%f,%f&z=14&l=map&size=600,300&pt=%f,%f,pm2rdm",
                        lng, lat, lng, lat);
                        
                    Glide.with(this)
                        .load(mapUrl)
                        .placeholder(R.color.surface_dark)
                        .error(R.color.surface_dark)
                        .into(mapPreview);
                }
            }

            // 6. User Profile Photo (Fallback to alert data if listener hasn't updated yet)
            if (alert.getUserProfilePhoto() != null && !alert.getUserProfilePhoto().isEmpty() && !isDestroyed()) {
                Glide.with(this)
                        .load(alert.getUserProfilePhoto())
                        .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                        .placeholder(R.drawable.ic_profile)
                        .into(userPhoto);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in displayAlertDetails", e);
        }
    }

    private void displayContactResponses(List<AlertHistory.ContactNotificationInfo.NotificationDetail> responses) {
        if (responses == null || responses.isEmpty()) {
            noResponsesMessage.setVisibility(View.VISIBLE);
            contactResponsesRecycler.setVisibility(View.GONE);
        } else {
            noResponsesMessage.setVisibility(View.GONE);
            contactResponsesRecycler.setVisibility(View.VISIBLE);
            if (contactResponseAdapter == null) {
                contactResponseAdapter = new ContactResponseAdapter(this, responses);
                contactResponsesRecycler.setAdapter(contactResponseAdapter);
            } else {
                contactResponseAdapter.updateResponses(responses);
            }
        }
    }

    private void callEmergency() {
        if (currentAlert != null && currentAlert.getUserPhone() != null) {
            String phoneNumber = currentAlert.getUserPhone();
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + phoneNumber));
            startActivity(intent);
        } else {
            // Default to emergency number if user phone not available
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:112")); // Universal emergency number
            startActivity(intent);
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

    private void openMap() {
        if (currentAlert != null && currentAlert.getLocation() != null) {
            double lat = currentAlert.getLocation().getLatitude();
            double lng = currentAlert.getLocation().getLongitude();
            String label = (currentAlert.getUserName() != null ? currentAlert.getUserName() : "User") + "'s SOS Location";
            String uri = String.format(Locale.ENGLISH, "geo:%f,%f?q=%f,%f(%s)", 
                lat, lng, lat, lng, label);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            startActivity(intent);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (alertListener != null) {
            alertListener.remove();
        }
        if (userListener != null) {
            userListener.remove();
        }
    }
}
