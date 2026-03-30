package com.example.aapraksha;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.aapraksha.models.User;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";
    private TextView tvAvatarInitial, tvProfileName, tvProfileEmail, tvProfilePhone, tvProfileRole;
    private Button btnLogout;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        tvAvatarInitial = findViewById(R.id.tvAvatarInitial);
        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        tvProfilePhone = findViewById(R.id.tvProfilePhone);
        tvProfileRole = findViewById(R.id.tvProfileRole);
        btnLogout = findViewById(R.id.btnLogout);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        userRepository = new UserRepository();

        loadUserData();

        btnLogout.setOnClickListener(v -> logout());
    }

    private void loadUserData() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String uid = auth.getCurrentUser().getUid();
        
        // Load user profile using UserRepository
        userRepository.getUserProfile(uid, new UserRepository.OnUserFetchListener() {
            @Override
            public void onSuccess(User user) {
                if (user != null) {
                    displayUserProfile(user);
                } else {
                    Toast.makeText(ProfileActivity.this, "User profile not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Failed to load profile: " + errorMessage);
                Toast.makeText(ProfileActivity.this, "Failed to load profile: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayUserProfile(User user) {
        // Set avatar initial
        if (user.getFullName() != null && !user.getFullName().isEmpty()) {
            tvAvatarInitial.setText(user.getFullName().substring(0, 1).toUpperCase());
        } else {
            tvAvatarInitial.setText("?");
        }

        tvProfileName.setText(user.getFullName() != null ? user.getFullName() : "N/A");
        tvProfileEmail.setText(user.getEmail() != null ? user.getEmail() : "N/A");
        tvProfilePhone.setText(user.getPhone() != null && !user.getPhone().isEmpty() ? user.getPhone() : "Not provided");
        
        // All users are equal - show "User" role
        tvProfileRole.setText("User");

        Log.d(TAG, "Profile loaded: " + user.getFullName());
    }

    private void logout() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    auth.signOut();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("No", null)
                .show();
    }
}
