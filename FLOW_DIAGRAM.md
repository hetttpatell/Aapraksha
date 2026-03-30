# SOS Complete Flow Diagram

## 🎯 Main Flow: Dashboard → SOS Triggered → SMS Sent

```
┌─────────────────────────────────────────────────────────────────┐
│                     DASHBOARD ACTIVITY                         │
│  Welcome User [Name]                                           │
│  [SOS BUTTON]  [Emergency Contacts]  [Settings]                │
│                                                                 │
│  User clicks SOS Button                                        │
│  ↓                                                              │
│  startContinuousPulse() - starts 3-ring pulse animation       │
│  triggerSOS() - shows "SOS Triggered" toast + 2sec delay      │
│  ↓ (after 2 seconds)                                          │
│  Intent → SosTriggeredActivity                                │
│  finish()                                                      │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│               SOS TRIGGERED ACTIVITY                            │
│                                                                 │
│  onCreate():                                                   │
│  ├─ Initialize Firebase (auth, db, location)                 │
│  ├─ getLastLocation() - fetch real device location           │
│  ├─ startPulseAnimation() - 3 pulsing rings                   │
│  └─ startCountdown() - 5-second countdown                     │
│                                                                 │
│  🔴🔴🔴 Pulse Animation                                        │
│  🕐 5 seconds... 4... 3... 2... 1... 0                        │
│  [Cancel SOS] button                                          │
│                                                                 │
│  If User Clicks [Cancel SOS]:                                 │
│  └─ cancelSos() → finish() → back to Dashboard               │
│                                                                 │
│  If Countdown Finishes → onFinish():                          │
│  ├─ tvCountdown.setText("00")                                 │
│  ├─ Toast "Emergency Alert Sent Successfully!"               │
│  └─ createSOSAlert()                                          │
└─────────────────────────────────────────────────────────────────┘
                            ↓
                   ┌────────────────┐
                   │  CREATE SOS    │
                   │    ALERT       │
                   └────────────────┘
                            ↓
        ┌───────────────────────────────────────────┐
        │ Firebase:sosAlerts Collection             │
        │ {                                         │
        │   userId: "abc123"                        │
        │   timestamp: 1699999999                   │
        │   latitude: 28.4595                       │
        │   longitude: 77.0266                      │
        │   status: "active"                        │
        │   type: "SOS"                             │
        │ }                                         │
        └───────────────────────────────────────────┘
                            ↓
          ✓ Alert Created → onSuccess()
                            ↓
             sendSmsToEmergencyContacts()
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│      LOAD EMERGENCY CONTACTS FROM FIRESTORE                    │
│                                                                 │
│  Query: users/{userId}/emergencyContacts                      │
│  ├─ Contact 1: "Jitu" +919876543210 (isPriority: true)       │
│  ├─ Contact 2: "Mom" 9876543211 (isPriority: false)           │
│  └─ Contact 3: "Dad" 9876543212 (isPriority: false)           │
│                                                                 │
│  Create List:                                                  │
│  phoneNumbers = ["+919876543210", "9876543211", "9876543212"] │
└─────────────────────────────────────────────────────────────────┘
                            ↓
           ┌────────────────────────────┐
           │ VALIDATE PHONE NUMBERS     │
           └────────────────────────────┘
                            ↓
        ┌──────────────────────────────────────┐
        │ SMSHelper.isValidIndianPhone()       │
        ├─ "+919876543210"   ✅ Valid         │
        ├─ "9876543211"      ✅ Valid         │
        ├─ "9876543212"      ✅ Valid         │
        └──────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│         CHECK PERMISSIONS                                      │
│                                                                 │
│  if (ContextCompat.checkSelfPermission(SEND_SMS))              │
│  ├─ ✅ GRANTED → Continue                                     │
│  └─ ❌ NOT GRANTED → requestPermissions()                     │
│                  ↓                                             │
│          User Grants Permission                                │
│                  ↓                                             │
│          Retry sendSmsToEmergencyContacts()                    │
└─────────────────────────────────────────────────────────────────┘
                            ↓
        ┌────────────────────────────────┐
        │  FORMAT PHONE NUMBERS          │
        │                                │
        │  "+919876543210" → "+919876543210" ✓
        │  "9876543211"    → "+919876543211" ✓
        │  "9876543212"    → "+919876543212" ✓
        └────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│        SEND SMS VIA ANDROID SMS MANAGER                        │
│                                                                 │
│  message = "🚨 EMERGENCY SOS! I am in danger! Location: ..."  │
│                                                                 │
│  For each phone number:                                        │
│  ├─ SmsManager.sendMultipartTextMessage(                       │
│  │   phone: "+919876543210"                                    │
│  │   message: "🚨 EMERGENCY SOS! ..."                          │
│  │   sentIntents: [PendingIntent]                              │
│  │   )                                                         │
│  │  ✓ SMS Queued                                              │
│  │                                                             │
│  ├─ Repeat for phone 2 → ✓ SMS Queued                         │
│  └─ Repeat for phone 3 → ✓ SMS Queued                         │
│                                                                 │
│  successCount = 3 / 3 total                                    │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│        CALLBACK: onSmsSent()                                   │
│                                                                 │
│  Update UI on SosTriggeredActivity:                            │
│  tvCountdownText.setText("✓ SOS sent to 3 contacts")          │
│                                                                 │
│  Logcat Output:                                                │
│  I/SOS_TRIGGERED: ✓ SOS sent to 3 contacts                    │
│  I/SOS_TRIGGERED: Failed: 0 contacts                          │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│        ANDROID OS SENDS SMS (1-5 seconds)                      │
│                                                                 │
│  Android SMS Queue                                             │
│  ├─ Jitu's Phone   → SMS sent via carrier (2-3 sec)           │
│  ├─ Mom's Phone    → SMS sent via carrier (2-3 sec)           │
│  └─ Dad's Phone    → SMS sent via carrier (2-3 sec)           │
│                                                                 │
│  Each recipient receives:                                      │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │ SMS From: [Your Number]                                  │ │
│  │ 🚨 EMERGENCY SOS! I am in danger!                        │ │
│  │ Location: https://maps.google.com/?q=28.4595,77.0266    │ │
│  │                                                          │ │
│  │ [VIEW ON MAP]  [COPY]  [REPLY]                          │ │
│  └──────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

## 📊 Data Flow

```
User Input (SOS Button)
        ↓
   DashboardActivity
   ├─ Pulse Animation
   └─ Intent → SosTriggeredActivity
                ↓
         SosTriggeredActivity
         ├─ Location Service (get real location)
         ├─ Firebase Auth (get userId)
         ├─ 5-second Countdown Timer
         └─ onFinish()
                ↓
         Firebase Operations
         ├─ POST: sosAlerts/
         │   {userId, location, timestamp, status}
         │
         └─ GET: users/{userId}/emergencyContacts
             [{name, phone, isPriority}, ...]
                ↓
         Phone Validation & Formatting
         ├─ isValidIndianPhone(phone)
         ├─ formatPhoneForSMS(phone)
         └─ Build phonNumbers[]
                ↓
         Permission Check
         ├─ Manifest: SEND_SMS ✓
         └─ Runtime: checkSelfPermission() ✓
                ↓
         SMS Sending via SMSHelper
         ├─ For each phone in phoneNumbers:
         │   ├─ Format: "+91XXXXXXXXXX"
         │   ├─ SmsManager.sendMultipartTextMessage()
         │   ├─ successCount++
         │   └─ Log: "SMS queued"
         │
         └─ Callback: onSmsSent(success, total)
                ↓
         Update UI
         ├─ tvCountdownText: "✓ SOS sent to X contacts"
         └─ Logcat: "I/SOS_TRIGGERED: ✓ SOS sent to X contacts"
                ↓
         Android SMS Queue
         ├─ SMS Provider processes queue
         └─ Sends via carrier to each contact
                ↓
         Recipients Receive SMS (in inbox)
         ├─ Contact 1: Received ✓
         ├─ Contact 2: Received ✓
         └─ Contact 3: Received ✓
