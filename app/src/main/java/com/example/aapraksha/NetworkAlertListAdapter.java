package com.example.aapraksha;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * NetworkAlertListAdapter for displaying live SOS alerts from other users
 * Uses item_network_alert_card.xml layout matching the original network alerts design
 */
public class NetworkAlertListAdapter extends RecyclerView.Adapter<NetworkAlertListAdapter.ViewHolder> {
    private List<SOSAlert> alerts;
    private Context context;
    private OnNetworkAlertClickListener listener;

    public interface OnNetworkAlertClickListener {
        void onAlertClicked(SOSAlert alert);
    }

    public NetworkAlertListAdapter(Context context, List<SOSAlert> alerts, OnNetworkAlertClickListener listener) {
        this.context = context;
        this.alerts = alerts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_network_alert_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SOSAlert alert = alerts.get(position);
        holder.bind(alert, listener, context);
    }

    @Override
    public int getItemCount() {
        return alerts.size();
    }

    public void updateAlerts(List<SOSAlert> newAlerts) {
        this.alerts = newAlerts;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView alertStatus;
        private TextView userInitials;
        private TextView userName;
        private TextView locationAddress;
        private TextView timeAgo;
        private TextView distanceText;
        private LinearLayout distanceLayout;
        private CardView alertContainer;
        private CardView statusBadgeCard;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            alertStatus = itemView.findViewById(R.id.alert_status);
            userInitials = itemView.findViewById(R.id.user_initials);
            userName = itemView.findViewById(R.id.user_name);
            locationAddress = itemView.findViewById(R.id.location_address);
            timeAgo = itemView.findViewById(R.id.time_ago);
            distanceText = itemView.findViewById(R.id.distance_text);
            distanceLayout = itemView.findViewById(R.id.distance_layout);
            alertContainer = itemView.findViewById(R.id.alert_container);
            statusBadgeCard = itemView.findViewById(R.id.status_badge_card);
        }

        public void bind(SOSAlert alert, OnNetworkAlertClickListener listener, Context context) {
            // Set user name
            if (alert.getUserName() != null && !alert.getUserName().isEmpty()) {
                userName.setText(alert.getUserName());
                userInitials.setText(getInitials(alert.getUserName()));
            } else {
                userName.setText("Unknown User");
                userInitials.setText("?");
            }

            // Set location
            if (alert.getLocation() != null && alert.getLocation().getAddress() != null) {
                locationAddress.setText(alert.getLocation().getAddress());
                distanceLayout.setVisibility(View.VISIBLE);
                // For now, distance calculation is disabled as the model doesn't have it
                // and we don't have current user location in the adapter.
                // You can add this later using android.location.Location.distanceBetween()
                distanceLayout.setVisibility(View.GONE);
            } else {
                locationAddress.setText("Location unknown");
                distanceLayout.setVisibility(View.GONE);
            }

            // Set time ago
            if (alert.getCreatedAt() != null) {
                timeAgo.setText(getTimeAgo(alert.getCreatedAt().toDate().getTime()));
            } else {
                timeAgo.setText("---");
            }

            // Set status
            if (alert.getStatus() != null) {
                setStatusStyle(alert.getStatus(), context);
            }

            // Handle card click
            alertContainer.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAlertClicked(alert);
                }
            });
        }

        private void setStatusStyle(String status, Context context) {
            if ("ACTIVE".equals(status) || "TRIGGERED".equals(status)) {
                alertStatus.setText("ACTIVE ALERT");
                alertStatus.setTextColor(context.getResources().getColor(R.color.sos_red));
                statusBadgeCard.setCardBackgroundColor(0x33FF1744);
                timeAgo.setTextColor(context.getResources().getColor(R.color.sos_red));
                // Red border on the main card
                alertContainer.setCardBackgroundColor(context.getResources().getColor(R.color.surface_dark));
            } else if ("RESOLVED".equals(status) || "CANCELLED".equals(status)) {
                alertStatus.setText("RESOLVED");
                alertStatus.setTextColor(context.getResources().getColor(R.color.slate_grey));
                statusBadgeCard.setCardBackgroundColor(0x1A94A3B8);
                timeAgo.setTextColor(context.getResources().getColor(R.color.slate_grey));
                alertContainer.setCardBackgroundColor(context.getResources().getColor(R.color.surface_dark));
            }
        }

        private String getInitials(String name) {
            if (name == null || name.isEmpty()) return "?";
            String[] parts = name.trim().split("\\s+");
            StringBuilder initials = new StringBuilder();
            for (int i = 0; i < Math.min(parts.length, 2); i++) {
                if (!parts[i].isEmpty()) {
                    initials.append(parts[i].charAt(0));
                }
            }
            return initials.toString().toUpperCase();
        }

        private String getTimeAgo(long timestamp) {
            long currentTime = System.currentTimeMillis();
            long diff = currentTime - timestamp;

            long seconds = diff / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            long days = hours / 24;

            if (seconds < 60) {
                return "now";
            } else if (minutes < 60) {
                return minutes + "m";
            } else if (hours < 24) {
                return hours + "h";
            } else if (days < 7) {
                return days + "d";
            } else {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());
                return sdf.format(timestamp);
            }
        }
    }
}
