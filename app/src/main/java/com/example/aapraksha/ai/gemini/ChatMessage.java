package com.example.aapraksha.ai.gemini;

/**
 * ChatMessage — Data model for AI chatbot messages
 * 
 * Phase 6 Implementation:
 * Represents a single message in a chat session.
 * Can be from user or AI, supports multi-language.
 */
public class ChatMessage {
    
    public enum Sender {
        USER,
        AI
    }

    private String id;              // Unique message ID
    private String text;            // Message content
    private Sender sender;          // USER or AI
    private long timestamp;         // When message was sent
    private String language;        // Language used (en, hi, gu)
    private boolean isLoading;      // For AI "typing" indicator
    private String error;           // Error message if any

    // Full constructor
    public ChatMessage(String id, String text, Sender sender, long timestamp, 
                       String language, boolean isLoading, String error) {
        this.id = id;
        this.text = text;
        this.sender = sender;
        this.timestamp = timestamp;
        this.language = language;
        this.isLoading = isLoading;
        this.error = error;
    }

    // Constructor for user messages
    public ChatMessage(String text, long timestamp) {
        this.id = System.currentTimeMillis() + "_" + (int)(Math.random() * 10000);
        this.text = text;
        this.sender = Sender.USER;
        this.timestamp = timestamp;
        this.language = "en";
        this.isLoading = false;
        this.error = null;
    }

    // Constructor for AI messages with loading state
    public ChatMessage(String text, long timestamp, boolean isLoading) {
        this.id = System.currentTimeMillis() + "_" + (int)(Math.random() * 10000);
        this.text = text;
        this.sender = Sender.AI;
        this.timestamp = timestamp;
        this.language = "en";
        this.isLoading = isLoading;
        this.error = null;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public Sender getSender() { return sender; }
    public void setSender(Sender sender) { this.sender = sender; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public boolean isLoading() { return isLoading; }
    public void setLoading(boolean loading) { isLoading = loading; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    // Convert to Firestore map for storage
    public com.google.firebase.firestore.FieldValue toFirestore() {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("id", id);
        map.put("text", text);
        map.put("sender", sender.toString());
        map.put("timestamp", timestamp);
        map.put("language", language);
        return com.google.firebase.firestore.FieldValue.delete(); // Placeholder
    }

    @Override
    public String toString() {
        return "ChatMessage{" +
                "id='" + id + '\'' +
                ", text='" + text + '\'' +
                ", sender=" + sender +
                ", timestamp=" + timestamp +
                ", language='" + language + '\'' +
                ", isLoading=" + isLoading +
                ", error='" + error + '\'' +
                '}';
    }
}
