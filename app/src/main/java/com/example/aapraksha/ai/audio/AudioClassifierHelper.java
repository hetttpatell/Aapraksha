package com.example.aapraksha.ai.audio;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import org.tensorflow.lite.task.audio.classifier.AudioClassifier;
import org.tensorflow.lite.task.audio.classifier.Classifications;
import org.tensorflow.lite.task.audio.classifier.AudioClassifier.AudioClassifierOptions;
import org.tensorflow.lite.support.label.Category;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AudioClassifierHelper {
    private static final String TAG = "AudioClassifierHelper";
    private static final String MODEL_FILE = "yamnet.tflite";

    // Target threats: Scream, Glass, Gunshot, Explosion
    private static final Set<String> THREAT_LABELS = new HashSet<>(Arrays.asList(
            "Screaming", "Explosion", "Gunshot, gunfire", "Glass"
    ));
    
    // YAMNet requires 0.975 seconds of 16kHz audio = 15600 samples
    private static final int SAMPLE_RATE = 16000;
    private static final float THREAT_THRESHOLD = 0.40f; 

    private AudioClassifier classifier;
    private AudioRecord audioRecord;
    private boolean isRecording = false;

    public AudioClassifierHelper(Context context) {
        try {
            AudioClassifierOptions options = AudioClassifierOptions.builder()
                    .setMaxResults(5) // Get top 5 predictions
                    .build();
            classifier = AudioClassifier.createFromFileAndOptions(context, MODEL_FILE, options);
            Log.d(TAG, "Audio Classifier initialized successfully");
        } catch (IOException e) {
            Log.e(TAG, "Failed to initialize Audio Classifier: " + e.getMessage());
        }
    }

    public void startListening(AudioDetectionListener listener) {
        if (classifier == null) {
            Log.e(TAG, "Classifier is null, cannot start listening");
            return;
        }

        int bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
        );

        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            bufferSize = SAMPLE_RATE * 2; // Fallback
        }

        // We use the AudioClassifier's createInputTensorAudio matching the model's specs
        org.tensorflow.lite.support.audio.TensorAudio tensorAudio = classifier.createInputTensorAudio();

        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                tensorAudio.getFormat().getSampleRate(),
                tensorAudio.getFormat().getChannels() == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
        );

        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord failed to initialize");
            return;
        }

        audioRecord.startRecording();
        isRecording = true;

        new Thread(() -> {
            while (isRecording && audioRecord != null) {
                // tensorAudio object loads a chunk of audio
                tensorAudio.load(audioRecord);
                
                // Run inference
                List<Classifications> output = classifier.classify(tensorAudio);

                // Check for threats
                if (output != null && !output.isEmpty()) {
                    for (Category category : output.get(0).getCategories()) {
                        String label = category.getLabel();
                        float score = category.getScore();

                        // Log high confidence results for debugging
                        if (score > 0.1f) {
                            Log.d(TAG, "Detected: " + label + " (" + score + ")");
                        }

                        if (THREAT_LABELS.contains(label) && score >= THREAT_THRESHOLD) {
                            Log.w(TAG, "🚨 THREAT DETECTED 🚨 : " + label + " Score: " + score);
                            if (listener != null) {
                                listener.onThreatDetected(label, score);
                            }
                            // To prevent spamming, we can break or sleep
                            try {
                                Thread.sleep(5000); 
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }).start();
    }

    public void stopListening() {
        isRecording = false;
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
    }

    public interface AudioDetectionListener {
        void onThreatDetected(String threatType, float confidence);
    }
}
