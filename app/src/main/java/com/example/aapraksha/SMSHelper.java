package com.example.aapraksha;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class SMSHelper {
    private static final String TAG = "SMSHelper";
    
    public interface OnSMSStatusListener {
        void onSmsSent(int successCount, int totalCount, List<String> failedPhones);
        void onSmsError(String error);
    }

    /**
     * Validate Indian phone number format
     * Accepts: 10 digits, +919876543210, 919876543210
     */
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

    /**
     * Format phone number to E.164 standard (+91XXXXXXXXXX)
     */
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

    /**
     * Send SMS to multiple numbers with proper validation
     */
    public static void sendSMS(Context context, String message, List<String> phoneNumbers, OnSMSStatusListener listener) {
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
                    
                    // For multipart messages
                    ArrayList<String> messageParts = smsManager.divideMessage(message);
                    ArrayList<PendingIntent> sentIntents = new ArrayList<>();
                    
                    for (int i = 0; i < messageParts.size(); i++) {
                        Intent sentIntent = new Intent("SMS_SENT_" + System.currentTimeMillis() + "_" + i);
                        PendingIntent sentPendingIntent = PendingIntent.getBroadcast(
                            context,
                            (int) (System.currentTimeMillis() + i),
                            sentIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                        );
                        sentIntents.add(sentPendingIntent);
                    }

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
                } catch (Exception e) {
                    Log.e(TAG, "Unexpected error sending SMS to " + phone, e);
                    failedPhones.add(phone + " (" + e.getMessage() + ")");
                }
            }

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
}

