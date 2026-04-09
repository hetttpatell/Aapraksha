package com.example.aapraksha.ai.gemini;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.example.aapraksha.BuildConfig;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * GeminiChatHelper — AI Chatbot Integration
 * 
 * Phase 6 Implementation:
 * Integrates Gemini 1.5 Flash for multi-language safety conversations.
 * Includes context about danger zones and local safety information.
 * 
 * Usage:
 * ```
 * GeminiChatHelper helper = new GeminiChatHelper(context);
 * helper.chat("Is it safe to walk via CG Road now?", "en", new OnChatResponseListener() {
 *     @Override
 *     public void onResponse(String response) {
 *         // Display response
 *     }
 * });
 * ```
 */
public class GeminiChatHelper {

    private static final String TAG = "GeminiChatHelper";
    private static final String FALLBACK_MODEL_NAME = "gemini-2.0-flash";
    private static final String[] KNOWN_MODELS = new String[]{
            "gemini-2.0-flash",
            "gemini-2.0-flash-lite",
            "gemini-2.0-flash-thinking-exp",
            "gemini-1.5-flash",
            "gemini-1.5-flash-latest",
            "gemini-1.5-pro-latest",
            "gemini-1.5-pro"
    };

    private GenerativeModel generativeModel;
    private GenerativeModelFutures modelFutures;
    private final ExecutorService executorService;
    private final Context context;
    private final String apiKey;
    private final List<String> candidateModels;
    private String selectedLanguage = "en";
    private String configuredModelName = FALLBACK_MODEL_NAME;
    private int currentModelIndex = 0;

    /**
     * Listener for chat responses
     */
    public interface OnChatResponseListener {
        void onResponse(String response);
        void onError(String error);
    }

    public GeminiChatHelper(Context context) {
        this.context = context;
        this.executorService = Executors.newSingleThreadExecutor();

        // Initialize Gemini with API key from BuildConfig
        String buildApiKey = BuildConfig.GEMINI_API_KEY;
        if (TextUtils.isEmpty(buildApiKey) || TextUtils.isEmpty(buildApiKey.trim())) {
            this.apiKey = "";
            this.candidateModels = new ArrayList<>();
            Log.e(TAG, "⚠️ GEMINI_API_KEY not configured in local.properties");
            Log.e(TAG, "Add: GEMINI_API_KEY=your_key_here");
            return;
        }
        this.apiKey = buildApiKey.trim();

        String modelName = BuildConfig.GEMINI_MODEL_NAME;
        if (!TextUtils.isEmpty(modelName) && !TextUtils.isEmpty(modelName.trim())) {
            configuredModelName = modelName.trim();
        }
        this.candidateModels = buildCandidateModels(configuredModelName);

        initializeModelAtIndex(0);
    }

