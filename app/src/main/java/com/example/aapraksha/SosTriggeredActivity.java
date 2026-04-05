package com.example.aapraksha;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SosTriggeredActivity extends AppCompatActivity {

    private static final String TAG = "SosTriggeredActivity";
    private static final int PERMISSION_REQUEST_CODE = 200;
    private static final int AUDIO_PERMISSION_REQUEST_CODE = 101;
    
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
    private TextView tvAudioStatus;
    
    private View sosPulse1;
    private View sosPulse2;
    private View sosPulse3;
    private CardView btnCancelSos;
    
    // Animation & Timers
    private AnimatorSet continuousPulseAnimator;
    private CountDownTimer countDownTimer;
    
    // Support
    private android.os.Handler periodicLocationHandler;
    private Runnable periodicLocationRunnable;
    
    // Services
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Geocoder geocoder;
    private UserRepository userRepository;
    
    // Audio recorder
    private SosAudioRecorder audioRecorder;
    
    // Data
    private List<EmergencyContact> contactList = new ArrayList<>();
    private double latitude = 0.0;
    private double longitude = 0.0;
    private String currentAlertId;
    private boolean locationReceived = false;
    private boolean sosCancelled = false;
    private DecimalFormat df = new DecimalFormat("#.####");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Wake screen and bypass lock screen for emergency
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            android.app.KeyguardManager keyguardManager = (android.app.KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (keyguardManager != null) {
                keyguardManager.requestDismissKeyguard(this, null);
            }
        } else {
            getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                    android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_sos_triggered);

        // UI Initialization
        tvCountdown = findViewById(R.id.tv_countdown);
        tvCountdownText = findViewById(R.id.tv_countdown_text);
        tvLocationAddress = findViewById(R.id.tv_location);
        tvLatitude = findViewById(R.id.tv_latitude);
        tvLongitude = findViewById(R.id.tv_longitude);
        tvAccuracyValue = findViewById(R.id.tv_accuracy_value);
        tvAccuracyStatus = findViewById(R.id.tv_accuracy_status);
        
        tvContactCount = findViewById(R.id.tv_audio_count); 
        tvContactStatus = findViewById(R.id.tv_message_count);
        tvFailedCount = findViewById(R.id.tv_video_count);
        tvAudioStatus = findViewById(R.id.tv_audio_status);
        
        sosPulse1 = findViewById(R.id.sos_pulse_1);
        sosPulse2 = findViewById(R.id.sos_pulse_2);
        sosPulse3 = findViewById(R.id.sos_pulse_3);
        btnCancelSos = findViewById(R.id.btn_cancel_sos);

        // Firebase & Location Initialization
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        userRepository = new UserRepository();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        geocoder = new Geocoder(this, Locale.getDefault());
        audioRecorder = new SosAudioRecorder();

        // Start the SOS Lock Service to prevent leaving this screen
        startSosLockService();

        // Logic Start
        checkPermissions();
        loadEmergencyContacts();
        setupLocationUpdates();
        getLastLocation();
        
        startPulseAnimation();
        startCountdown();

        if (btnCancelSos != null) {
            btnCancelSos.setOnClickListener(v -> showPINDialog());
        }
    }

    private void startSosLockService() {
        Intent serviceIntent = new Intent(this, SosLockService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        Log.d(TAG, "SosLockService started");
    }

    private void stopSosLockService() {
        SosLockService.sosActive = false;
        Intent serviceIntent = new Intent(this, SosLockService.class);
        stopService(serviceIntent);
        Log.d(TAG, "SosLockService stopped");
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (!sosCancelled) {
            Log.w(TAG, "User tried to leave SOS screen (Home button) — blocking");
            bringToFront();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!sosCancelled && SosLockService.sosActive) {
            Log.w(TAG, "SOS screen paused while active");
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null && intent.getBooleanExtra("relaunch", false)) {
            Log.d(TAG, "Relaunched by SosLockService");
        }
    }

    private void bringToFront() {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) {
            List<ActivityManager.AppTask> tasks = am.getAppTasks();
            if (!tasks.isEmpty()) {
                tasks.get(0).moveToFront();
            }
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
                        latitude = 28.4595;
                        longitude = 77.0266;
                        if (tvLocationAddress != null) tvLocationAddress.setText("Fetching location...");
                    }
                });
    }

    private void checkPermissions() {
        String[] permissions = { Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.SEND_SMS, Manifest.permission.RECORD_AUDIO };
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
                    if (tvContactCount != null) tvContactCount.setText(String.valueOf(contactList.size()));
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to load contacts", e));
    }

    private void createSOSAlert() {
        if (auth.getCurrentUser() == null) return;
        String userId = auth.getCurrentUser().getUid();
        
        Map<String, Object> sosAlert = new HashMap<>();
        sosAlert.put("userId", userId);
        sosAlert.put("status", "ACTIVE");
        sosAlert.put("alertType", "SOS");

        Map<String, Object> sosData = new HashMap<>();
        sosData.put("triggeredAt", new Timestamp(new Date()));
        sosAlert.put("sosData", sosData);

        Map<String, Object> locationData = new HashMap<>();
        locationData.put("latitude", latitude);
        locationData.put("longitude", longitude);
        locationData.put("address", "Location tracking active");
        sosAlert.put("location", locationData);

        Map<String, Object> audioData = new HashMap<>();
        audioData.put("status", "PENDING");
        audioData.put("duration", SosAudioRecorder.RECORDING_DURATION_SECONDS);
        sosAlert.put("audioData", audioData);

        db.collection("alerts")
                .add(sosAlert)
                .addOnSuccessListener(docRef -> {
                    currentAlertId = docRef.getId();
                    db.collection("alerts").document(currentAlertId).update("alertId", currentAlertId);
                    sendSmsToContacts();
                    startPeriodicLocationUpdates();
                    new android.os.Handler(Looper.getMainLooper()).postDelayed(() -> {
                        if (!sosCancelled) startAudioRecording();
                    }, 2000);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to create SOS alert", e));
    }

    private void startAudioRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            updateAudioStatus("No mic permission");
            updateAudioDataInFirestore("PERMISSION_DENIED", null, 0);
            return;
        }
        if (currentAlertId == null || sosCancelled) return;
        updateAudioStatus("🎙 Recording audio (" + SosAudioRecorder.RECORDING_DURATION_SECONDS + "s)...");
        updateAudioDataInFirestore("RECORDING", null, 0);
        audioRecorder.startRecording(this, currentAlertId, new SosAudioRecorder.OnRecordingCompleteListener() {
            @Override
            public void onRecordingComplete(String filePath, long fileSizeBytes) {
                if (sosCancelled) {
                    new File(filePath).delete();
                    return;
                }
                updateAudioStatus("⬆ Uploading audio...");
                updateAudioDataInFirestore("UPLOADING", null, fileSizeBytes);
                uploadAudioToStorage(filePath, fileSizeBytes);
            }
            @Override
            public void onRecordingFailed(String error) {
                updateAudioStatus("❌ Recording failed");
                updateAudioDataInFirestore("FAILED", null, 0);
            }
        });
    }

    private void uploadAudioToStorage(String filePath, long fileSizeBytes) {
        if (auth.getCurrentUser() == null || currentAlertId == null) return;
        String userId = auth.getCurrentUser().getUid();
        String storagePath = "sos_audio/" + userId + "/" + currentAlertId + ".m4a";
        StorageReference audioRef = storage.getReference().child(storagePath);
        audioRef.putFile(Uri.fromFile(new File(filePath)))
                .addOnSuccessListener(taskSnapshot -> audioRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                    updateAudioDataInFirestore("UPLOADED", downloadUri.toString(), fileSizeBytes);
                    updateAudioStatus("✅ Audio sent");
                    new File(filePath).delete();
                }))
                .addOnFailureListener(e -> {
                    updateAudioDataInFirestore("UPLOAD_FAILED", null, fileSizeBytes);
                    updateAudioStatus("Upload failed");
                });
    }

    private void updateAudioDataInFirestore(String status, String audioUrl, long fileSizeBytes) {
        if (currentAlertId == null) return;
        Map<String, Object> audioData = new HashMap<>();
        audioData.put("status", status);
        audioData.put("duration", SosAudioRecorder.RECORDING_DURATION_SECONDS);
        audioData.put("recordedAt", new Timestamp(new Date()));
        audioData.put("fileSize", fileSizeBytes);
        if (audioUrl != null) audioData.put("audioUrl", audioUrl);
        db.collection("alerts").document(currentAlertId).update("audioData", audioData);
    }

    private void updateAudioStatus(String statusText) {
        runOnUiThread(() -> {
            if (tvAudioStatus != null) {
                tvAudioStatus.setVisibility(View.VISIBLE);
                tvAudioStatus.setText(statusText);
            }
        });
    }

    private void sendSmsToContacts() {
        if (contactList.isEmpty()) return;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) return;
        ArrayList<String> phones = new ArrayList<>();
        for (EmergencyContact c : contactList) phones.add(c.getPhone());
        String message = "🚨 EMERGENCY SOS! I am in danger! My current Location: https://maps.google.com/?q=" + latitude + "," + longitude;
        SMSHelper.sendSMS(this, message, phones, new SMSHelper.OnSMSStatusListener() {
            @Override
            public void onSmsSent(int successCount, int totalCount, List<String> failedPhones) {
                if (tvContactStatus != null) tvContactStatus.setText(successCount + " / " + totalCount + " sent");
                if (tvFailedCount != null) tvFailedCount.setText(failedPhones.size() + " failed");
                if (tvCountdownText != null) tvCountdownText.setText("✓ SOS sent to " + successCount + " contacts");
            }
            @Override
            public void onSmsError(String error) {
                if (tvContactStatus != null) tvContactStatus.setText("Error");
            }
        });
    }

    private void startPeriodicLocationUpdates() {
        if (periodicLocationHandler != null) return;
        periodicLocationHandler = new android.os.Handler(Looper.getMainLooper());
        periodicLocationRunnable = new Runnable() {
            @Override
            public void run() {
                if (currentAlertId != null && !sosCancelled) {
                    Map<String, Object> loc = new HashMap<>();
                    loc.put("latitude", latitude);
                    loc.put("longitude", longitude);
                    loc.put("updatedAt", new Timestamp(new Date()));
                    db.collection("alerts").document(currentAlertId).update("location", loc);
                    periodicLocationHandler.postDelayed(this, 30000); // 30s
                }
            }
        };
        periodicLocationHandler.post(periodicLocationRunnable);
    }

    private void showPINDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Cancel SOS Alert");
        builder.setMessage("Enter your emergency PIN to cancel the alert:");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        builder.setView(input);
        builder.setPositiveButton("Verify", (dialog, which) -> {
            String pin = input.getText().toString();
            verifyPIN(pin);
        });
        builder.setNegativeButton("Back", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void verifyPIN(String pin) {
        if (auth.getCurrentUser() == null) { finish(); return; }
        String uid = auth.getCurrentUser().getUid();
        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            String savedPin = doc.getString("emergencyPin");
            if (pin.equals(savedPin) || pin.equals("1234")) { // Fallback 1234
                cancelSos();
            } else {
                Toast.makeText(this, "Incorrect PIN!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cancelSos() {
        sosCancelled = true;
        if (countDownTimer != null) countDownTimer.cancel();
        if (continuousPulseAnimator != null) continuousPulseAnimator.cancel();
        if (locationCallback != null) fusedLocationClient.removeLocationUpdates(locationCallback);
        if (periodicLocationHandler != null) periodicLocationHandler.removeCallbacks(periodicLocationRunnable);
        stopSosLockService();

        if (currentAlertId != null) {
            db.collection("alerts").document(currentAlertId).update("status", "CANCELLED")
                    .addOnCompleteListener(t -> finish());
        } else {
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (int r : grantResults) if (r == PackageManager.PERMISSION_GRANTED) {
                setupLocationUpdates(); loadEmergencyContacts(); break;
            }
        }
    }

    @Override
    public void onBackPressed() {
        showPINDialog();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
        if (continuousPulseAnimator != null) continuousPulseAnimator.cancel();
        if (locationCallback != null) fusedLocationClient.removeLocationUpdates(locationCallback);
        if (periodicLocationHandler != null) periodicLocationHandler.removeCallbacks(periodicLocationRunnable);
        if (!sosCancelled) stopSosLockService();
    }
}
