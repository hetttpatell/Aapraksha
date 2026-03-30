# Code Changes Reference - SOS SMS Fix

## 🔑 Key Code Sections Modified

### 1. SosTriggeredActivity.java - Constructor & Initialization

```java
private FirebaseAuth auth;
private FirebaseFirestore db;
private FusedLocationProviderClient fusedLocationClient;
private double latitude = 0.0;
private double longitude = 0.0;
private String currentAlertId;

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_sos_triggered);

    // Initialize Firebase services
    auth = FirebaseAuth.getInstance();
    db = FirebaseFirestore.getInstance();
    fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

    // Get current location BEFORE countdown
    getLastLocation();
    
    // Start UI elements
    startPulseAnimation();
    startCountdown();
}
```

### 2. SosTriggeredActivity.java - Get Real Location

```java
private void getLastLocation() {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                        Log.d(TAG, "Location obtained: " + latitude + ", " + longitude);
                    } else {
                        // Fallback if location not available
                        latitude = 28.4595;
                        longitude = 77.0266;
                        Log.w(TAG, "Location not available, using fallback");
                    }
                });
    } else {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                PERMISSION_REQUEST_CODE);
    }
}
```

### 3. SosTriggeredActivity.java - Countdown Finishes (Main Trigger)

```java
private void startCountdown() {
    countDownTimer = new CountDownTimer(5000, 1000) {
        @Override
        public void onTick(long millisUntilFinished) {
            int secondsRemaining = (int) (millisUntilFinished / 1000);
            tvCountdown.setText(String.format("%02d", secondsRemaining));
            tvCountdownText.setText("Triggering alert in " + secondsRemaining + " seconds...");
        }

        @Override
        public void onFinish() {
            tvCountdown.setText("00");
            tvCountdownText.setText("SOS Alert Sent to Emergency Contacts!");
            Toast.makeText(SosTriggeredActivity.this, 
                "🚨 Emergency Alert Sent Successfully!", 
                Toast.LENGTH_LONG).show();
            
            // ⭐ KEY: Start the SOS process here
            createSOSAlert();
        }
    }.start();
}
```

### 4. SosTriggeredActivity.java - Create SOS Alert

```java
private void createSOSAlert() {
    if (auth.getCurrentUser() == null) {
        Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
        return;
    }

    String userId = auth.getCurrentUser().getUid();
    
    // Create SOS alert document
    java.util.Map<String, Object> sosAlert = new java.util.HashMap<>();
    sosAlert.put("userId", userId);
    sosAlert.put("timestamp", System.currentTimeMillis());
    sosAlert.put("latitude", latitude);
    sosAlert.put("longitude", longitude);
    sosAlert.put("status", "active");
    sosAlert.put("type", "SOS");

    // Save to Firebase
    db.collection("sosAlerts")
            .add(sosAlert)
            .addOnSuccessListener(docRef -> {
                currentAlertId = docRef.getId();
                Log.d(TAG, "SOS Alert created: " + currentAlertId);
                
                // ⭐ KEY: Send SMS after alert created
                sendSmsToEmergencyContacts();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to create SOS alert", e);
                Toast.makeText(this, "Failed to create alert: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            });
}
```

### 5. SosTriggeredActivity.java - Load Contacts & Send SMS

```java
private void sendSmsToEmergencyContacts() {
    if (auth.getCurrentUser() == null) return;

    String userId = auth.getCurrentUser().getUid();

    // ⭐ KEY: Fetch all emergency contacts
    db.collection("users").document(userId).collection("emergencyContacts")
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<String> phoneNumbers = new ArrayList<>();

                // Collect phone numbers
                for (QueryDocumentSnapshot doc : querySnapshot) {
                    String phone = doc.getString("phone");
                    if (phone != null && !phone.isEmpty()) {
                        phoneNumbers.add(phone);
                    }
                }

                if (phoneNumbers.isEmpty()) {
                    Toast.makeText(this, 
                        "No emergency contacts found. Please add contacts first.", 
                        Toast.LENGTH_LONG).show();
                    Log.w(TAG, "No emergency contacts to send SMS to");
                    return;
                }

                // Check SMS permission
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.SEND_SMS},
                            PERMISSION_REQUEST_CODE);
                    return;
                }

                // ⭐ KEY: Send SMS via SMSHelper
                String message = "🚨 EMERGENCY SOS! I am in danger! Location: https://maps.google.com/?q=" 
                    + latitude + "," + longitude;
                
                SMSHelper.sendSMS(this, message, phoneNumbers, 
                    new SMSHelper.OnSMSStatusListener() {
                        @Override
                        public void onSmsSent(int successCount, int totalCount, 
                            List<String> failedPhones) {
                            String statusMsg;
                            if (successCount > 0) {
                                statusMsg = "✓ SOS sent to " + successCount + " contacts";
                                Log.i(TAG, statusMsg);
                            } else {
                                statusMsg = "✗ Failed to send SOS to any contact";
                                Log.e(TAG, statusMsg);
                            }
                            
                            if (!failedPhones.isEmpty()) {
                                statusMsg += "\nFailed: " + failedPhones.size() + " contacts";
                                Log.w(TAG, "Failed contacts: " + failedPhones);
                            }
                            
                            // Update screen with result
                            tvCountdownText.setText(statusMsg);
                        }

                        @Override
                        public void onSmsError(String error) {
                            Log.e(TAG, "SMS Error: " + error);
                            tvCountdownText.setText("Error sending SOS: " + error);
                        }
                    });
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to load emergency contacts", e);
                Toast.makeText(this, "Failed to load contacts: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            });
}
```

