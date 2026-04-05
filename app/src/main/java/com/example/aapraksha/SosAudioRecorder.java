package com.example.aapraksha;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.io.IOException;

/**
 * SosAudioRecorder — Records ~5 seconds of ambient audio after an SOS trigger.
 *
 * The audio is saved as an .m4a file in the app's cache directory.
 * After recording completes, the caller receives a callback with the file path
 * for upload to Firebase Storage.
 */
public class SosAudioRecorder {

    private static final String TAG = "SosAudioRecorder";
    public static final int RECORDING_DURATION_SECONDS = 5;
    private static final int RECORDING_DURATION_MS = RECORDING_DURATION_SECONDS * 1000;

    private MediaRecorder mediaRecorder;
    private String outputFilePath;
    private boolean isRecording = false;
    private Handler handler;
    private OnRecordingCompleteListener listener;

    public interface OnRecordingCompleteListener {
        void onRecordingComplete(String filePath, long fileSizeBytes);
        void onRecordingFailed(String error);
    }

    public SosAudioRecorder() {
        handler = new Handler(Looper.getMainLooper());
    }

    /**
     * Start recording ambient audio for 5 seconds.
     *
     * @param context  Application context
     * @param alertId  Alert ID (used in the filename)
     * @param listener Callback for completion/failure
     */
    public void startRecording(Context context, String alertId, OnRecordingCompleteListener listener) {
        this.listener = listener;

        if (isRecording) {
            Log.w(TAG, "Already recording — ignoring start request");
            return;
        }

        // Create output file in app cache
        File cacheDir = context.getCacheDir();
        File audioFile = new File(cacheDir, "sos_audio_" + alertId + ".m4a");
        outputFilePath = audioFile.getAbsolutePath();

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                mediaRecorder = new MediaRecorder(context);
            } else {
                mediaRecorder = new MediaRecorder();
            }

            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setAudioEncodingBitRate(128000);
            mediaRecorder.setAudioSamplingRate(44100);
            mediaRecorder.setOutputFile(outputFilePath);

            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;

            Log.d(TAG, "Audio recording started — " + outputFilePath);

            // Auto-stop after 5 seconds
            handler.postDelayed(this::stopRecording, RECORDING_DURATION_MS);

        } catch (IOException | IllegalStateException | SecurityException e) {
            Log.e(TAG, "Failed to start audio recording: " + e.getMessage());
            cleanupRecorder();
            if (listener != null) {
                listener.onRecordingFailed("Recording failed: " + e.getMessage());
            }
        }
    }

    /**
     * Stop recording and deliver the file via callback.
     */
    public void stopRecording() {
        if (!isRecording || mediaRecorder == null) {
            Log.w(TAG, "stopRecording called but not recording (isRecording=" + isRecording + ", recorder=" + mediaRecorder + ")");
            return;
        }

        isRecording = false;

        try {
            mediaRecorder.stop();
            Log.d(TAG, "Audio recording stopped");
        } catch (IllegalStateException e) {
            Log.e(TAG, "Error stopping recorder: " + e.getMessage());
            cleanupRecorder();
            if (listener != null) {
                listener.onRecordingFailed("Stop failed: " + e.getMessage());
            }
            return;
        } catch (RuntimeException e) {
            Log.e(TAG, "Runtime error stopping recorder: " + e.getMessage());
            cleanupRecorder();
            if (listener != null) {
                listener.onRecordingFailed("Stop error: " + e.getMessage());
            }
            return;
        }

        // Release the MediaRecorder resources AFTER stop() succeeds
        // so the file is fully flushed to disk
        cleanupRecorder();

        // Deliver the file on the main thread with a small delay to ensure
        // the filesystem has fully flushed the file
        final String filePath = outputFilePath;
        handler.postDelayed(() -> {
            File audioFile = new File(filePath);
            if (audioFile.exists() && audioFile.length() > 0) {
                long fileSize = audioFile.length();
                Log.d(TAG, "Recording complete — file: " + filePath + ", size: " + fileSize + " bytes");
                if (listener != null) {
                    listener.onRecordingComplete(filePath, fileSize);
                }
            } else {
                Log.e(TAG, "Recording file is empty or missing: " + filePath
                        + " (exists=" + audioFile.exists()
                        + ", size=" + (audioFile.exists() ? audioFile.length() : -1) + ")");
                if (listener != null) {
                    listener.onRecordingFailed("Recording file is empty or missing");
                }
            }
        }, 200); // 200ms delay to let filesystem flush
    }

    /**
     * Cancel recording without delivering a result (e.g., SOS cancelled by PIN).
     */
    public void cancelRecording() {
        handler.removeCallbacksAndMessages(null);
        if (isRecording && mediaRecorder != null) {
            try {
                mediaRecorder.stop();
            } catch (IllegalStateException ignored) {}
        }
        cleanupRecorder();
        isRecording = false;

        // Delete the partial file
        if (outputFilePath != null) {
            File file = new File(outputFilePath);
            if (file.exists()) {
                file.delete();
            }
        }
        Log.d(TAG, "Recording cancelled and file deleted");
    }

    public boolean isRecording() {
        return isRecording;
    }

    private void cleanupRecorder() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.reset();
                mediaRecorder.release();
            } catch (Exception ignored) {}
            mediaRecorder = null;
        }
    }
}
