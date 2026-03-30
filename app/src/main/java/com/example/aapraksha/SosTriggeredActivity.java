package com.example.aapraksha;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.CountDownTimer;
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
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SosTriggeredActivity extends AppCompatActivity {

    private static final String TAG = "SosTriggeredActivity";
    private static final int PERMISSION_REQUEST_CODE = 200;
    
    private TextView tvCountdown;
    private TextView tvCountdownText;
    private TextView tvLocation;
    private View sosPulse1;
    private View sosPulse2;
    private View sosPulse3;
    private CardView btnCancelSos;
    
    private AnimatorSet continuousPulseAnimator;
    private CountDownTimer countDownTimer;
    
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FusedLocationProviderClient fusedLocationClient;
    
    private List<EmergencyContact> contactList = new ArrayList<>();
    private double latitude = 0.0;
    private double longitude = 0.0;
    private String currentAlertId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sos_triggered);

        // UI Initialization
        tvCountdown = findViewById(R.id.tv_countdown);
        tvCountdownText = findViewById(R.id.tv_countdown_text);
        tvLocation = findViewById(R.id.tv_location);
        sosPulse1 = findViewById(R.id.sos_pulse_1);
        sosPulse2 = findViewById(R.id.sos_pulse_2);
        sosPulse3 = findViewById(R.id.sos_pulse_3);
        btnCancelSos = findViewById(R.id.btn_cancel_sos);

        // Firebase Initialization
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Load data and start logic
        checkPermissions();
        getCurrentLocation();
        loadEmergencyContacts();
        
        startPulseAnimation();
        startCountdown();

        if (btnCancelSos != null) {
            btnCancelSos.setOnClickListener(v -> cancelSos());
        }
    }

    private void startPulseAnimation() {
        continuousPulseAnimator = new AnimatorSet();
        
        AnimatorSet anim1 = createPulseAnimator(sosPulse1, 0);
        AnimatorSet anim2 = createPulseAnimator(sosPulse2, 600);
        AnimatorSet anim3 = createPulseAnimator(sosPulse3, 1200);
        
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
                if (tvCountdownText != null) tvCountdownText.setText("SOS Alert Sent to Emergency Contacts!");
                Toast.makeText(SosTriggeredActivity.this, "🚨 Emergency Alert Sent Successfully!", Toast.LENGTH_LONG).show();
                
                // Create SOS alert and send SMS
                createSOSAlert();
            }
        }.start();
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

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                        Log.d(TAG, "Location obtained: " + latitude + ", " + longitude);
                        if (tvLocation != null) {
                            tvLocation.setText(String.format("%.4f, %.4f", latitude, longitude));
                        }
                    } else {
                        Log.w(TAG, "Location is null, using fallback");
                        latitude = 28.4595; // Gurgaon Fallback
                        longitude = 77.0266;
                    }
                });
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
                    Log.d(TAG, "Loaded " + contactList.size() + " emergency contacts");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load contacts", e);
                });
    }

    private void createSOSAlert() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

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
                    Log.d(TAG, "SOS Alert created: " + currentAlertId);
                    sendSmsToContacts();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create SOS alert", e);
                    Toast.makeText(this, "Failed to create alert: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void sendSmsToContacts() {
        if (contactList.isEmpty()) {
            Log.w(TAG, "No emergency contacts to notify");
            return;
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) 
            != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "No SMS permission to send alert");
            return;
        }

        ArrayList<String> validPhones = new ArrayList<>();
        for (EmergencyContact contact : contactList) {
            String phone = contact.getPhone();
            if (phone != null && !phone.isEmpty()) {
                validPhones.add(phone);
            }
        }

        if (validPhones.isEmpty()) return;

        String message = "🚨 EMERGENCY SOS! I am in danger! My current Location: https://maps.google.com/?q=" + latitude + "," + longitude;
        
        SMSHelper smsHelper = new SMSHelper(this, new SMSHelper.OnSMSStatusListener() {
            @Override
            public void onSmsSent(int count, int total) {
                Log.i(TAG, "SMS sent to " + count + "/" + total);
                if (tvCountdownText != null) tvCountdownText.setText("✓ SOS sent to " + count + " contacts");
            }

            @Override
            public void onSmsDelivered(int count, int total) {
                Log.i(TAG, "SMS delivered to " + count + "/" + total);
            }

            @Override
            public void onSmsError(String phone, String error) {
                Log.e(TAG, "Error sending to " + phone + ": " + error);
            }
        });

        smsHelper.sendSMS(message, validPhones);
    }

    private void cancelSos() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        if (continuousPulseAnimator != null) {
            continuousPulseAnimator.cancel();
        }

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
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                getCurrentLocation();
                loadEmergencyContacts();
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
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        if (continuousPulseAnimator != null) {
            continuousPulseAnimator.cancel();
        }
    }
}
