package com.example.aapraksha.ai.gemini;

import android.content.Context;
import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ChatRepository — Firestore Integration for Chat Sessions
 * 
 * Phase 6 Implementation:
 * Stores chat sessions and messages in Firestore for:
 * - Chat history persistence
 * - Multi-device sync
 * - Analytics and pattern analysis
 * 
 * Firestore Structure:
 * chat_sessions/{sessionId}
 *   - userId: String
 *   - language: String
 *   - createdAt: Timestamp
 *   - updatedAt: Timestamp
 *   - messageCount: Number
 *   - messages/{messageId}
 *     - text: String
 *     - sender: String ("USER" | "AI")
 *     - timestamp: Timestamp
 *     - language: String
 */
public class ChatRepository {

    private static final String TAG = "ChatRepository";
    private static final String COLLECTION_CHAT_SESSIONS = "chat_sessions";
    private static final String SUBCOLLECTION_MESSAGES = "messages";

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    /**
     * Callback for chat session operations
     */
    public interface OnChatSessionListener {
        void onSuccess(String sessionId);
        void onError(String error);
    }

    /**
     * Callback for message operations
     */
    public interface OnMessageListener {
        void onSuccess();
        void onError(String error);
    }

    /**
     * Callback for loading messages
     */
    public interface OnMessagesLoadedListener {
        void onLoaded(List<ChatMessage> messages);
        void onError(String error);
    }

    public ChatRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    /**
     * Create a new chat session
     */
    public void createChatSession(String language, OnChatSessionListener listener) {
        if (auth.getCurrentUser() == null) {
            listener.onError("Not authenticated");
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        Map<String, Object> session = new HashMap<>();
        session.put("userId", userId);
        session.put("language", language);
        session.put("createdAt", Timestamp.now());
        session.put("updatedAt", Timestamp.now());
        session.put("messageCount", 0);

        db.collection(COLLECTION_CHAT_SESSIONS)
                .add(session)
                .addOnSuccessListener(docRef -> {
                    Log.d(TAG, "✅ Chat session created: " + docRef.getId());
                    listener.onSuccess(docRef.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error creating session", e);
                    listener.onError(e.getMessage());
                });
    }

    /**
     * Save a message to a chat session
     */
    public void saveMessage(String sessionId, ChatMessage message, OnMessageListener listener) {
        if (auth.getCurrentUser() == null) {
            listener.onError("Not authenticated");
            return;
        }

        Map<String, Object> messageData = new HashMap<>();
        messageData.put("text", message.getText());
        messageData.put("sender", message.getSender().toString());
        messageData.put("timestamp", Timestamp.now());
        messageData.put("language", message.getLanguage());

        db.collection(COLLECTION_CHAT_SESSIONS)
                .document(sessionId)
                .collection(SUBCOLLECTION_MESSAGES)
                .add(messageData)
                .addOnSuccessListener(docRef -> {
                    // Update message count
                    updateSessionMetadata(sessionId);
                    Log.d(TAG, "✅ Message saved: " + docRef.getId());
                    listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error saving message", e);
                    listener.onError(e.getMessage());
                });
    }

    /**
     * Load all messages from a session
     */
    public void loadMessages(String sessionId, OnMessagesLoadedListener listener) {
        if (auth.getCurrentUser() == null) {
            listener.onError("Not authenticated");
            return;
        }

        db.collection(COLLECTION_CHAT_SESSIONS)
                .document(sessionId)
                .collection(SUBCOLLECTION_MESSAGES)
                .orderBy("timestamp")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<ChatMessage> messages = new ArrayList<>();
                    
                    for (var doc : querySnapshot.getDocuments()) {
                        String text = doc.getString("text");
                        String senderStr = doc.getString("sender");
                        String language = doc.getString("language");
                        Long timestamp = doc.getLong("timestamp");

                        ChatMessage.Sender sender = ChatMessage.Sender.USER;
                        if ("AI".equals(senderStr)) {
                            sender = ChatMessage.Sender.AI;
                        }

                        ChatMessage message = new ChatMessage(
                                text,
                                timestamp != null ? timestamp : System.currentTimeMillis()
                        );
                        message.setSender(sender);
                        message.setLanguage(language != null ? language : "en");
                        messages.add(message);
                    }

                    Log.d(TAG, "✅ Loaded " + messages.size() + " messages");
                    listener.onLoaded(messages);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error loading messages", e);
                    listener.onError(e.getMessage());
                });
    }

    /**
     * Update session metadata (message count, last update time)
     */
    private void updateSessionMetadata(String sessionId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("updatedAt", Timestamp.now());
        updates.put("messageCount", FieldValue.increment(1));

        db.collection(COLLECTION_CHAT_SESSIONS)
                .document(sessionId)
                .update(updates)
                .addOnFailureListener(e -> Log.e(TAG, "Error updating metadata", e));
    }

    /**
     * Delete a chat session (and all its messages)
     */
    public void deleteSession(String sessionId, OnMessageListener listener) {
        if (auth.getCurrentUser() == null) {
            listener.onError("Not authenticated");
            return;
        }

        // Delete all messages first
        db.collection(COLLECTION_CHAT_SESSIONS)
                .document(sessionId)
                .collection(SUBCOLLECTION_MESSAGES)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (var doc : querySnapshot.getDocuments()) {
                        doc.getReference().delete();
                    }

                    // Then delete the session
                    db.collection(COLLECTION_CHAT_SESSIONS)
                            .document(sessionId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "✅ Session deleted");
                                listener.onSuccess();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "❌ Error deleting session", e);
                                listener.onError(e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error loading messages for deletion", e);
                    listener.onError(e.getMessage());
                });
    }

    /**
     * Get all chat sessions for current user
     */
    public interface OnSessionsLoadedListener {
        void onLoaded(List<String> sessionIds);
        void onError(String error);
    }

    public void getAllSessions(OnSessionsLoadedListener listener) {
        if (auth.getCurrentUser() == null) {
            listener.onError("Not authenticated");
            return;
        }

        String userId = auth.getCurrentUser().getUid();

        db.collection(COLLECTION_CHAT_SESSIONS)
                .whereEqualTo("userId", userId)
                .orderBy("createdAt")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> sessionIds = new ArrayList<>();
                    for (var doc : querySnapshot.getDocuments()) {
                        sessionIds.add(doc.getId());
                    }
                    Log.d(TAG, "✅ Loaded " + sessionIds.size() + " sessions");
                    listener.onLoaded(sessionIds);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error loading sessions", e);
                    listener.onError(e.getMessage());
                });
    }
}
