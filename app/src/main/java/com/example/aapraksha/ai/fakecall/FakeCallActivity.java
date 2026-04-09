package com.example.aapraksha.ai.fakecall;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.aapraksha.R;

import java.util.Locale;

public class FakeCallActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    public static final String EXTRA_CALLER_NAME = "caller_name";
    private static final long AUTO_END_MS = 60_000L;

    private TextToSpeech textToSpeech;
    private Handler handler;
    private Runnable endCallRunnable;
    private TextView tvCallerName;
    private TextView tvCallStatus;
    private String callerName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        );
        setContentView(R.layout.activity_fake_call);

        callerName = getIntent().getStringExtra(EXTRA_CALLER_NAME);
        if (callerName == null || callerName.trim().isEmpty()) callerName = "Maa";

        tvCallerName = findViewById(R.id.tvCallerName);
        tvCallStatus = findViewById(R.id.tvCallStatus);
        tvCallerName.setText(callerName);

        findViewById(R.id.btnAccept).setOnClickListener(v -> acceptCall());
        findViewById(R.id.btnDecline).setOnClickListener(v -> finish());
        findViewById(R.id.btnEndCall).setOnClickListener(v -> finish());

        handler = new Handler(Looper.getMainLooper());
        endCallRunnable = this::finish;
        handler.postDelayed(endCallRunnable, AUTO_END_MS);

        textToSpeech = new TextToSpeech(this, this);
    }

    private void acceptCall() {
        tvCallStatus.setText("Connected");
        speakScript();
    }

    private void speakScript() {
        if (textToSpeech == null) return;
        String script = "Hey! Where are you? I am already here, just come outside. I am waiting for you.";
        textToSpeech.speak(script, TextToSpeech.QUEUE_FLUSH, null, "fake_call_script");
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = textToSpeech.setLanguage(Locale.US);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Text to speech language not supported", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Text to speech init failed", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null && endCallRunnable != null) {
            handler.removeCallbacks(endCallRunnable);
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }
}

