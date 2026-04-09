package com.example.aapraksha.ai.danger;

import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Repository for the Danger Intelligence Layer.
 * 
 * Reads cached danger zone scores from Firestore 'danger_zones' collection.
 * These scores are computed by the Cloud Function (calculateDangerZones)
 * triggered on SOS events — this class only READS, never computes scores.
 * 
 * Efficiency:
 * - Reads are cached by Firestore SDK's built-in offline persistence
 * - Listener only active when DangerMapFragment or DashboardActivity is in foreground
 * - Neighbor queries use geohash prefix matching (1 query for entire area)
 */
public class DangerZoneRepository {

    private static final String TAG = "DangerZoneRepo";
    private static final String COLLECTION_DANGER_ZONES = "danger_zones";
    private static final String COLLECTION_ALERTS = "alerts";

    private final FirebaseFirestore db;
    private ListenerRegistration dangerZoneListener;

    /**
     * Callback for danger zone queries
     */
    public interface OnDangerZonesLoadedListener {
        void onLoaded(List<DangerZone> zones);
        void onError(String error);
    }

    /**
     * Callback for single danger zone check
     */
    public interface OnDangerCheckListener {
        void onResult(DangerZone zone); // null if zone is SAFE or not found
        void onError(String error);
    }

    public DangerZoneRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    // ========== Read Operations ==========

    /**
     * Check the danger level at a specific location.
     * Looks up the geohash for the given coordinates in the danger_zones collection.
     *
     * @param latitude  User's current latitude
     * @param longitude User's current longitude
     * @param listener  Callback with the DangerZone result (null if safe)
     */
    public void checkDangerAtLocation(double latitude, double longitude, OnDangerCheckListener listener) {
        String geohash = GeoHashUtil.encode(latitude, longitude);

        db.collection(COLLECTION_DANGER_ZONES)
                .document(geohash)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        DangerZone zone = doc.toObject(DangerZone.class);
                        if (zone != null) {
                            zone.setGeohash(geohash);
                            listener.onResult(zone);
                        } else {
                            listener.onResult(null);
                        }
                    } else {
                        listener.onResult(null); // No data = safe
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to check danger at location", e);
                    listener.onError(e.getMessage());
                });
    }

    /**
     * Get all danger zones in the vicinity of a location.
     * Queries the center + 8 neighboring geohash cells.
     * 
     * This is used by DangerMapFragment for the heatmap overlay.
     *
     * @param latitude  Center latitude
     * @param longitude Center longitude
     * @param listener  Callback with list of danger zones
     */
    public void getDangerZonesNearby(double latitude, double longitude, OnDangerZonesLoadedListener listener) {
        String centerHash = GeoHashUtil.encode(latitude, longitude);
        String[] neighbors = GeoHashUtil.getNeighbors(centerHash);

        List<DangerZone> allZones = new ArrayList<>();
        final int[] completedQueries = {0};
        final boolean[] errorOccurred = {false};

        for (String geohash : neighbors) {
            db.collection(COLLECTION_DANGER_ZONES)
                    .document(geohash)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            DangerZone zone = doc.toObject(DangerZone.class);
                            if (zone != null) {
                                zone.setGeohash(geohash);
                                synchronized (allZones) {
                                    allZones.add(zone);
                                }
                            }
                        }
                        completedQueries[0]++;
                        if (completedQueries[0] == neighbors.length && !errorOccurred[0]) {
                            listener.onLoaded(allZones);
                        }
                    })
                    .addOnFailureListener(e -> {
                        completedQueries[0]++;
                        if (!errorOccurred[0]) {
                            errorOccurred[0] = true;
                            Log.e(TAG, "Failed to load nearby danger zones", e);
                            listener.onError(e.getMessage());
                        }
                    });
        }
    }

    /**
     * Get all danger zones with rank >= HIGH_RISK (3+).
     * Used for generating the citywide danger map.
     * 
     * Limited to 50 results to stay within Firestore free tier reads.
     *
     * @param listener Callback with list of high-risk zones
     */
    public void getHighRiskZones(OnDangerZonesLoadedListener listener) {
        db.collection(COLLECTION_DANGER_ZONES)
                .whereGreaterThanOrEqualTo("rank", DangerZone.RANK_HIGH_RISK)
                .orderBy("rank", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<DangerZone> zones = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot) {
                        DangerZone zone = doc.toObject(DangerZone.class);
                        if (zone != null) {
                            zone.setGeohash(doc.getId());
                            zones.add(zone);
                        }
                    }
                    listener.onLoaded(zones);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load high-risk zones", e);
                    listener.onError(e.getMessage());
                });
    }

    /**
     * Listen for real-time updates to a specific zone.
     * Used when the user is currently inside or near a danger zone.
     * 
     * Call removeListener() when the fragment/activity is paused to avoid
     * unnecessary Firestore reads.
     *
     * @param geohash  The zone to listen to
     * @param listener Callback on each update
     */
    public void listenToDangerZone(String geohash, OnDangerCheckListener listener) {
        removeListener();

        dangerZoneListener = db.collection(COLLECTION_DANGER_ZONES)
                .document(geohash)
                .addSnapshotListener((doc, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Danger zone listener error", error);
                        listener.onError(error.getMessage());
                        return;
                    }
                    if (doc != null && doc.exists()) {
                        DangerZone zone = doc.toObject(DangerZone.class);
                        if (zone != null) {
                            zone.setGeohash(geohash);
                        }
                        listener.onResult(zone);
                    } else {
                        listener.onResult(null);
                    }
                });
    }

    /**
     * Remove the real-time listener. Call this in onPause()/onDestroy().
     */
    public void removeListener() {
        if (dangerZoneListener != null) {
            dangerZoneListener.remove();
            dangerZoneListener = null;
        }
    }

    // ========== Utility ==========

    /**
     * Get the geohash for a given location (convenience method).
     */
    public static String getGeohashForLocation(double latitude, double longitude) {
        return GeoHashUtil.encode(latitude, longitude);
    }

    /**
     * Get the time-of-day category for the current time.
     */
    public static String getCurrentTimeOfDay() {
        Calendar cal = Calendar.getInstance();
        return GeoHashUtil.getTimeOfDay(cal.get(Calendar.HOUR_OF_DAY));
    }
}
