# Quick Testing Guide for SOS SMS Fix

## Before Testing
1. Ensure you're logged in to the app
2. Add at least ONE emergency contact through EmergencyContactsActivity
3. Use VALID Indian phone number format:
   - 10 digits starting with 6-9 (e.g., 9876543210)
   - OR +91 prefix (e.g., +919876543210)
4. Grant SMS and Location permissions when prompted

## Testing Steps

### Step 1: Add Emergency Contact
1. Open app → Dashboard
2. Click "Emergency Contacts" card
3. Fill in:
   - **Name**: Test Contact
   - **Phone**: Your mobile number (10 digits or +91 format)
   - **Relation**: Brother/Sister/Friend
   - Optional: Check "Set as Priority Contact"
4. Click "Save"
5. Verify contact appears in list

### Step 2: Trigger SOS
1. Return to Dashboard
2. Click **SOS Button** (big red button)
3. You'll see pulse animation and "SOS Triggered" message
4. Wait 2 seconds, then SosTriggeredActivity opens

### Step 3: Monitor Countdown
1. Watch 5-second countdown
2. Screen shows: "Triggering alert in X seconds..."
3. When countdown reaches 0:
   - Text changes to "SOS Alert Sent to Emergency Contacts!"
   - Toast shows "Emergency Alert Sent Successfully!"

### Step 4: Check SMS Sending (Logcat)
Open Android Studio Logcat and filter:
```
SOS_TRIGGERED|SMSHelper
```

You should see logs like:
```
D/SOS_TRIGGERED: Location obtained: 28.4595, 77.0266
D/SOS_TRIGGERED: SOS Alert created: abc123def456
D/SMSHelper: Sending SMS to: +919876543210
D/SMSHelper: SMS queued successfully for: 9876543210
I/SOS_TRIGGERED: ✓ SOS sent to 1 contacts
```

### Step 5: Receive SMS
Check your phone for SMS message:
```
🚨 EMERGENCY SOS! I am in danger! Location: https://maps.google.com/?q=28.4595,77.0266
```

## Expected Outcomes

✅ **Success**: 
- SMS received on your phone within 2-5 seconds
- Location link is clickable and shows actual coordinates
- No errors in Logcat

⚠️ **Common Issues & Solutions**:

| Issue | Cause | Fix |
|-------|-------|-----|
| No SMS received | Invalid phone format | Check phone matches: 10 digits or +91XXXXXXXXXX |
| No SMS received | SMS permission not granted | Grant permission in app or Settings |
| "No emergency contacts" | Forgot to add contact | Add contact through EmergencyContacts screen |
| Logcat shows "Invalid phone format" | Phone number has spaces/dashes | Remove special characters or re-enter |
| "Failed to load contacts" | Not logged in | Ensure user is authenticated |
| SMS appears to send but doesn't arrive | Telecom carrier issue | Retry after 1-2 minutes |

## Debugging Tips

### Check if Contact is Saved
1. Open Logcat: `Firestore`
2. Add contact and watch logs
3. Should see successful write operations

### Check if SMS Permission Granted
1. Settings → Apps → Aapraksha → Permissions
2. Find "SMS" permission
3. Should show "✓ Allowed" or "✓ Granted"

### Check Real Location
1. Enable GPS on phone
2. Open Google Maps to get fix
3. Then test SOS
4. Logcat should show different coordinates than default

### View Firebase Data
1. Open Firebase Console
2. Go to Firestore Database
3. Check `sosAlerts` collection for new alerts
4. Verify `users/{userId}/emergencyContacts` has your contact

## Full Test Flow (5 minutes)

```
Time    Action                          Expected Result
0:00    Open app                        Dashboard loads
0:15    Click "Emergency Contacts"      Contact list appears
0:30    Add contact (9876543210)        "Contact saved successfully"
1:00    Return to Dashboard             Dashboard shows updated
1:15    Click SOS button                Pulse animation starts
1:25    SosTriggeredActivity opens      5-second countdown visible
1:30    Watch countdown to 0            Screen shows "SOS Alert Sent"
2:00    Check Logcat                    Logs show SMS sending
3:00    Check your phone                SMS message received ✓
```

## Emergency Testing (Without Real SMS)

If you can't test with real SMS on your device:

1. **Use Logcat Verification**: 
   - If logs show "SMS queued successfully", SMS was sent
   - Firebase collection will have alert record

2. **Check Firebase**:
   - New document in `sosAlerts` collection
   - With correct userId, location, timestamp

3. **Unit Test**:
   - SMSHelper.isValidIndianPhone("9876543210") → true
   - SMSHelper.formatPhoneForSMS("9876543210") → "+919876543210"

## Performance Notes

- First SMS takes 1-3 seconds to queue
- Multiple contacts sent in parallel
- Total time for 3 contacts: ~2 seconds
- Firestore document creation: ~1 second

## Success Confirmation

All of these should be true:
- ✅ Contact saves to Firestore
- ✅ SosTriggeredActivity shows 5-sec countdown
- ✅ SMS sends after countdown
- ✅ Logcat shows "SMS queued successfully"
- ✅ Firebase `sosAlerts` collection has new document
- ✅ Phone receives SMS with location link
- ✅ Can cancel before countdown finishes
