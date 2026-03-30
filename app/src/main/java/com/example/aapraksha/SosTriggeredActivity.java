package com.example.aapraksha;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SosTriggeredActivity extends AppCompatActivity {

    private static final String TAG = "SosTriggeredActivity";
    private static final int PERMISSION_REQUEST_CODE = 200;
    
    // UI Elements
    private TextView tvCountdown;
    private TextView tvCountdownText;
    private TextView tvLocationAddress;
    private TextView tvLatitude;
    private TextView tvLongitude;
    private TextView tvAccuracyValue;
    private TextView tvAccuracyStatus;
    private TextView tvContactCount;
    private TextView tvContactStatus;
    private TextView tvFailedCount;
    
    private View sosPulse1;
    private View sosPulse2;
    private View sosPulse3;
    private CardView btnCancelSos;
    
    // Animation & Timers
    private AnimatorSet continuousPulseAnimator;
    private CountDownTimer countDownTimer;
    
    // Services
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Geocoder geocoder;
    
    // Data
    private List<EmergencyContact> contactList = new ArrayList<>();
    private double latitude = 0.0;
    private double longitude = 0.0;
    private String currentAlertId;
    private boolean locationReceived = false;
    private DecimalFormat df = new DecimalFormat("#.####");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sos_triggered);

        // UI Initialization
        tvCountdown = findViewById(R.id.tv_countdown);
        tvCountdownText = findViewById(R.id.tv_countdown_text);
        tvLocationAddress = findViewById(R.id.tv_location); // Maps to 'tv_location' in XML based on previous check, but we'll use it as address
        tvLatitude = findViewById(R.id.tv_latitude);
        tvLongitude = findViewById(R.id.tv_longitude);
        tvAccuracyValue = findViewById(R.id.tv_accuracy_value);
        tvAccuracyStatus = findViewById(R.id.tv_accuracy_status);
        tvContactCount = findViewById(R.id.tv_audio_count); // Reusing these IDs for status display
        tvContactStatus = findViewById(R.id.tv_message_count);
        tvFailedCount = findViewById(R.id.tv_video_count);
        
        sosPulse1 = findViewById(R.id.sos_pulse_1);
        sosPulse2 = findViewById(R.id.sos_pulse_2);
        sosPulse3 = findViewById(R.id.sos_pulse_3);
        btnCancelSos = findViewById(R.id.btn_cancel_sos);

        // Firebase & Location Initialization
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        geocoder = new Geocoder(this, Locale.getDefault());

        // Logic Start
        checkPermissions();
        loadEmergencyContacts();
        setupLocationUpdates();
        getLastLocation();
        
        startPulseAnimation();
        startCountdown();

        if (btnCancelSos != null) {
            btnCancelSos.setOnClickListener(v -> cancelSos());
        }
    }

    private void startPulseAnimation() {
        continuousPulseAnimator = new AnimatorSet();
        
        AnimatorSet anim1 = createPulseAnimator(sosPulse1, 0);
        AnimatorSet anim2 = createPulseAnimator(sosPulse2, 400);
        AnimatorSet anim3 = createPulseAnimator(sosPulse3, 800);
        
        continuousPulseAnimator.playTogether(anim1, anim2, anim3);
        continuousPulseAnimator.start();
    }

    private AnimatorSet createPulseAnimator(View view, long delay) {
        if (view == null) return new AnimatorSet();
        
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 2.4f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 2.4f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, "alpha", 0.8f, 0f);
        
        scaleX.setDuration(1200);
        scaleY.setDuration(1200);
        alpha.setDuration(1200);
        
        scaleX.setStartDelay(delay);
        scaleY.setStartDelay(delay);
        alpha.setStartDelay(delay);

        scaleX.setRepeatCount(ObjectAnimator.INFINITE);
        scaleY.setRepeatCount(ObjectAnimator.INFINITE);
        alpha.setRepeatCount(ObjectAnimator.INFINITE);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(scaleX, scaleY, alpha);
        set.setInterpolator(new LinearInterpolator());
        return set;
    }

    private void startCountdown() {
        countDownTimer = new CountDownTimer(5000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secondsRemaining = (int) (millisUntilFinished / 1000);
                if (tvCountdown != null) tvCountdown.setText(String.format("%02d", secondsRemaining));
                if (tvCountdownText != null) tvCountdownText.setText("Triggering alert in " + secondsRemaining + " seconds...");
            }

            @Override
            public void onFinish() {
                if (tvCountdown != null) tvCountdown.setText("00");
                if (tvCountdownText != null) tvCountdownText.setText("SOS Alert Sent!");
                Toast.makeText(SosTriggeredActivity.this, "🚨 Emergency Alert Sent Successfully!", Toast.LENGTH_LONG).show();
                
                createSOSAlert();
            }
        }.start();
    }

    private void setupLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateDistanceMeters(0)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    updateLocationUI(location);
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                    locationReceived = true;
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void updateLocationUI(Location location) {
        if (tvLatitude != null) tvLatitude.setText(df.format(location.getLatitude()));
        if (tvLongitude != null) tvLongitude.setText(df.format(location.getLongitude()));
        if (tvAccuracyValue != null) tvAccuracyValue.setText(String.format("%.0fm", location.getAccuracy()));
        
        // Update accuracy status
        float accuracy = location.getAccuracy();
        if (tvAccuracyStatus != null) {
            if (accuracy < 10) {
                tvAccuracyStatus.setText("HIGH");
                tvAccuracyStatus.setTextColor(getColor(android.R.color.holo_green_light));
            } else if (accuracy < 30) {
                tvAccuracyStatus.setText("MEDIUM");
                tvAccuracyStatus.setTextColor(getColor(android.R.color.holo_orange_light));
            } else {
                tvAccuracyStatus.setText("LOW");
                tvAccuracyStatus.setTextColor(getColor(android.R.color.holo_red_light));
            }
        }

        // Reverse geocode address
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String addressText = (address.getThoroughfare() != null ? address.getThoroughfare() + ", " : "") +
                        (address.getLocality() != null ? address.getLocality() : "");
                if (tvLocationAddress != null) tvLocationAddress.setText(addressText.isEmpty() ? "Location Obtained" : addressText);
            }
        } catch (IOException e) {
            Log.e(TAG, "Geocoding error: " + e.getMessage());
        }
    }

    private void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                        updateLocationUI(location);
                    } else {
                        latitude = 28.4595; // Fallback
                        longitude = 77.0266;
                        if (tvLocationAddress != null) tvLocationAddress.setText("Fetching location...");
                    }
                });
    }

    private void checkPermissions() {
        String[] permissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.SEND_SMS
        };
        List<String> missing = new ArrayList<>();
        for (String p : permissions) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                missing.add(p);
            }
        }
        if (!missing.isEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }
    }

    private void loadEmergencyContacts() {
        if (auth.getCurrentUser() == null) return;

        String uid = auth.getCurrentUser().getUid();
        db.collection("users").document(uid)
                .collection("emergencyContacts")
                .orderBy("name", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    contactList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        EmergencyContact contact = doc.toObject(EmergencyContact.class);
                        if (contact != null) {
                            contact.setId(doc.getId());
                            contactList.add(contact);
                        }
                    }
                    if (tvContactCount != null) tvContactCount.setText(contactList.size() + " Found");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load contacts", e);
                });
    }

    private void createSOSAlert() {
        if (auth.getCurrentUser() == null) return;

        String userId = auth.getCurrentUser().getUid();
        
        Map<String, Object> sosAlert = new HashMap<>();
        sosAlert.put("userId", userId);
        sosAlert.put("timestamp", System.currentTimeMillis());
        sosAlert.put("latitude", latitude);
        sosAlert.put("longitude", longitude);
        sosAlert.put("status", "active");
        sosAlert.put("type", "SOS");

        db.collection("alerts")
                .add(sosAlert)
                .addOnSuccessListener(docRef -> {
                    currentAlertId = docRef.getId();
                    sendSmsToContacts();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create SOS alert", e);
                });
    }

    private void sendSmsToContacts() {
        if (contactList.isEmpty()) return;
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        ArrayList<String> phoneNumbers = new ArrayList<>();
        for (EmergencyContact contact : contactList) {
            String phone = contact.getPhone();
            if (phone != null && !phone.isEmpty()) {
                phoneNumbers.add(phone);
            }
        }

        if (phoneNumbers.isEmpty()) return;

        String message = "🚨 EMERGENCY SOS! I am in danger! My current Location: https://maps.google.com/?q=" + latitude + "," + longitude;
        
        SMSHelper.sendSMS(this, message, phoneNumbers, new SMSHelper.OnSMSStatusListener() {
            @Override
            public void onSmsSent(int successCount, int totalCount, List<String> failedPhones) {
                if (tvContactStatus != null) tvContactStatus.setText(successCount + " Sent");
                if (tvFailedCount != null) tvFailedCount.setText(failedPhones.size() + " Failed");
                if (tvCountdownText != null) tvCountdownText.setText("✓ SOS sent to " + successCount + " contacts");
            }

            @Override
            public void onSmsError(String error) {
                Log.e(TAG, "SMS Error: " + error);
                if (tvContactStatus != null) tvContactStatus.setText("Error");
            }
        });
    }

    private void cancelSos() {
        if (countDownTimer != null) countDownTimer.cancel();
        if (continuousPulseAnimator != null) continuousPulseAnimator.cancel();
        if (locationCallback != null) fusedLocationClient.removeLocationUpdates(locationCallback);

        if (currentAlertId != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("status", "cancelled");

            db.collection("alerts").document(currentAlertId)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "SOS Alert Cancelled", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Cancel failed", e);
                        finish();
                    });
        } else {
            Toast.makeText(this, "SOS Alert Cancelled", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (int result : grantResults) {
                if (result == PackageManager.PERMISSION_GRANTED) {
                    setupLocationUpdates();
                    getLastLocation();
                    loadEmergencyContacts();
                    break;
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        cancelSos();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
        if (continuousPulseAnimator != null) continuousPulseAnimator.cancel();
        if (locationCallback != null) fusedLocationClient.removeLocationUpdates(locationCallback);
    }
}
