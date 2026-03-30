package com.example.aapraksha;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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
    
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FusedLocationProviderClient fusedLocationClient;
    private List<EmergencyContact> contactList = new ArrayList<>();
    private double latitude = 0.0;
    private double longitude = 0.0;
    private String currentSOSId;
    private Button btnCancel;
    private Button btnSendSMS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sos_triggered);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        btnCancel = findViewById(R.id.btn_cancel_sos);
        btnSendSMS = findViewById(R.id.btn_send_sms);

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> cancelSOS());
        }

        if (btnSendSMS != null) {
            btnSendSMS.setOnClickListener(v -> sendSmsToContacts());
        }

        checkPermissions();
        getCurrentLocation();
        loadEmergencyContacts();
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
                Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Some permissions denied. Features may not work.", Toast.LENGTH_LONG).show();
            }
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
                    } else {
                        Log.w(TAG, "Location is null");
                        latitude = 0.0;
                        longitude = 0.0;
                    }
                });
    }

    private void loadEmergencyContacts() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

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
                    Toast.makeText(this, "Failed to load contacts", Toast.LENGTH_SHORT).show();
                });
    }

    private void sendSmsToContacts() {
        if (contactList.isEmpty()) {
            Toast.makeText(this, "No emergency contacts to notify", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) 
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.SEND_SMS}, 200);
            return;
        }

        ArrayList<String> validPhones = new ArrayList<>();
        for (EmergencyContact contact : contactList) {
            String phone = contact.getPhone();
            if (phone != null && !phone.isEmpty() && SMSHelper.isValidIndianPhone(phone)) {
                validPhones.add(phone);
            }
        }

        if (validPhones.isEmpty()) {
            Toast.makeText(this, "No valid contact numbers", Toast.LENGTH_SHORT).show();
            return;
        }

        String message = "🚨 EMERGENCY SOS! I am in danger! Location: https://maps.google.com/?q=" + latitude + "," + longitude;
        
        SMSHelper smsHelper = new SMSHelper(this, new SMSHelper.OnSMSStatusListener() {
            @Override
            public void onSmsSent(int count, int total) {
                Log.i("SOS", "SMS sent to " + count + "/" + total);
            }

            @Override
            public void onSmsDelivered(int count, int total) {
                Log.i("SOS", "SMS delivered to " + count + "/" + total);
            }

            @Override
            public void onSmsError(String phone, String error) {
                Log.e("SOS", "Error sending to " + phone + ": " + error);
            }
        });

        smsHelper.sendSMS(message, validPhones);
        createSOSAlert();
    }

    private void createSOSAlert() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = auth.getCurrentUser().getUid();
        Map<String, Object> alert = new HashMap<>();
        alert.put("userId", uid);
        alert.put("timestamp", System.currentTimeMillis());
        alert.put("status", "active");
        alert.put("type", "SOS");
        alert.put("latitude", latitude);
        alert.put("longitude", longitude);

        db.collection("alerts")
                .add(alert)
                .addOnSuccessListener(doc -> {
                    currentSOSId = doc.getId();
                    Log.i(TAG, "SOS alert created with ID: " + doc.getId());
                    Toast.makeText(this, "🚨 SOS Alert Sent Successfully!", Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create SOS alert", e);
                    Toast.makeText(this, "Failed to create SOS alert. Please try again.", Toast.LENGTH_SHORT).show();
                });
    }

    private void cancelSOS() {
        if (currentSOSId != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("status", "cancelled");

            db.collection("alerts").document(currentSOSId)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "SOS cancelled", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Cancel failed", e);
                        Toast.makeText(this, "Failed to cancel SOS: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        cancelSOS();
    }
}
