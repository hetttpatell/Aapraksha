package com.example.aapraksha;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * ResponseOptionsActivity - Shows response options when user clicks "Respond Now"
 * Options: View on Map, Call Contact
 */
public class ResponseOptionsActivity extends AppCompatActivity {
    private static final String TAG = "ResponseOptionsActivity";

    private String alertId;
    private double latitude;
    private double longitude;
    private String userNameStr;
    private String addressStr;
    private String userPhone;
    private String status;
    private long timestamp;

    // View Components
    private TextView userName;
    private TextView locationAddress;
    private TextView timeAgo;
    private TextView statusText;
    private View statusDot;
    private CardView viewOnMapBtn;
    private CardView callNowBtn;
    private ImageButton backButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_response_options);

        // Standard Navigation Initialization
        initializeViews();
        loadAlertData();
        setupClickListeners();
    }

    private void initializeViews() {
        userName = findViewById(R.id.user_name);
        locationAddress = findViewById(R.id.location_address);
        timeAgo = findViewById(R.id.time_ago);
        statusText = findViewById(R.id.status_text);
        statusDot = findViewById(R.id.status_dot);
        viewOnMapBtn = findViewById(R.id.view_on_map_btn);
        callNowBtn = findViewById(R.id.call_now_btn);
        backButton = findViewById(R.id.back_button);
    }

    private void loadAlertData() {
        Intent intent = getIntent();
        if (intent != null) {
            alertId = intent.getStringExtra("alertId");
            latitude = intent.getDoubleExtra("latitude", 0.0);
            longitude = intent.getDoubleExtra("longitude", 0.0);
            userNameStr = intent.getStringExtra("userName");
            addressStr = intent.getStringExtra("address");
            userPhone = intent.getStringExtra("userPhone");
            status = intent.getStringExtra("status");
            timestamp = intent.getLongExtra("timestamp", 0L);

            // Bind to UI
            if (userNameStr != null) userName.setText(userNameStr);
            if (addressStr != null) locationAddress.setText(addressStr);
            
            if (timestamp > 0) {
                timeAgo.setText(getTimeAgo(timestamp));
            } else {
                timeAgo.setText("Time unknown");
            }

            if ("RESOLVED".equals(status) || "CANCELLED".equals(status)) {
                statusText.setText("ALERT RESOLVED");
                statusText.setTextColor(getResources().getColor(R.color.slate_grey));
                if (statusDot != null) statusDot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.slate_grey)));
                callNowBtn.setAlpha(0.6f); 
            } else {
                statusText.setText("SOS SIGNAL DETECTED");
                statusText.setTextColor(getResources().getColor(R.color.sos_red));
                if (statusDot != null) statusDot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.sos_red)));
            }
        }
    }

    private void setupClickListeners() {
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }

        if (viewOnMapBtn != null) {
            viewOnMapBtn.setOnClickListener(v -> {
                if (latitude != 0 && longitude != 0) {
                    String label = (userNameStr != null ? userNameStr : "User") + "'s SOS Location";
                    String uri = String.format(Locale.ENGLISH, "geo:%f,%f?q=%f,%f(%s)", 
                        latitude, longitude, latitude, longitude, label);
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Coordinates not available", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (callNowBtn != null) {
            callNowBtn.setOnClickListener(v -> {
                if (userPhone != null && !userPhone.isEmpty()) {
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse("tel:" + userPhone));
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private String getTimeAgo(long timestamp) {
        long diff = System.currentTimeMillis() - timestamp;
        long minutes = diff / (1000 * 60);
        if (minutes < 1) return "Just now";
        if (minutes < 60) return minutes + "m ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + "h ago";
        return new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(new Date(timestamp));
    }
}
