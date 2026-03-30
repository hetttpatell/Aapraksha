package com.example.aapraksha;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.example.aapraksha.models.User;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class UserDashboardActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String TAG = "UserDashboard";
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FusedLocationProviderClient fusedLocationClient;
    private String userName = "";
    private String userPhone = "";
    private List<EmergencyContact> contactList = new ArrayList<>();
    private double currentLat = 0.0;
    private double currentLng = 0.0;
    private UserRepository userRepository;

    // Location history
    private Handler locationHandler = new Handler();
    private Runnable locationRunnable;

    // SOS cancellation
    private String currentSOSId = null;
    private Handler cancelHandler = new Handler();
    private Runnable autoCancelRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard); // Using existing dashboard layout

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        userRepository = new UserRepository();

        setupClickListeners();
        checkAccessibilityPermission();
        fetchUserData();
        loadEmergencyContacts();
        checkPermissions();
    }

    private void checkAccessibilityPermission() {
        // Notify user to enable VolumeButtonService in accessibility settings
        Toast.makeText(this, "Enable VolumeButtonService in Accessibility Settings for SOS", Toast.LENGTH_LONG).show();
    }

    private void setupClickListeners() {
        // Profile button
        View btnProfile = findViewById(R.id.btn_profile);
        if (btnProfile != null) {
            btnProfile.setOnClickListener(v -> {
                Intent intent = new Intent(UserDashboardActivity.this, ProfileActivity.class);
                startActivity(intent);
            });
        }

        // Nav settings
        View navSettings = findViewById(R.id.nav_settings);
        if (navSettings != null) {
            navSettings.setOnClickListener(v -> {
                Intent intent = new Intent(UserDashboardActivity.this, ProfileActivity.class);
                startActivity(intent);
            });
        }
    }

    private void checkPermissions() {
        String[] permissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.SEND_SMS,
                Manifest.permission.CALL_PHONE
        };
        List<String> missing = new ArrayList<>();
        for (String p : permissions) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                missing.add(p);
            }
        }
        if (!missing.isEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        } else {
            getLastLocation();
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
                getLastLocation();
                Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Some permissions denied. Features may not work.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        currentLat = location.getLatitude();
                        currentLng = location.getLongitude();
                        Log.d(TAG, "Location updated: " + currentLat + ", " + currentLng);
                        startLocationUpdates();
                    }
                });
    }

    // ========== Location History Methods ==========
    private void startLocationUpdates() {
        if (locationRunnable != null) {
            locationHandler.removeCallbacks(locationRunnable);
        }
        locationRunnable = () -> {
            saveCurrentLocation();
            locationHandler.postDelayed(this.locationRunnable, 5 * 60 * 1000);
        };
        locationHandler.post(locationRunnable);
        Log.d(TAG, "Location updates started (every 5 minutes)");
    }

    private void saveCurrentLocation() {
        if (auth.getCurrentUser() == null) {
            Log.e(TAG, "User not logged in, cannot save location");
            return;
        }
        if (currentLat == 0.0 && currentLng == 0.0) {
            Log.d(TAG, "Location not available yet, skipping save");
            return;
        }
        String uid = auth.getCurrentUser().getUid();
        LocationHistoryActivity.LocationPoint point = new LocationHistoryActivity.LocationPoint(currentLat, currentLng, System.currentTimeMillis());
        db.collection("users").document(uid)
                .collection("locations")
                .add(point)
                .addOnSuccessListener(doc -> Log.d(TAG, "Location saved at " + new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(point.getTimestamp()))))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save location", e));
    }

    // ========== User Data ==========
    private void fetchUserData() {
        if (auth.getCurrentUser() == null) return;

        String uid = auth.getCurrentUser().getUid();
        userRepository.getUserProfile(uid, new UserRepository.OnUserFetchListener() {
            @Override
            public void onSuccess(User user) {
                if (user != null) {
                    userName = user.getFullName();
                    userPhone = user.getPhone();
                }
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Failed to load user: " + errorMessage);
            }
        });
    }

    // ========== SOS Management ==========
    private void showSOSConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Create Alert")
                .setMessage("Do you want to create an SOS alert?")
                .setPositiveButton("Yes", (dialog, which) -> sendSOS())
                .setNegativeButton("No", null)
                .show();
    }

    private void sendSOS() {
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
        alert.put("userName", userName);
        alert.put("latitude", currentLat);
        alert.put("longitude", currentLng);

        db.collection("alerts")
                .add(alert)
                .addOnSuccessListener(doc -> {
                    currentSOSId = doc.getId();

                    // Auto-cancel after 5 minutes
                    autoCancelRunnable = () -> {
                        if (currentSOSId != null) {
                            db.collection("alerts").document(currentSOSId).get()
                                    .addOnSuccessListener(snapshot -> {
                                        if ("active".equals(snapshot.getString("status"))) {
                                            cancelSOS();
                                        }
                                    });
                        }
                    };
                    cancelHandler.postDelayed(autoCancelRunnable, 5 * 60 * 1000);

                    Log.i(TAG, "SOS alert created with ID: " + doc.getId());
                    Toast.makeText(this, "🚨 SOS Alert Sent Successfully!", Toast.LENGTH_LONG).show();
                    sendSmsToContacts();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create SOS alert", e);
                    Toast.makeText(this, "Failed to send SOS alert. Please try again.", Toast.LENGTH_SHORT).show();
                });
    }

    private void cancelSOS() {
        if (currentSOSId == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "cancelled");

        db.collection("alerts").document(currentSOSId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "SOS cancelled", Toast.LENGTH_SHORT).show();
                    if (autoCancelRunnable != null) {
                        cancelHandler.removeCallbacks(autoCancelRunnable);
                    }
                    currentSOSId = null;
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Cancel failed", e);
                    Toast.makeText(this, "Failed to cancel SOS: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void sendSmsToContacts() {
        if (contactList.isEmpty()) {
            Toast.makeText(this, "No emergency contacts to notify", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) 
            != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "SMS permission not granted", Toast.LENGTH_SHORT).show();
            return;
        }

        // Collect phone numbers and validate
        List<String> validPhones = new ArrayList<>();
        for (EmergencyContact contact : contactList) {
            String phone = contact.getPhone();
            if (phone != null && !phone.isEmpty()) {
                if (SMSHelper.isValidIndianPhone(phone)) {
                    validPhones.add(phone);
                } else {
                    Log.w(TAG, "Invalid phone format for contact: " + contact.getName() + " - " + phone);
                    Toast.makeText(this, "Invalid phone format for: " + contact.getName(), Toast.LENGTH_SHORT).show();
                }
            }
        }

        if (validPhones.isEmpty()) {
            Toast.makeText(this, "No valid contact numbers found", Toast.LENGTH_SHORT).show();
            return;
        }

        String message = "🚨 EMERGENCY SOS! I am in danger! My location: https://maps.google.com/?q=" + currentLat + "," + currentLng;
        
        // Use static method from SMSHelper
        SMSHelper.sendSMS(this, message, validPhones, new SMSHelper.OnSMSStatusListener() {
            @Override
            public void onSmsSent(int successCount, int totalCount, List<String> failedPhones) {
                String msg = successCount > 0 ? 
                    "SOS alert sent to " + successCount + "/" + totalCount + " contacts" : 
                    "Failed to send SOS to any contact";
                Toast.makeText(UserDashboardActivity.this, msg, Toast.LENGTH_LONG).show();
                Log.i(TAG, msg);
                
                if (!failedPhones.isEmpty()) {
                    Log.w(TAG, "Failed to send to: " + failedPhones);
                }
            }

            @Override
            public void onSmsError(String error) {
                Toast.makeText(UserDashboardActivity.this, "SMS Error: " + error, Toast.LENGTH_LONG).show();
                Log.e(TAG, "SMS error: " + error);
            }
        });
    }

    // ========== Emergency Contacts Management ==========
    private void loadEmergencyContacts() {
        if (auth.getCurrentUser() == null) return;

        String uid = auth.getCurrentUser().getUid();
        CollectionReference contactsRef = db.collection("users").document(uid)
                .collection("emergencyContacts");

        contactsRef.orderBy("name", Query.Direction.ASCENDING)
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
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load contacts", Toast.LENGTH_SHORT).show()
                );
    }

    private void showAddContactDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Emergency Contact");

        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_contact, null);
        EditText etName = dialogView.findViewById(R.id.edit_contact_name);
        EditText etPhone = dialogView.findViewById(R.id.edit_contact_phone);
        EditText etRelation = dialogView.findViewById(R.id.edit_contact_relation);

        builder.setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String phone = etPhone.getText().toString().trim();
                    String relation = etRelation.getText().toString().trim();

                    if (TextUtils.isEmpty(name) || TextUtils.isEmpty(phone)) {
                        Toast.makeText(this, "Name and phone are required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // **KEY VALIDATION: User cannot add their own phone number**
                    if (!TextUtils.isEmpty(userPhone) && 
                        phone.replaceAll("[^0-9]", "").endsWith(userPhone.replaceAll("[^0-9]", ""))) {
                        Toast.makeText(this, "You cannot add your own phone number as emergency contact", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    addEmergencyContact(name, phone, relation);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void addEmergencyContact(String name, String phone, String relation) {
        if (auth.getCurrentUser() == null) return;

        String uid = auth.getCurrentUser().getUid();
        CollectionReference contactsRef = db.collection("users").document(uid)
                .collection("emergencyContacts");

        Map<String, Object> contact = new HashMap<>();
        contact.put("name", name);
        contact.put("phone", phone);
        contact.put("relation", relation);
        contact.put("timestamp", System.currentTimeMillis());

        contactsRef.add(contact)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Contact added", Toast.LENGTH_SHORT).show();
                    loadEmergencyContacts();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to add contact", Toast.LENGTH_SHORT).show()
                );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationHandler != null && locationRunnable != null) {
            locationHandler.removeCallbacks(locationRunnable);
        }
        if (cancelHandler != null && autoCancelRunnable != null) {
            cancelHandler.removeCallbacks(autoCancelRunnable);
        }
    }
}
