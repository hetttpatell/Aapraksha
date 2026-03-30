package com.example.aapraksha;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LocationHistoryActivity extends AppCompatActivity {

    private static final String TAG = "LocationHistory";
    private RecyclerView recyclerView;
    private LocationAdapter adapter;
    private List<LocationPoint> locationList = new ArrayList<>();
    private FirebaseFirestore db;
    private String userId;
    private String userName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_history);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Location History");
        }

        userId = getIntent().getStringExtra("userId");
        userName = getIntent().getStringExtra("userName");

        if (userId == null) {
            Toast.makeText(this, "User ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        TextView tvSubtitle = findViewById(R.id.tvSubtitle);
        tvSubtitle.setText(userName != null ? userName : "User");

        recyclerView = findViewById(R.id.recyclerViewLocations);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LocationAdapter(locationList);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        loadLocations();
    }

    private void loadLocations() {
        db.collection("users").document(userId)
                .collection("locations")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    locationList.clear();
                    Log.d(TAG, "Found " + queryDocumentSnapshots.size() + " location points");
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        LocationPoint point = doc.toObject(LocationPoint.class);
                        if (point != null) {
                            locationList.add(point);
                        }
                    }
                    adapter.notifyDataSetChanged();
                    if (locationList.isEmpty()) {
                        Toast.makeText(this, "No location history yet", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load locations", e);
                    Toast.makeText(this, "Failed to load locations: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Location Point POJO for Firestore
     */
    public static class LocationPoint {
        private double latitude;
        private double longitude;
        private long timestamp;
        private String accuracy;

        public LocationPoint() {}

        public LocationPoint(double latitude, double longitude, long timestamp) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.timestamp = timestamp;
        }

        public double getLatitude() { return latitude; }
        public void setLatitude(double latitude) { this.latitude = latitude; }

        public double getLongitude() { return longitude; }
        public void setLongitude(double longitude) { this.longitude = longitude; }

        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

        public String getAccuracy() { return accuracy; }
        public void setAccuracy(String accuracy) { this.accuracy = accuracy; }
    }

    /**
     * RecyclerView Adapter for locations
     */
    private static class LocationAdapter extends RecyclerView.Adapter<LocationAdapter.ViewHolder> {
        private List<LocationPoint> locations;
        private SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

        public LocationAdapter(List<LocationPoint> locations) {
            this.locations = locations;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            android.view.View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_location, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            LocationPoint point = locations.get(position);
            String dateStr = sdf.format(new Date(point.getTimestamp()));
            holder.tvTime.setText(dateStr);
            holder.tvCoordinates.setText(String.format(Locale.getDefault(),
                    "%.6f, %.6f", point.getLatitude(), point.getLongitude()));
        }

        @Override
        public int getItemCount() {
            return locations.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTime, tvCoordinates;

            ViewHolder(android.view.View itemView) {
                super(itemView);
                tvTime = itemView.findViewById(R.id.tvTime);
                tvCoordinates = itemView.findViewById(R.id.tvCoordinates);
            }
        }
    }
}