```

## 🔴 Error Handling Flow

```
                    Start SOS
                        ↓
            ┌───────────────────────────┐
            │ Location Permission?      │
            └───────────────────────────┘
            ├─ ❌ NO → Request Permission → User Grants → Continue
            └─ ✅ YES → Get Location → Continue
                            ↓
            ┌───────────────────────────────────────┐
            │ Location Available?                   │
            └───────────────────────────────────────┘
            ├─ ✅ YES → Use real coordinates
            └─ ❌ NO → Use fallback (28.4595, 77.0266)
                            ↓
            ┌───────────────────────────────────────┐
            │ Can Create SOS Alert?                 │
            └───────────────────────────────────────┘
            ├─ ✅ YES → Alert created in Firestore
            └─ ❌ NO → Toast: "Failed to create alert"
                    └─ Stop here
                            ↓
            ┌───────────────────────────────────────┐
            │ Any Emergency Contacts?               │
            └───────────────────────────────────────┘
            ├─ ❌ NO → Toast: "No contacts. Add first"
            │         └─ Stop here
            └─ ✅ YES → Continue
                            ↓
            ┌───────────────────────────────────────┐
            │ SMS Permission Granted?               │
            └───────────────────────────────────────┘
            ├─ ❌ NO → Request Permission
            │         ├─ User Denies → Toast: "Permission needed"
            │         │                └─ Stop here
            │         └─ User Grants → Continue
            └─ ✅ YES → Continue
                            ↓
            ┌───────────────────────────────────────┐
            │ Phone Numbers Valid Format?           │
            └───────────────────────────────────────┘
            ├─ ✅ YES → Send SMS
            └─ ❌ NO → Toast: "Invalid format"
                    └─ Skip that contact
                            ↓
            ┌───────────────────────────────────────┐
            │ SmsManager.sendMultipartTextMessage() │
            └───────────────────────────────────────┘
            ├─ ✓ OK → successCount++
            └─ ✗ Exception → failedPhones.add()
                            ↓
            ┌───────────────────────────────────────┐
            │ Callback: onSmsSent()                 │
            └───────────────────────────────────────┘
            └─ Show: "✓ Sent to X/Y contacts"
