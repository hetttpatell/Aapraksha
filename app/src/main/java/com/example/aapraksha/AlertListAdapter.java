package com.example.aapraksha;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * AlertListAdapter for displaying live SOS alerts from other users
 */
public class AlertListAdapter extends RecyclerView.Adapter<AlertListAdapter.ViewHolder> {
    private List<SOSAlert> alerts;
    private Context context;
    private OnAlertClickListener listener;

    public interface OnAlertClickListener {
        void onAlertClicked(SOSAlert alert);
        void onRespondNowClicked(SOSAlert alert);
    }

    public AlertListAdapter(Context context, List<SOSAlert> alerts, OnAlertClickListener listener) {
        this.context = context;
        this.alerts = alerts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_alert_card, parent, false);
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
        private ImageView userPhoto;
        private TextView userName;
        private TextView timeAgo;
        private TextView locationAddress;
        private TextView alertStatus;
        private Button respondNowBtn;
        private View alertContainer;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            userPhoto = itemView.findViewById(R.id.user_photo);
            userName = itemView.findViewById(R.id.user_name);
            timeAgo = itemView.findViewById(R.id.time_ago);
            locationAddress = itemView.findViewById(R.id.location_address);
            alertStatus = itemView.findViewById(R.id.alert_status);
            respondNowBtn = itemView.findViewById(R.id.respond_now_btn);
            alertContainer = itemView.findViewById(R.id.alert_container);
        }

        public void bind(SOSAlert alert, OnAlertClickListener listener, Context context) {
            // Set user name and photo
            if (alert.getUserName() != null) {
                userName.setText(alert.getUserName());
            }

            // Set time ago
            if (alert.getCreatedAt() != null) {
                timeAgo.setText(getTimeAgo(alert.getCreatedAt().toDate().getTime()));
            }

            // Set location
            if (alert.getLocation() != null && alert.getLocation().getAddress() != null) {
                locationAddress.setText(alert.getLocation().getAddress());
            } else {
                locationAddress.setText("Location unknown");
            }

            // Set status badge
            if (alert.getStatus() != null) {
                alertStatus.setText(alert.getStatus());
                setStatusColor(alertStatus, alert.getStatus(), context);
            }

            // Handle card click
            alertContainer.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAlertClicked(alert);
                }
            });

            // Handle Respond Now button
            respondNowBtn.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRespondNowClicked(alert);
                }
            });
        }

        private void setStatusColor(TextView statusView, String status, Context context) {
            // Set text color based on status
            if ("ACTIVE".equals(status)) {
                statusView.setTextColor(context.getResources().getColor(R.color.sos_red));
            } else if ("TRIGGERED".equals(status)) {
                statusView.setTextColor(context.getResources().getColor(R.color.sos_red));
            } else if ("CANCELLED".equals(status)) {
                statusView.setTextColor(context.getResources().getColor(R.color.slate_grey));
            } else {
                statusView.setTextColor(context.getResources().getColor(R.color.midnight_blue));
            }
        }

        private String getTimeAgo(long timestamp) {
            long currentTime = System.currentTimeMillis();
            long diff = currentTime - timestamp;

            long seconds = diff / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            long days = hours / 24;

            if (seconds < 60) {
                return "Just now";
            } else if (minutes < 60) {
                return minutes + "m ago";
            } else if (hours < 24) {
                return hours + "h ago";
            } else if (days < 7) {
                return days + "d ago";
            } else {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());
                return sdf.format(timestamp);
            }
        }
    }
}