    /**
     * Send a chat message and get AI response
     * 
     * @param userMessage The user's question
     * @param language Language code: "en", "hi", "gu"
     * @param listener Callback for response
     */
    public void chat(String userMessage, String language, OnChatResponseListener listener) {
        if (generativeModel == null) {
            listener.onError("Gemini not initialized. Check API key in local.properties");
            return;
        }

        this.selectedLanguage = language;

        // Build system prompt with context
        String systemPrompt = buildSystemPrompt(language);

        // Create the full prompt
        String fullPrompt = systemPrompt + "\n\nUser: " + userMessage;

        Log.d(TAG, "Sending chat to Gemini...");

        Content prompt = new Content.Builder()
                .addText(fullPrompt)
                .build();

        ListenableFuture<GenerateContentResponse> future = modelFutures.generateContent(prompt);
        Futures.addCallback(future, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String responseText = result != null ? result.getText() : null;
                if (responseText != null && !responseText.isEmpty()) {
                    Log.d(TAG, "✅ Got response: " + responseText.substring(0, Math.min(100, responseText.length())));
                    listener.onResponse(responseText);
                } else {
                    listener.onError("Empty response from AI");
                }
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "Error in chat", t);
                String raw = t != null && t.getMessage() != null ? t.getMessage() : "Unknown error";
                if (isModelNotFoundError(raw) && switchToNextModel()) {
                    chat(userMessage, language, listener);
                    return;
                }
                listener.onError("Error: " + raw);
            }
        }, executorService);
    }

    /**
     * Build system prompt with danger zone context
     */
    private String buildSystemPrompt(String language) {
        String basePrompt;

        switch (language) {
            case "hi":
                basePrompt = "आप Aapraksha ऐप के लिए एक पेशेवर व्यक्तिगत सुरक्षा सलाहकार हैं।\n" +
                        "हमेशा हिंदी में उत्तर दें।\n" +
                        "उत्तर स्पष्ट, शांत, पेशेवर और उपयोगी रखें।\n" +
                        "अनुमान आधारित दावे न करें।\n" +
                        "तुरंत खतरे की स्थिति में स्थानीय आपातकालीन सेवा (पुलिस: 100) को कॉल करने की सलाह दें।";
                break;
            case "gu":
                basePrompt = "તમે Aapraksha એપ માટે એક વ્યાવસાયિક વ્યક્તિગત સુરક્ષા સલાહકાર છો.\n" +
                        "હંમેશા ગુજરાતીમાં જવાબ આપો.\n" +
                        "જવાબ સંક્ષિપ્ત, સ્પષ્ટ, શાંત અને વ્યવહારુ રાખો.\n" +
                        "અંદાજ પર આધારિત દાવા ન કરો.\n" +
                        "તાત્કાલિક જોખમ હોય તો સ્થાનિક ઇમરજન્સી સેવા (પોલીસ: 100) સંપર્ક કરવાની સલાહ આપો.";
                break;
            default: // English
                basePrompt = "You are a professional personal safety advisor integrated into the Aapraksha safety app.\n" +
                        "Respond in English.\n" +
                        "Use a professional, calm, actionable tone.\n" +
                        "Keep answers concise and structured with practical steps.\n" +
                        "Do not fabricate facts or claim access to data you do not have.\n" +
                        "If there is immediate danger, instruct the user to call emergency services (Police: 100).\n";
        }

        // Add response quality and safety context.
        basePrompt += "\n\nCurrent Safety Context:\n" +
                "- User is using the Aapraksha safety app.\n" +
                "- Provide localized safety information.\n" +
                "- Consider recent safety incidents in user's area.\n" +
                "- Recommend safe routes when asked.\n" +
                "- Prefer this response format: Risk Level, Why, Immediate Actions, Next Safe Step.\n";

        return basePrompt;
    }

    /**
     * Set language preference
     */
    public void setLanguage(String language) {
        this.selectedLanguage = language;
        Log.d(TAG, "Language set to: " + language);
    }

    /**
     * Get current language
     */
    public String getLanguage() {
        return selectedLanguage;
    }

    /**
     * Check if Gemini is properly initialized
     */
    public boolean isInitialized() {
        return generativeModel != null;
    }

    /**
     * Shutdown executor service
     */
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            Log.d(TAG, "Executor service shut down");
        }
    }

    /**
     * Test connection to Gemini API
     */
    public void testConnection(OnChatResponseListener listener) {
        if (generativeModel == null || modelFutures == null) {
            listener.onError("Gemini not initialized. Check API key in local.properties");
            return;
        }

        Content prompt = new Content.Builder()
                .addText("Respond with exactly one word: OK")
                .build();

        ListenableFuture<GenerateContentResponse> future = modelFutures.generateContent(prompt);
        Futures.addCallback(future, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String responseText = result != null ? result.getText() : null;
                if (TextUtils.isEmpty(responseText)) {
                    listener.onError("Empty response from AI");
                    return;
                }
                listener.onResponse(responseText);
            }

            @Override
            public void onFailure(Throwable t) {
                String raw = t != null && t.getMessage() != null ? t.getMessage() : "Unknown error";
                String normalized = raw.toLowerCase();
                if (isModelNotFoundError(raw) && switchToNextModel()) {
                    testConnection(listener);
                    return;
                }
                if (normalized.contains("404") || normalized.contains("not found")) {
                    listener.onError("Model not found: " + configuredModelName + " (" + raw + ")");
                    return;
                }
                if (normalized.contains("401") || normalized.contains("unauthorized") || normalized.contains("api key")) {
                    listener.onError("Unauthorized Gemini API request (" + raw + ")");
                    return;
                }
                if (normalized.contains("429") || normalized.contains("quota") || normalized.contains("rate")) {
                    listener.onError("Gemini quota/rate limit reached (" + raw + ")");
                    return;
                }
                listener.onError(raw);
            }
        }, executorService);
    }

    private synchronized boolean switchToNextModel() {
        int nextIndex = currentModelIndex + 1;
        while (nextIndex < candidateModels.size()) {
            if (initializeModelAtIndex(nextIndex)) {
                return true;
            }
            nextIndex++;
        }
        return false;
    }

    private synchronized boolean initializeModelAtIndex(int index) {
        if (index < 0 || index >= candidateModels.size()) {
            return false;
        }

        String model = candidateModels.get(index);
        try {
            GenerativeModel newModel = new GenerativeModel(model, apiKey);
            GenerativeModelFutures newFutures = GenerativeModelFutures.from(newModel);
            this.generativeModel = newModel;
            this.modelFutures = newFutures;
            this.configuredModelName = model;
            this.currentModelIndex = index;
            Log.d(TAG, "✅ Gemini initialized for chatbot, model: " + model);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Gemini model: " + model, e);
            return false;
        }
    }

    private boolean isModelNotFoundError(String error) {
        if (TextUtils.isEmpty(error)) {
            return false;
        }
        String normalized = error.toLowerCase(Locale.US);
        return normalized.contains("404")
                || (normalized.contains("not found") && normalized.contains("model"));
    }

    private List<String> buildCandidateModels(String preferredModel) {
        Set<String> unique = new LinkedHashSet<>();
        String normalizedPreferredModel = normalizeModelName(preferredModel);
        if (!TextUtils.isEmpty(normalizedPreferredModel)) {
            unique.add(normalizedPreferredModel);
        }
        unique.add(FALLBACK_MODEL_NAME);
        for (String model : KNOWN_MODELS) {
            String normalizedModel = normalizeModelName(model);
            if (!TextUtils.isEmpty(normalizedModel)) {
                unique.add(normalizedModel);
            }
        }
        return new ArrayList<>(unique);
    }

    private String normalizeModelName(String modelName) {
        if (TextUtils.isEmpty(modelName)) {
            return "";
        }
        String normalized = modelName.trim();
        if (normalized.startsWith("models/")) {
            normalized = normalized.substring("models/".length());
        }
        return normalized;
    }
}