```

## ⏱️ Timeline Example (3 Contacts)

```
00:00 - User clicks SOS button
00:02 - Dashboard pulse animation stops
00:02 - SosTriggeredActivity opens, countdown starts
00:03 - Countdown: "Triggering in 5 seconds..."
00:04 - Countdown: "Triggering in 4 seconds..."
00:05 - Countdown: "Triggering in 3 seconds..."
00:06 - Countdown: "Triggering in 2 seconds..."
00:07 - Countdown: "Triggering in 1 seconds..."
00:08 - Countdown finishes: "SOS Alert Sent to Emergency Contacts!"
00:09 - Firebase: SOS alert created
00:09 - Load emergency contacts from Firestore
00:10 - Validate 3 phone numbers
00:10 - Check SMS permission (already granted)
00:11 - Send SMS to contact 1 via SmsManager
00:11 - Send SMS to contact 2 via SmsManager
00:12 - Send SMS to contact 3 via SmsManager
00:12 - SMSHelper callback: onSmsSent(3, 3, [])
00:12 - UI updates: "✓ SOS sent to 3 contacts"
00:13 - Android SMS queue processing
00:14 - Contact 1 receives SMS (from carrier)
00:14 - Contact 2 receives SMS (from carrier)
00:15 - Contact 3 receives SMS (from carrier)
```

## 📱 Screen States

```
BEFORE:                          AFTER FIX:
┌──────────────────────┐       ┌──────────────────────────┐
│ Dashboard            │       │ Dashboard                │
│ [SOS Button] ← Click │       │ [SOS Button] ← Click     │
│                      │       │                          │
│ Toast:               │       │ Toast:                   │
│ "SOS Triggered"      │       │ "SOS Triggered" (2 sec)  │
│                      │       │                          │
│ SMS Sent:            │       │ SMS Sent:                │
│ ❌ NO                │       │ ✅ YES (after 7 sec)    │
│                      │       │                          │
│ Emergency Contact:   │       │ Emergency Contact:       │
│ ❌ No SMS Received   │       │ ✅ SMS Received in 3-5s │
└──────────────────────┘       └──────────────────────────┘
```
