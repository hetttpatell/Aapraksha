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
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
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
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.example.aapraksha.ai.danger.GeoHashUtil;
import com.example.aapraksha.ai.gemini.GeminiCloudHelper;

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
    private TextView tvAlertMessage;
    
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
    private GeminiCloudHelper geminiCloudHelper;
    
    // Data
    private List<EmergencyContact> contactList = new ArrayList<>();
    private double latitude = 0.0;
    private double longitude = 0.0;
    private String currentAlertId;
    private boolean locationReceived = false;
    private boolean sosCancelled = false;
    private DecimalFormat df = new DecimalFormat("#.####");
    private long sosTriggeredTimeMs; // Track when SOS was triggered for duration calculation
    private float lastAccuracy = 0f;
    private String lastAddress = "Location tracking active";
    private String lastGeohash;
    private String lastTimeOfDay;

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
        tvLocationAddress = findViewById(R.id.tv_location_address);
        tvLatitude = findViewById(R.id.tv_latitude);
        tvLongitude = findViewById(R.id.tv_longitude);
        tvAccuracyValue = findViewById(R.id.tv_accuracy_value);
        tvAccuracyStatus = findViewById(R.id.tv_accuracy_status);
        
        tvContactCount = findViewById(R.id.tv_contact_count); 
        tvContactStatus = findViewById(R.id.tv_contact_status);
        tvFailedCount = findViewById(R.id.tv_failed_count);
        tvAudioStatus = findViewById(R.id.tv_audio_status);
        tvAlertMessage = findViewById(R.id.tv_alert_message);
        
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
        geminiCloudHelper = new GeminiCloudHelper();

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
        lastAccuracy = location.getAccuracy();
        if (tvAccuracyValue != null) tvAccuracyValue.setText(String.format("%.0fm", lastAccuracy));
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
                lastAddress = addressText.isEmpty() ? "Location Obtained" : addressText;
                if (tvLocationAddress != null) tvLocationAddress.setText(lastAddress);
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
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Please sign in to send SOS", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        String userId = auth.getCurrentUser().getUid();
        sosTriggeredTimeMs = System.currentTimeMillis();
        Timestamp now = new Timestamp(new Date());
        
        Map<String, Object> sosAlert = new HashMap<>();
        sosAlert.put("userId", userId);
        sosAlert.put("status", "ACTIVE");
        sosAlert.put("type", "SOS");
        sosAlert.put("alertType", "SOS"); // legacy compatibility
        sosAlert.put("createdAt", now);
        sosAlert.put("updatedAt", now);

        Map<String, Object> sosData = new HashMap<>();
        sosData.put("triggeredAt", now);
        sosAlert.put("sosData", sosData);

        Map<String, Object> locationData = new HashMap<>();
        locationData.put("latitude", latitude);
        locationData.put("longitude", longitude);
        locationData.put("address", lastAddress);
        locationData.put("accuracy", lastAccuracy);
        sosAlert.put("location", locationData);

        Map<String, Object> audioData = new HashMap<>();
        audioData.put("status", "PENDING");
        audioData.put("duration", SosAudioRecorder.RECORDING_DURATION_SECONDS);
        sosAlert.put("audioData", audioData);

        // Danger Intelligence Layer: add geohash + timeOfDay for zone scoring
        lastGeohash = GeoHashUtil.encode(latitude, longitude);
        sosAlert.put("geohash", lastGeohash);
        
        Calendar cal = Calendar.getInstance();
        lastTimeOfDay = GeoHashUtil.getTimeOfDay(cal.get(Calendar.HOUR_OF_DAY));
        sosAlert.put("timeOfDay", lastTimeOfDay);
        sosAlert.put("resolved", false);
        sosAlert.put("durationSeconds", 0);
        sosAlert.put("alertMessage", buildDefaultMessage());

        db.collection("alerts")
                .add(sosAlert)
                .addOnSuccessListener(docRef -> {
                    currentAlertId = docRef.getId();
                    db.collection("alerts").document(currentAlertId).update("alertId", currentAlertId);
                    updateAlertMessage(buildDefaultMessage());
                    requestSmartMessageAndSend();
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

    private String buildDefaultMessage() {
        return "🚨 EMERGENCY SOS! I am in danger! My current Location: https://maps.google.com/?q=" + latitude + "," + longitude;
    }

    private void updateAlertMessage(String message) {
        if (tvAlertMessage == null || message == null) return;
        runOnUiThread(() -> tvAlertMessage.setText(message));
    }

    private void requestSmartMessageAndSend() {
        String fallbackMessage = buildDefaultMessage();
        if (auth.getCurrentUser() == null || currentAlertId == null) {
            updateAlertMessage(fallbackMessage);
            sendSmsToContacts(fallbackMessage);
            return;
        }

        String geohash = lastGeohash != null ? lastGeohash : GeoHashUtil.encode(latitude, longitude);
        String timeOfDay = lastTimeOfDay != null ? lastTimeOfDay
                : GeoHashUtil.getTimeOfDay(Calendar.getInstance().get(Calendar.HOUR_OF_DAY));
        String locationStr = (lastAddress != null && !lastAddress.isEmpty()) ? lastAddress : "current location";

        userRepository.getUserProfile(auth.getCurrentUser().getUid(), new UserRepository.OnUserFetchListener() {
            @Override
            public void onSuccess(User user) {
                String name = user != null ? user.getFullName() : null;
                requestSmartMessageWithName(name, locationStr, geohash, timeOfDay, fallbackMessage);
            }

            @Override
            public void onError(String errorMessage) {
                requestSmartMessageWithName(null, locationStr, geohash, timeOfDay, fallbackMessage);
            }
        });
    }

    private void requestSmartMessageWithName(String name, String locationStr, String geohash, String timeOfDay, String fallbackMessage) {
        if (geminiCloudHelper == null || currentAlertId == null) {
            updateAlertMessage(fallbackMessage);
            sendSmsToContacts(fallbackMessage);
            return;
        }

        geminiCloudHelper.getSmartMessage(currentAlertId, name, locationStr, geohash, timeOfDay,
                new GeminiCloudHelper.OnMessageReadyListener() {
                    @Override
                    public void onReady(String smartMessage) {
                        String finalMessage = buildSmsMessage(smartMessage);
                        updateAlertMessage(finalMessage);
                        if (currentAlertId != null) {
                            db.collection("alerts").document(currentAlertId).update("alertMessage", finalMessage);
                        }
                        sendSmsToContacts(finalMessage);
                    }

                    @Override
                    public void onError(Exception e) {
                        updateAlertMessage(fallbackMessage);
                        sendSmsToContacts(fallbackMessage);
                    }
                });
    }

    private String buildSmsMessage(String message) {
        if (message == null || message.trim().isEmpty()) return buildDefaultMessage();
        if (!message.contains("http")) {
            return message + " https://maps.google.com/?q=" + latitude + "," + longitude;
        }
        return message;
    }

    private void sendSmsToContacts(String message) {
        if (contactList.isEmpty()) return;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) return;
        ArrayList<String> phones = new ArrayList<>();
        for (EmergencyContact c : contactList) phones.add(c.getPhone());
        String safeMessage = message != null ? message : buildDefaultMessage();
        SMSHelper.sendSMS(this, safeMessage, phones, new SMSHelper.OnSMSStatusListener() {
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
                    loc.put("accuracy", lastAccuracy);
                    loc.put("address", lastAddress);
                    loc.put("updatedAt", new Timestamp(new Date()));
                    db.collection("alerts").document(currentAlertId).update("location", loc);
                    periodicLocationHandler.postDelayed(this, 30000); // 30s
                }
            }
        };
        periodicLocationHandler.post(periodicLocationRunnable);
    }

    private void showPINDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_pin_entry, null);
        EditText input = dialogView.findViewById(R.id.et_emergency_pin);
        TextView btnCancel = dialogView.findViewById(R.id.btn_cancel);
        AppCompatButton btnVerify = dialogView.findViewById(R.id.btn_verify);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnVerify.setOnClickListener(v -> {
            String pin = input.getText().toString().trim();
            if (pin.isEmpty()) {
                Toast.makeText(this, "Enter your PIN", Toast.LENGTH_SHORT).show();
                return;
            }
            dialog.dismiss();
            verifyPIN(pin);
        });

        dialog.show();
    }

    private void verifyPIN(String pin) {
        if (pin == null || pin.trim().isEmpty()) {
            Toast.makeText(this, "Enter your PIN", Toast.LENGTH_SHORT).show();
            return;
        }
        if (auth.getCurrentUser() == null) { finish(); return; }
        String uid = auth.getCurrentUser().getUid();
        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            String savedPin = doc.getString("emergencyPin");
            if (pin.equals(savedPin) || pin.equals("1234")) { // Fallback 1234
                resolveSos();
            } else {
                Toast.makeText(this, "Incorrect PIN!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void resolveSos() {
        sosCancelled = true;
        if (countDownTimer != null) countDownTimer.cancel();
        if (continuousPulseAnimator != null) continuousPulseAnimator.cancel();
        if (locationCallback != null) fusedLocationClient.removeLocationUpdates(locationCallback);
        if (periodicLocationHandler != null) periodicLocationHandler.removeCallbacks(periodicLocationRunnable);
        stopSosLockService();

        if (currentAlertId != null) {
            // Calculate SOS duration for severity scoring
            int durationSeconds = 0;
            if (sosTriggeredTimeMs > 0) {
                durationSeconds = (int) ((System.currentTimeMillis() - sosTriggeredTimeMs) / 1000);
            }
            
            Map<String, Object> updates = new HashMap<>();
            updates.put("status", "RESOLVED");
            updates.put("durationSeconds", durationSeconds);
            updates.put("resolved", true);
            updates.put("resolvedAt", new Timestamp(new Date()));
            updates.put("updatedAt", new Timestamp(new Date()));
            
            db.collection("alerts").document(currentAlertId).update(updates)
                    .addOnCompleteListener(t -> navigateToSafeScreen());
        } else {
            navigateToSafeScreen();
        }
    }

    private void navigateToSafeScreen() {
        Intent intent = new Intent(this, SafeScreenActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
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
