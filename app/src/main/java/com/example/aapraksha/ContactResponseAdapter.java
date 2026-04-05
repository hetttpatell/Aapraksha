package com.example.aapraksha;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * ContactResponseAdapter for displaying contact responses in alert details
 */
public class ContactResponseAdapter extends RecyclerView.Adapter<ContactResponseAdapter.ViewHolder> {
    private List<AlertHistory.ContactNotificationInfo.NotificationDetail> responses;
    private Context context;

    public ContactResponseAdapter(Context context, List<AlertHistory.ContactNotificationInfo.NotificationDetail> responses) {
        this.context = context;
        this.responses = responses;
    }

    public void updateResponses(List<AlertHistory.ContactNotificationInfo.NotificationDetail> newResponses) {
        this.responses = newResponses;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_contact_response, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AlertHistory.ContactNotificationInfo.NotificationDetail response = responses.get(position);
        holder.bind(response, context);
    }

    @Override
    public int getItemCount() {
        return responses.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView contactName;
        private TextView responseStatus;
        private ImageView statusIcon;
        private View statusDot;
        private ImageView contactPhoto;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            contactName = itemView.findViewById(R.id.contact_name);
            responseStatus = itemView.findViewById(R.id.response_status);
            statusIcon = itemView.findViewById(R.id.status_icon);
            statusDot = itemView.findViewById(R.id.status_dot);
            contactPhoto = itemView.findViewById(R.id.contact_photo);
        }

        public void bind(AlertHistory.ContactNotificationInfo.NotificationDetail response, Context context) {
            // Set contact name
            if (response.getContactName() != null) {
                contactName.setText(response.getContactName());
            }

            if (response.getResponseStatus() != null) {
                String statusText = response.getResponseStatus();
                if ("REACHED".equals(statusText) || "ACKNOWLEDGED".equals(statusText)) {
                    statusDot.setVisibility(View.VISIBLE);
                } else {
                    statusDot.setVisibility(View.GONE);
                }
                
                if (response.getNotifiedAt() != null && response.getResponseTime() > 0) {
                    statusText += " - " + formatResponseTime(response.getResponseTime());
                }
                responseStatus.setText(statusText);
            }

            // Set icon based on response status
            if (response.getResponseStatus() != null) {
                setStatusIcon(response.getResponseStatus(), context);
            }
        }

        private void setStatusIcon(String status, Context context) {
            if ("REACHED".equals(status)) {
                statusIcon.setImageResource(R.drawable.ic_call);
                statusIcon.setColorFilter(context.getResources().getColor(R.color.electric_indigo));
            } else if ("CALLING".equals(status)) {
                statusIcon.setImageResource(R.drawable.ic_phone);
                statusIcon.setColorFilter(context.getResources().getColor(R.color.electric_indigo));
            } else if ("ACKNOWLEDGED".equals(status)) {
                statusIcon.setImageResource(R.drawable.ic_notification);
                statusIcon.setColorFilter(context.getResources().getColor(R.color.electric_indigo));
            } else {
                statusIcon.setImageResource(R.drawable.ic_info);
                statusIcon.setColorFilter(context.getResources().getColor(R.color.slate_grey));
            }
        }

        private String formatResponseTime(int seconds) {
            int minutes = seconds / 60;
            if (minutes < 1) {
                return "Few seconds ago";
            } else if (minutes < 60) {
                return minutes + " min";
            } else {
                int hours = minutes / 60;
                return hours + "h " + (minutes % 60) + "m";
            }
        }
    }
}
