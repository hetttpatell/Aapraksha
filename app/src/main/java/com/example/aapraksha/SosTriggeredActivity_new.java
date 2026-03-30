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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SosTriggeredActivity extends AppCompatActivity {

    private static final String TAG = "SOS_TRIGGERED";
    private static final int PERMISSION_REQUEST_CODE = 100;
    
    private TextView tvCountdown;
    private TextView tvCountdownText;
    private TextView tvContactCount;
    private TextView tvContactStatus;
    private TextView tvFailedCount;
    private TextView tvLocationAddress;
    private TextView tvLatitude;
    private TextView tvLongitude;
    private TextView tvAccuracyValue;
    private TextView tvAccuracyStatus;
    
    private View sosPulse1;
    private View sosPulse2;
    private View sosPulse3;
    private CardView btnCancelSos;
    private AnimatorSet continuousPulseAnimator;
    private CountDownTimer countDownTimer;
    
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Geocoder geocoder;
    
    private double latitude = 0.0;
    private double longitude = 0.0;
    private String currentAlertId;
    private boolean locationReceived = false;
    private int successfulContacts = 0;
    private int failedContacts = 0;
    private int totalContacts = 0;
    private DecimalFormat df = new DecimalFormat("#.####");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sos_triggered);

        tvCountdown = findViewById(R.id.tv_countdown);
        tvCountdownText = findViewById(R.id.tv_countdown_text);
        tvContactCount = findViewById(R.id.tv_contact_count);
        tvContactStatus = findViewById(R.id.tv_contact_status);
        tvFailedCount = findViewById(R.id.tv_failed_count);
        tvLocationAddress = findViewById(R.id.tv_location_address);
        tvLatitude = findViewById(R.id.tv_latitude);
        tvLongitude = findViewById(R.id.tv_longitude);
        tvAccuracyValue = findViewById(R.id.tv_accuracy_value);
        tvAccuracyStatus = findViewById(R.id.tv_accuracy_status);
        
        sosPulse1 = findViewById(R.id.sos_pulse_1);
        sosPulse2 = findViewById(R.id.sos_pulse_2);
        sosPulse3 = findViewById(R.id.sos_pulse_3);
        btnCancelSos = findViewById(R.id.btn_cancel_sos);

        // Initialize Firebase and Location
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        geocoder = new Geocoder(this, Locale.getDefault());

        // Get current location
        getLastLocation();
        startPulseAnimation();
        startCountdown();
        setupLocationUpdates();

        btnCancelSos.setOnClickListener(v -> cancelSos());
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
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.8f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.8f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, "alpha", 0.6f, 0f);
        
        scaleX.setDuration(1000);
        scaleY.setDuration(1000);
        alpha.setDuration(1000);
        
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
                tvCountdown.setText(String.format("%02d", secondsRemaining));
                tvCountdownText.setText("Sending in " + secondsRemaining + "s...");
            }

            @Override
            public void onFinish() {
                tvCountdown.setText("00");
                tvCountdownText.setText("Alert sent!");
                createSOSAlert();
            }
        }.start();
    }

    private void setupLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateDistanceMeters(0)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                
                for (Location location : locationResult.getLocations()) {
                    updateLocationUI(location);
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                    locationReceived = true;
                }
            }
        };

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    private void updateLocationUI(Location location) {
        tvLatitude.setText(df.format(location.getLatitude()));
        tvLongitude.setText(df.format(location.getLongitude()));
        tvAccuracyValue.setText(String.format("%.0fm", location.getAccuracy()));
        
        // Update accuracy status
        float accuracy = location.getAccuracy();
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

        // Reverse geocode address
        try {
            List<Address> addresses = geocoder.getFromLocation(
                    location.getLatitude(),
                    location.getLongitude(),
                    1
            );
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String addressText = (address.getThoroughfare() != null ? address.getThoroughfare() + ", " : "") +
                        (address.getLocality() != null ? address.getLocality() : "");
                tvLocationAddress.setText(addressText.isEmpty() ? "Location Unknown" : addressText);
            }
        } catch (IOException e) {
            Log.e(TAG, "Geocoding error: " + e.getMessage());
        }
    }

    private void getLastLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                            updateLocationUI(location);
                            Log.d(TAG, "Location obtained: " + latitude + ", " + longitude);
                        } else {
                            latitude = 28.4595;
                            longitude = 77.0266;
                            Log.w(TAG, "Location not available, using fallback");
                            tvLocationAddress.setText("Fetching location...");
                        }
                    });
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_CODE);
        }
    }

    private void createSOSAlert() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        
        java.util.Map<String, Object> sosAlert = new java.util.HashMap<>();
        sosAlert.put("userId", userId);
        sosAlert.put("timestamp", System.currentTimeMillis());
        sosAlert.put("latitude", latitude);
        sosAlert.put("longitude", longitude);
        sosAlert.put("status", "active");
        sosAlert.put("type", "SOS");

        db.collection("sosAlerts")
                .add(sosAlert)
                .addOnSuccessListener(docRef -> {
                    currentAlertId = docRef.getId();
                    Log.d(TAG, "SOS Alert created: " + currentAlertId);
                    sendSmsToEmergencyContacts();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create SOS alert", e);
                    Toast.makeText(this, "Failed to create alert: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void sendSmsToEmergencyContacts() {
        if (auth.getCurrentUser() == null) return;

        String userId = auth.getCurrentUser().getUid();

        db.collection("users").document(userId).collection("emergencyContacts")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> phoneNumbers = new ArrayList<>();
                    totalContacts = 0;

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String phone = doc.getString("phone");
                        if (phone != null && !phone.isEmpty()) {
                            phoneNumbers.add(phone);
                            totalContacts++;
                        }
                    }

                    tvContactCount.setText(String.valueOf(totalContacts));

                    if (phoneNumbers.isEmpty()) {
                        Toast.makeText(this, "No emergency contacts found. Please add contacts first.", Toast.LENGTH_LONG).show();
                        tvContactStatus.setText("No contacts");
                        Log.w(TAG, "No emergency contacts to send SMS to");
                        return;
                    }

                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                            != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.SEND_SMS},
                                PERMISSION_REQUEST_CODE);
                        return;
                    }

                    String message = "🚨 EMERGENCY SOS! I am in danger! Location: https://maps.google.com/?q=" + latitude + "," + longitude;
                    
                    SMSHelper.sendSMS(this, message, phoneNumbers, new SMSHelper.OnSMSStatusListener() {
                        @Override
                        public void onSmsSent(int successCount, int totalCount, List<String> failedPhones) {
                            successfulContacts = successCount;
                            failedContacts = failedPhones.size();
                            
                            tvContactCount.setText(String.valueOf(successCount));
                            tvContactStatus.setText(successCount + " / " + totalCount + " sent");
                            tvFailedCount.setText(failedContacts + " failed");
                            
                            Log.i(TAG, "SOS sent to " + successCount + " contacts");
                            if (!failedPhones.isEmpty()) {
                                Log.w(TAG, "Failed contacts: " + failedPhones);
                            }
                        }

                        @Override
                        public void onSmsError(String error) {
                            Log.e(TAG, "SMS Error: " + error);
                            tvContactStatus.setText("Send error");
                        }
                    });

                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load emergency contacts", e);
                    Toast.makeText(this, "Failed to load contacts: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void cancelSos() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        if (continuousPulseAnimator != null) {
            continuousPulseAnimator.cancel();
        }
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        Toast.makeText(this, "SOS Alert Cancelled", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (allGranted) {
                Log.d(TAG, "Permissions granted");
                getLastLocation();
                setupLocationUpdates();
            } else {
                Toast.makeText(this, "Permissions required to send SOS", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        if (continuousPulseAnimator != null) {
            continuousPulseAnimator.cancel();
        }
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}
