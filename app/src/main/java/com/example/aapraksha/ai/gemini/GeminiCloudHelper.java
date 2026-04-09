package com.example.aapraksha.ai.gemini;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;

import java.util.HashMap;
import java.util.Map;

/**
 * GeminiCloudHelper — Calls Firebase Cloud Functions to interact with Gemini safely.
 * 
 * DESIGN:
 * - We do NOT call the Gemini API from the Android app directly for security.
 * - We call callable Cloud Functions which manage API keys and caching.
 * - Smart message templates are cached per geohash in Cloud Firestore.
 */
public class GeminiCloudHelper {

    private static final String TAG = "GeminiCloudHelper";
    private final FirebaseFunctions functions;

    public GeminiCloudHelper() {
        this.functions = FirebaseFunctions.getInstance();
    }

    public interface OnMessageReadyListener {
        void onReady(String smartMessage);
        void onError(Exception e);
    }

    /**
     * Call 'getSmartMessage' Cloud Function.
     * This provides a context-aware SOS message based on the user's location.
     */
    public void getSmartMessage(String alertId, String name, String locationStr, 
                                String geohash, String timeOfDay, 
                                OnMessageReadyListener listener) {
        
        Map<String, Object> data = new HashMap<>();
        data.put("alertId", alertId);
        data.put("name", name);
        data.put("locationStr", locationStr);
        data.put("geohash", geohash);
        data.put("timeOfDay", timeOfDay);

        Log.d(TAG, "Requesting smart message for alert: " + alertId);

        functions
                .getHttpsCallable("getSmartMessage")
                .call(data)
                .continueWith(task -> {
                    // This continuation runs even if the task fails
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    HttpsCallableResult result = task.getResult();
                    Map<String, Object> responseData = (Map<String, Object>) result.getData();
                    return (String) responseData.get("message");
                })
                .addOnSuccessListener(message -> {
                    Log.d(TAG, "Smart message received: " + message);
                    if (listener != null) listener.onReady(message);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch smart message", e);
                    if (listener != null) listener.onError(e);
                });
    }
}