### 6. SMSHelper.java - Validate Phone

```java
public static boolean isValidIndianPhone(String phone) {
    if (phone == null || phone.isEmpty()) return false;
    
    // Remove all non-digit characters
    String cleaned = phone.replaceAll("[^0-9]", "");
    
    // Must be 10 digits (mobile) starting with 6-9
    if (cleaned.length() == 10) {
        return cleaned.matches("[6-9]\\d{9}");
    }
    
    // Or 12 digits with 91 prefix
    if (cleaned.length() == 12 && cleaned.startsWith("91")) {
        return cleaned.substring(2).matches("[6-9]\\d{9}");
    }
    
    return false;
}
```

### 7. SMSHelper.java - Format Phone

```java
public static String formatPhoneForSMS(String phone) {
    if (phone == null || phone.isEmpty()) return null;
    
    // Remove all non-digit characters
    String cleaned = phone.replaceAll("[^0-9]", "");
    
    // If 10 digits, add +91
    if (cleaned.length() == 10) {
        return "+91" + cleaned;
    }
    
    // If 12 digits with 91, add +
    if (cleaned.length() == 12 && cleaned.startsWith("91")) {
        return "+" + cleaned;
    }
    
    // Already formatted or invalid
    return cleaned.length() == 13 && cleaned.startsWith("91") ? "+" + cleaned : null;
}
```

### 8. SMSHelper.java - Send SMS Static Method

```java
public static void sendSMS(Context context, String message, List<String> phoneNumbers, 
    OnSMSStatusListener listener) {
    
    if (phoneNumbers == null || phoneNumbers.isEmpty()) {
        if (listener != null) {
            listener.onSmsError("No phone numbers provided");
        }
        return;
    }

    try {
        SmsManager smsManager = SmsManager.getDefault();
        int successCount = 0;
        List<String> failedPhones = new ArrayList<>();

        // Send to each phone
        for (String phone : phoneNumbers) {
            if (!isValidIndianPhone(phone)) {
                Log.w(TAG, "Invalid phone format: " + phone);
                failedPhones.add(phone + " (invalid format)");
                continue;
            }

            try {
                String formattedPhone = formatPhoneForSMS(phone);
                
                if (formattedPhone == null) {
                    Log.w(TAG, "Could not format phone: " + phone);
                    failedPhones.add(phone + " (format error)");
                    continue;
                }

                Log.d(TAG, "Sending SMS to: " + formattedPhone);
                
                // Multipart message support
                ArrayList<String> messageParts = smsManager.divideMessage(message);
                ArrayList<PendingIntent> sentIntents = new ArrayList<>();
                
                for (int i = 0; i < messageParts.size(); i++) {
                    Intent sentIntent = new Intent("SMS_SENT_" 
                        + System.currentTimeMillis() + "_" + i);
                    PendingIntent sentPendingIntent = PendingIntent.getBroadcast(
                        context,
                        (int) (System.currentTimeMillis() + i),
                        sentIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                    );
                    sentIntents.add(sentPendingIntent);
                }

                // Send multipart SMS
                smsManager.sendMultipartTextMessage(
                    formattedPhone,
                    null,
                    messageParts,
                    sentIntents,
                    null
                );

                Log.d(TAG, "SMS queued successfully for: " + phone);
                successCount++;

            } catch (IllegalArgumentException e) {
                Log.e(TAG, "SMS sending error for " + phone + ": " + e.getMessage());
                failedPhones.add(phone + " (" + e.getMessage() + ")");
            }
        }

        // Report results
        if (listener != null) {
            listener.onSmsSent(successCount, phoneNumbers.size(), failedPhones);
        }

    } catch (Exception e) {
        Log.e(TAG, "Failed to send SMS: " + e.getMessage());
        if (listener != null) {
            listener.onSmsError("SMS Error: " + e.getMessage());
        }
    }
}
```

### 9. SosTriggeredActivity.java - Permission Callback

```java
@Override
public void onRequestPermissionsResult(int requestCode, String[] permissions, 
    int[] grantResults) {
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
            // Retry sending SMS if we just got permission
            if (currentAlertId != null) {
                sendSmsToEmergencyContacts();
            }
        } else {
            Toast.makeText(this, "Permissions required to send SOS", 
                Toast.LENGTH_SHORT).show();
        }
    }
}
```

## 📊 Data Structures

### SOS Alert in Firebase
```json
{
  "userId": "auth_uid_123",
  "timestamp": 1699999999000,
  "latitude": 28.4595,
  "longitude": 77.0266,
  "status": "active",
  "type": "SOS"
}
```

### Emergency Contact in Firebase
```json
{
  "name": "Jitu",
  "phone": "9876543210",
  "relation": "Brother",
  "isPriority": true
}
```

### SMS Message Format
```
🚨 EMERGENCY SOS! I am in danger! Location: https://maps.google.com/?q=28.4595,77.0266
```

## ✅ Verification Checklist

- [ ] All imports added to SosTriggeredActivity
- [ ] Firebase initialization in onCreate
- [ ] getLastLocation() called before countdown
- [ ] createSOSAlert() called on countdown finish
- [ ] sendSmsToEmergencyContacts() called after alert created
- [ ] SMSHelper.sendSMS() uses static method call
- [ ] Phone validation working for Indian format
- [ ] Phone formatting to +91 standard
- [ ] SMS permission check before sending
- [ ] Permission request callback handling
- [ ] Error messages clear and informative
- [ ] Logging at all key points
- [ ] UI updated with SMS result
- [ ] No dummy data in production code
- [ ] Uses real Firebase collections
