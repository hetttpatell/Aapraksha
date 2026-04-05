package com.example.aapraksha;

import android.util.Log;

import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

/**
 * AlertService manages real-time alert listeners and data operations
 * Handles lifecycle of alert listeners (attach on start, detach on stop)
 */
public class AlertService {
    private static final String TAG = "AlertService";
    private final SOSAlertRepository sosAlertRepository;
    private ListenerRegistration liveAlertsListener;
    private ListenerRegistration userAlertsListener;
    private AlertServiceCallback callback;

    public interface AlertServiceCallback {
        void onLiveAlertsUpdated(List<SOSAlert> alerts);
        void onUserAlertsUpdated(List<SOSAlert> alerts);
        void onError(String errorMessage, String type);
    }

    public AlertService(SOSAlertRepository sosAlertRepository) {
        this.sosAlertRepository = sosAlertRepository;
    }

    /**
     * Set callback for alert updates
     */
    public void setCallback(AlertServiceCallback callback) {
        this.callback = callback;
    }

    /**
     * Start listening to live alerts from other users
     */
    public void startLiveAlertsListener() {
        if (liveAlertsListener != null) {
            Log.w(TAG, "Live alerts listener already active");
            return;
        }

        Log.d(TAG, "Starting live alerts listener");
        liveAlertsListener = sosAlertRepository.listenToLiveAlerts(new SOSAlertRepository.OnRealtimeAlertsListener() {
            @Override
            public void onAlertsUpdated(List<SOSAlert> alerts) {
                if (callback != null) {
                    callback.onLiveAlertsUpdated(alerts);
                }
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error in live alerts listener: " + errorMessage);
                if (callback != null) {
                    callback.onError(errorMessage, "LIVE_ALERTS");
                }
            }
        });
    }

    /**
     * Start listening to user's alert history
     */
    public void startUserAlertsListener(String userId) {
        if (userAlertsListener != null) {
            Log.w(TAG, "User alerts listener already active");
            return;
        }

        Log.d(TAG, "Starting user alerts listener for: " + userId);
        userAlertsListener = sosAlertRepository.listenToUserAlerts(userId, new SOSAlertRepository.OnRealtimeAlertsListener() {
            @Override
            public void onAlertsUpdated(List<SOSAlert> alerts) {
                if (callback != null) {
                    callback.onUserAlertsUpdated(alerts);
                }
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error in user alerts listener: " + errorMessage);
                if (callback != null) {
                    callback.onError(errorMessage, "USER_ALERTS");
                }
            }
        });
    }

    /**
     * Stop listening to live alerts
     */
    public void stopLiveAlertsListener() {
        if (liveAlertsListener != null) {
            liveAlertsListener.remove();
            liveAlertsListener = null;
            Log.d(TAG, "Live alerts listener stopped");
        }
    }

    /**
     * Stop listening to user alerts
     */
    public void stopUserAlertsListener() {
        if (userAlertsListener != null) {
            userAlertsListener.remove();
            userAlertsListener = null;
            Log.d(TAG, "User alerts listener stopped");
        }
    }

    /**
     * Stop all listeners
     */
    public void stopAllListeners() {
        stopLiveAlertsListener();
        stopUserAlertsListener();
        Log.d(TAG, "All listeners stopped");
    }

    /**
     * Fetch single alert by ID
     */
    public void getAlertById(String alertId, SOSAlertRepository.OnAlertFetchListener listener) {
        sosAlertRepository.getAlertById(alertId, listener);
    }

    /**
     * Fetch live alerts once (one-time fetch)
     */
    public void fetchLiveAlerts(SOSAlertRepository.OnAlertsListFetchListener listener) {
        sosAlertRepository.fetchLiveAlerts(listener);
    }

    /**
     * Fetch user's alerts with pagination
     */
    public void fetchUserAlerts(String userId, int limit, SOSAlertRepository.OnAlertsListFetchListener listener) {
        sosAlertRepository.fetchUserAlerts(userId, limit, listener);
    }

    /**
     * Fetch alerts by status (for filtering)
     */
    public void fetchAlertsByStatus(String userId, String status, SOSAlertRepository.OnAlertsListFetchListener listener) {
        sosAlertRepository.fetchAlertsByStatus(userId, status, listener);
    }

    /**
     * Check if live alerts listener is active
     */
    public boolean isLiveAlertsListenerActive() {
        return liveAlertsListener != null;
    }

    /**
     * Check if user alerts listener is active
     */
    public boolean isUserAlertsListenerActive() {
        return userAlertsListener != null;
    }
}
