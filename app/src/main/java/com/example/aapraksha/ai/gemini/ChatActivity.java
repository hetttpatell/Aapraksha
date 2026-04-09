package com.example.aapraksha.ai.gemini;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aapraksha.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * ChatActivity — AI Safety Assistant Chatbot
 * 
 * Phase 6 Implementation:
 * Conversational AI interface for safety-related questions.
 * Supports English, Hindi, and Gujarati.
 * 
 * Features:
 * - Multi-language support (en, hi, gu)
 * - Message history with timestamps
 * - Loading indicator for AI thinking
 * - Real-time message streaming
 * - Gemini 1.5 Flash integration
 * - Safety context awareness
 */
public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";
    private static final int REQ_VOICE_INPUT = 1007;

    // UI Components
    private RecyclerView rvChat;
    private EditText etMessage;
    private ImageButton btnSend;
    private ImageButton btnVoiceInput;
    private Spinner spinnerLanguage;
    private ImageView btnBack;
    private TextView tvOnlineStatus;

    // Chat
    private ChatAdapter chatAdapter;
    private List<ChatMessage> messages;
    private GeminiChatHelper geminiHelper;
    private String currentLanguage = "en";
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        initializeViews();
        setupRecyclerView();
        setupGemini();
        setupListeners();
        addWelcomeMessage();
    }

    private void initializeViews() {
        rvChat = findViewById(R.id.rvChat);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnVoiceInput = findViewById(R.id.btnVoiceInput);
        spinnerLanguage = findViewById(R.id.spinnerLanguage);
        btnBack = findViewById(R.id.btnBack);
        tvOnlineStatus = findViewById(R.id.tvOnlineStatus);

        messages = new ArrayList<>();
    }

    private void setupRecyclerView() {
        chatAdapter = new ChatAdapter(messages);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // Start from bottom
        rvChat.setLayoutManager(layoutManager);
        rvChat.setAdapter(chatAdapter);
    }

    private void setupGemini() {
        geminiHelper = new GeminiChatHelper(this);
        
        if (!geminiHelper.isInitialized()) {
            Log.e(TAG, "Gemini not initialized!");
            Toast.makeText(this, "⚠️ Check GEMINI_API_KEY in local.properties", Toast.LENGTH_LONG).show();
            tvOnlineStatus.setText("● Offline (API key missing)");
            tvOnlineStatus.setTextColor(getResources().getColor(R.color.neon_crimson));
        } else {
            Log.d(TAG, "✅ Gemini initialized");
            // Test connection
            geminiHelper.testConnection(new GeminiChatHelper.OnChatResponseListener() {
                @Override
                public void onResponse(String response) {
                    Log.d(TAG, "✅ Connected to Gemini");
                    runOnUiThread(() -> {
                        tvOnlineStatus.setText("● Online");
                        tvOnlineStatus.setTextColor(getResources().getColor(R.color.electric_indigo_light));
                    });
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "Connection error: " + error);
                    runOnUiThread(() -> {
                        String normalized = error == null ? "" : error.toLowerCase(Locale.US);
                        if (normalized.contains("model") && normalized.contains("not found")) {
                            tvOnlineStatus.setText("● Model Unavailable");
                        } else if (normalized.contains("unauthorized") || normalized.contains("api key") || normalized.contains("401")) {
                            tvOnlineStatus.setText("● Config Error");
                        } else {
                            tvOnlineStatus.setText("● Connection Error");
                        }
                        tvOnlineStatus.setTextColor(getResources().getColor(R.color.neon_crimson));
                    });
                }
            });
        }
    }

    private void setupListeners() {
        // Send button
        btnSend.setOnClickListener(v -> sendMessage());
        btnVoiceInput.setOnClickListener(v -> startVoiceInput());

        // Back button
        btnBack.setOnClickListener(v -> finish());

        // Language selector
        spinnerLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] languages = {"en", "hi", "gu"};
                currentLanguage = languages[position];
                geminiHelper.setLanguage(currentLanguage);
                Log.d(TAG, "Language switched to: " + currentLanguage);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Message input
        etMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });
    }

    private void sendMessage() {
        if (!isNetworkConnected()) {
            Toast.makeText(this, "No internet connection. Please check your network and try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        String userMessage = etMessage.getText().toString().trim();
        
        if (userMessage.isEmpty()) {
            Toast.makeText(this, "Please type a message", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isLoading) {
            Toast.makeText(this, "Waiting for AI response...", Toast.LENGTH_SHORT).show();
            return;
        }

        // Add user message to chat
        ChatMessage userMsg = new ChatMessage(userMessage, System.currentTimeMillis());
        chatAdapter.addMessage(userMsg);
        rvChat.scrollToPosition(messages.size() - 1);

        // Clear input
        etMessage.setText("");
        etMessage.clearFocus();

        // Add loading indicator
        ChatMessage loadingMsg = new ChatMessage("", System.currentTimeMillis(), true);
        chatAdapter.addMessage(loadingMsg);
        rvChat.scrollToPosition(messages.size() - 1);

        isLoading = true;
        btnSend.setEnabled(false);

        // Get AI response
        geminiHelper.chat(userMessage, currentLanguage, new GeminiChatHelper.OnChatResponseListener() {
            @Override
            public void onResponse(String response) {
                runOnUiThread(() -> {
                    // Remove loading message
                    chatAdapter.removeLastMessage();

                    // Add AI response
                    ChatMessage aiMsg = new ChatMessage(response, System.currentTimeMillis());
                    aiMsg.setSender(ChatMessage.Sender.AI);
                    aiMsg.setLanguage(currentLanguage);
                    chatAdapter.addMessage(aiMsg);
                    rvChat.scrollToPosition(messages.size() - 1);

                    isLoading = false;
                    btnSend.setEnabled(true);

                    Log.d(TAG, "✅ AI response displayed");
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    // Remove loading message
                    chatAdapter.removeLastMessage();

                    // Add error message
                    String displayMessage = formatErrorForDisplay(error);
                    ChatMessage errorMsg = new ChatMessage(
                            displayMessage,
                            System.currentTimeMillis()
                    );
                    errorMsg.setSender(ChatMessage.Sender.AI);
                    errorMsg.setError(error);
                    chatAdapter.addMessage(errorMsg);
                    rvChat.scrollToPosition(messages.size() - 1);

                    isLoading = false;
                    btnSend.setEnabled(true);

                    Log.e(TAG, "❌ Error: " + error);
                });
            }
        });
    }

    private void startVoiceInput() {
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLanguage);
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your safety question");
            startActivityForResult(intent, REQ_VOICE_INPUT);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Voice input is not available on this device.", Toast.LENGTH_SHORT).show();
        }
    }

    private String formatErrorForDisplay(String error) {
        if (error == null || error.trim().isEmpty()) {
            return "Sorry, I couldn't complete that request right now. Please try again.";
        }

        String normalized = error.toLowerCase(Locale.US);

        if ((normalized.contains("404") && normalized.contains("model"))
                || (normalized.contains("model") && normalized.contains("not found"))) {
            return "The AI model isn't available right now. Please try again later.";
        }
        if (normalized.contains("401") || normalized.contains("api key") || normalized.contains("unauthorized") || normalized.contains("permission")) {
            return "The AI service isn't configured. Please check the Gemini API key setup.";
        }
        if (normalized.contains("429") || normalized.contains("quota") || normalized.contains("rate limit")) {
            return "The AI service is busy right now. Please try again in a moment.";
        }
        if (normalized.contains("timeout") || normalized.contains("timed out")) {
            return "The AI service took too long to respond. Please try again.";
        }
        if (normalized.contains("unable to resolve host") || normalized.contains("failed to connect") || normalized.contains("network")) {
            return "I couldn't reach the AI service. Please check your internet connection.";
        }

        return "Sorry, I couldn't complete that request right now. Please try again.";
    }

    private boolean isNetworkConnected() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return false;
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            Network activeNetwork = connectivityManager.getActiveNetwork();
            if (activeNetwork == null) {
                return false;
            }
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
            return capabilities != null && (
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                            || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                            || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            );
        }

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    private void addWelcomeMessage() {
        String welcomeText = "👋 Hi! I'm your Aapraksha Safety Assistant.\n\n" +
                "I can help you with:\n" +
                "🗺️ Safety information about areas\n" +
                "⚠️ Emergency tips and advice\n" +
                "🚨 What to do in dangerous situations\n" +
                "📍 Safe route suggestions\n\n" +
                "Ask me anything related to your safety!";

        ChatMessage welcomeMsg = new ChatMessage(welcomeText, System.currentTimeMillis());
        welcomeMsg.setSender(ChatMessage.Sender.AI);
        chatAdapter.addMessage(welcomeMsg);
        rvChat.scrollToPosition(messages.size() - 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_VOICE_INPUT || resultCode != RESULT_OK || data == null) {
            return;
        }
        ArrayList<String> spokenResults = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
        if (spokenResults == null || spokenResults.isEmpty()) {
            return;
        }
        etMessage.setText(spokenResults.get(0));
        etMessage.setSelection(etMessage.getText().length());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (geminiHelper != null) {
            geminiHelper.shutdown();
        }
    }
}
