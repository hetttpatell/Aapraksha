package com.example.aapraksha;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * HistoryListAdapter for displaying user's historical SOS alerts
 * Uses item_history_card.xml layout matching the original activity_history.xml design
 */
public class HistoryListAdapter extends RecyclerView.Adapter<HistoryListAdapter.ViewHolder> {
    private List<SOSAlert> alerts;
    private Context context;
    private OnHistoryItemClickListener listener;

    public interface OnHistoryItemClickListener {
        void onViewDetailsClicked(SOSAlert alert);
        void onShareClicked(SOSAlert alert);
    }

    public HistoryListAdapter(Context context, List<SOSAlert> alerts, OnHistoryItemClickListener listener) {
        this.context = context;
        this.alerts = alerts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_history_card, parent, false);
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
        private TextView alertTime;
        private TextView alertLocation;
        private TextView alertStatus;
        private TextView alertType;
        private TextView alertCity;
        private ImageView mapPreview;
        private View viewDetailsBtn;
        private CardView shareBtn;
        private CardView historyContainer;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            alertTime = itemView.findViewById(R.id.alert_time);
            alertLocation = itemView.findViewById(R.id.alert_location);
            alertStatus = itemView.findViewById(R.id.alert_status);
            alertType = itemView.findViewById(R.id.alert_type);
            alertCity = itemView.findViewById(R.id.alert_city);
            mapPreview = itemView.findViewById(R.id.map_preview);
            viewDetailsBtn = itemView.findViewById(R.id.view_details_btn);
            shareBtn = itemView.findViewById(R.id.share_btn);
            historyContainer = itemView.findViewById(R.id.history_container);
        }

        public void bind(SOSAlert alert, OnHistoryItemClickListener listener, Context context) {
            // Set time - always show something meaningful
            if (alert.getCreatedAt() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault());
                alertTime.setText("Triggered: " + sdf.format(alert.getCreatedAt().toDate()));
            } else {
                alertTime.setText("Triggered time unknown");
            }

            // Set location
            if (alert.getLocation() != null && alert.getLocation().getAddress() != null) {
                alertLocation.setText(alert.getLocation().getAddress());
                // Extract city name for the map overlay
                String address = alert.getLocation().getAddress();
                String city = extractCity(address);
                alertCity.setText(city);
            } else {
                alertLocation.setText("Location unknown");
                alertCity.setText("");
            }

            // Set alert type
            if (alert.getAlertType() != null) {
                if ("SOS".equals(alert.getAlertType()) || "EMERGENCY_SOS".equals(alert.getAlertType())) {
                    alertType.setText("SOS Alert");
                } else if ("CHECK_IN".equals(alert.getAlertType())) {
                    alertType.setText("Check-in");
                } else {
                    alertType.setText(alert.getAlertType());
                }
            } else {
                alertType.setText("Alert");
            }

            // Set status badge - show Active / Inactive
            String displayStatus = alert.getStatusDisplayText();
            alertStatus.setText(displayStatus);
            setStatusStyle(alertStatus, alert, context);

            // Load Map Preview using Yandex Static Maps (free, no API key required)
            if (alert.getLocation() != null && 
                (alert.getLocation().getLatitude() != 0 || alert.getLocation().getLongitude() != 0)) {
                double lat = alert.getLocation().getLatitude();
                double lng = alert.getLocation().getLongitude();
                
                String mapUrl = String.format(Locale.US, 
                    "https://static-maps.yandex.ru/1.x/?lang=en_US&ll=%f,%f&z=13&l=map&size=450,250&pt=%f,%f,pm2rdm",
                    lng, lat, lng, lat);
                
                Glide.with(context)
                    .load(mapUrl)
                    .apply(new RequestOptions()
                        .transforms(new CenterCrop()))
                    .placeholder(R.color.input_background_dark)
                    .error(R.color.surface_dark)
                    .into(mapPreview);
            } else {
                mapPreview.setImageResource(R.color.surface_dark);
            }

            // View Details button
            viewDetailsBtn.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onViewDetailsClicked(alert);
                }
            });

            // Share button
            shareBtn.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onShareClicked(alert);
                }
            });
        }

        private String extractCity(String address) {
            if (address == null || address.isEmpty()) return "";
            // Try to get a meaningful location name from the address
            String[] parts = address.split(",");
            if (parts.length >= 2) {
                return parts[parts.length - 2].trim();
            } else if (parts.length == 1) {
                return parts[0].trim();
            }
            return address;
        }

        private void setStatusStyle(TextView statusView, SOSAlert alert, Context context) {
            if (alert.isActive()) {
                statusView.setTextColor(context.getResources().getColor(R.color.sos_red));
                View badgeContainer = statusView.getRootView().findViewById(R.id.status_badge_container);
                if (badgeContainer instanceof CardView) {
                    ((CardView) badgeContainer).setCardBackgroundColor(0x33FF1744);
                }
            } else {
                statusView.setTextColor(context.getResources().getColor(R.color.slate_grey));
                View badgeContainer = statusView.getRootView().findViewById(R.id.status_badge_container);
                if (badgeContainer instanceof CardView) {
                    ((CardView) badgeContainer).setCardBackgroundColor(0x1A94A3B8);
                }
            }
        }
    }
}
