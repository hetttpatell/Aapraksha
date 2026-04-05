package com.example.aapraksha;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SetPinActivity extends AppCompatActivity {

    private EditText etPin;
    private AppCompatButton btnSavePin;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_pin);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etPin = findViewById(R.id.et_pin_input);
        btnSavePin = findViewById(R.id.btn_save_pin);

        btnSavePin.setOnClickListener(v -> savePin());
    }

    private void savePin() {
        String pin = etPin.getText().toString().trim();

        if (pin.length() != 4) {
            etPin.setError("PIN must be exactly 4 digits");
            etPin.requestFocus();
            return;
        }

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Not authenticated", Toast.LENGTH_SHORT).show();
            navigateToLogin();
            return;
        }

        btnSavePin.setEnabled(false);
        Toast.makeText(this, "Saving PIN...", Toast.LENGTH_SHORT).show();

        String userId = user.getUid();
        Map<String, Object> updates = new HashMap<>();
        updates.put("emergencyPin", pin);

        db.collection("users").document(userId).update(updates)
                .addOnCompleteListener(task -> {
                    btnSavePin.setEnabled(true);
                    if (task.isSuccessful()) {
                        Toast.makeText(SetPinActivity.this, "Emergency PIN set successfully!", Toast.LENGTH_SHORT).show();
                        navigateToDashboard();
                    } else {
                        String errMsg = task.getException() != null ? task.getException().getMessage() : "Error saving PIN";
                        Toast.makeText(SetPinActivity.this, "Failed: " + errMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void navigateToDashboard() {
        android.content.SharedPreferences prefs = getSharedPreferences("AaprakshaPrefs", MODE_PRIVATE);
        prefs.edit().putBoolean("hasSeenOnboarding", true).apply();

        Intent intent = new Intent(SetPinActivity.this, DashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(SetPinActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
