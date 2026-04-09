package com.example.aapraksha.ai.gemini;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aapraksha.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * ChatAdapter — RecyclerView adapter for chat messages
 * 
 * Phase 6 Implementation:
 * Displays chat messages with different styling for user vs AI.
 * Handles loading state for AI responses.
 */
public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private List<ChatMessage> messages;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm", Locale.getDefault());

    private static final int VIEW_TYPE_USER = 0;
    private static final int VIEW_TYPE_AI = 1;
    private static final int VIEW_TYPE_AI_LOADING = 2;

    public ChatAdapter(List<ChatMessage> messages) {
        this.messages = messages;
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messages.get(position);
        
        if (message.getSender() == ChatMessage.Sender.USER) {
            return VIEW_TYPE_USER;
        } else if (message.isLoading()) {
            return VIEW_TYPE_AI_LOADING;
        } else {
            return VIEW_TYPE_AI;
        }
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        
        if (viewType == VIEW_TYPE_USER) {
            View view = inflater.inflate(R.layout.item_chat_message_user, parent, false);
            return new UserMessageViewHolder(view);
        } else if (viewType == VIEW_TYPE_AI_LOADING) {
            View view = inflater.inflate(R.layout.item_chat_message_ai_loading, parent, false);
            return new AILoadingViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_chat_message_ai, parent, false);
            return new AIMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        holder.bind(message);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void addMessage(ChatMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    public void updateLastMessage(String text) {
        if (!messages.isEmpty()) {
            ChatMessage last = messages.get(messages.size() - 1);
            last.setText(text);
            last.setLoading(false);
            notifyItemChanged(messages.size() - 1);
        }
    }

    public void removeLastMessage() {
        if (!messages.isEmpty()) {
            messages.remove(messages.size() - 1);
            notifyItemRemoved(messages.size());
        }
    }

    // Base ViewHolder
    abstract static class ChatViewHolder extends RecyclerView.ViewHolder {
        ChatViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        abstract void bind(ChatMessage message);
    }

    // User message ViewHolder
    static class UserMessageViewHolder extends ChatViewHolder {
        private TextView tvMessage;
        private TextView tvTime;

        UserMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tv_message_text);
            tvTime = itemView.findViewById(R.id.tv_message_time);
        }

        @Override
        void bind(ChatMessage message) {
            tvMessage.setText(message.getText());
            tvTime.setText(formatTime(message.getTimestamp()));
        }

        private String formatTime(long timestamp) {
            return new SimpleDateFormat("hh:mm a", Locale.getDefault())
                    .format(new Date(timestamp));
        }
    }

    // AI message ViewHolder
    static class AIMessageViewHolder extends ChatViewHolder {
        private TextView tvMessage;
        private TextView tvTime;

        AIMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tv_message_text);
            tvTime = itemView.findViewById(R.id.tv_message_time);
        }

        @Override
        void bind(ChatMessage message) {
            tvMessage.setText(message.getText());
            tvTime.setText(formatTime(message.getTimestamp()));
        }

        private String formatTime(long timestamp) {
            return new SimpleDateFormat("hh:mm a", Locale.getDefault())
                    .format(new Date(timestamp));
        }
    }

    // AI Loading ViewHolder
    static class AILoadingViewHolder extends ChatViewHolder {
        private ProgressBar progressBar;
        private TextView tvStatus;

        AILoadingViewHolder(@NonNull View itemView) {
            super(itemView);
            progressBar = itemView.findViewById(R.id.progress_bar);
            tvStatus = itemView.findViewById(R.id.tv_status);
        }

        @Override
        void bind(ChatMessage message) {
            tvStatus.setText("AI is thinking...");
        }
    }
}
