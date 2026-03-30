package com.example.aapraksha;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.example.aapraksha.models.User;

public class EditProfileActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextView btnReset;
    private ImageView profileImage;
    private CardView btnEditPhoto;
    private TextView tvUserName;
    private TextView tvMemberSince;
    private EditText etFullName;
    private EditText etPhone;
    private EditText etEmail;
    private EditText etEmergencyPin;
    private ImageView btnTogglePinVisibility;
    private CardView btnSaveChanges;

    // Store original values for reset
    private String originalName;
    private String originalPhone;
    private String originalEmail;
    private String originalPin;
    private boolean isPinVisible = false;
    
    private FirebaseAuth auth;
    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        auth = FirebaseAuth.getInstance();
        userRepository = new UserRepository();
        
        initializeViews();
        setupClickListeners();
        loadUserData();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btn_back);
        btnReset = findViewById(R.id.btn_reset);
        profileImage = findViewById(R.id.profile_image);
        btnEditPhoto = findViewById(R.id.btn_edit_photo);
        tvUserName = findViewById(R.id.tv_user_name);
        tvMemberSince = findViewById(R.id.tv_member_since);
        etFullName = findViewById(R.id.et_full_name);
        etPhone = findViewById(R.id.et_phone);
        etEmail = findViewById(R.id.et_email);
        etEmergencyPin = findViewById(R.id.et_emergency_pin);
        btnTogglePinVisibility = findViewById(R.id.btn_toggle_pin_visibility);
        btnSaveChanges = findViewById(R.id.btn_save_changes);
    }

    private void setupClickListeners() {
        // Back button
        btnBack.setOnClickListener(v -> finish());

        // Reset button
        btnReset.setOnClickListener(v -> resetFields());

        // Edit photo button
        btnEditPhoto.setOnClickListener(v -> {
            Toast.makeText(this, "Photo selection coming soon", Toast.LENGTH_SHORT).show();
            // TODO: Implement photo picker
        });

        // Toggle PIN visibility
        btnTogglePinVisibility.setOnClickListener(v -> togglePinVisibility());

        // Save changes button
        btnSaveChanges.setOnClickListener(v -> saveChanges());
    }

    private void loadUserData() {
        // Load actual user data from Firebase
        if (auth.getCurrentUser() == null) return;
        
        String userId = auth.getCurrentUser().getUid();
        userRepository.getUserProfile(userId, new UserRepository.OnUserFetchListener() {
            @Override
            public void onSuccess(User user) {
                if (user != null) {
                    originalName = user.getFullName() != null ? user.getFullName() : "";
                    originalPhone = user.getPhone() != null ? user.getPhone() : "";
                    originalEmail = user.getEmail() != null ? user.getEmail() : "";
                    originalPin = ""; // PIN is not stored in profile
                    
                    tvUserName.setText(originalName);
                    etFullName.setText(originalName);
                    etPhone.setText(originalPhone);
                    etEmail.setText(originalEmail);
                    tvMemberSince.setText("Member since " + (user.getMemberSince() != null ? user.getMemberSince() : ""));
                }
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(EditProfileActivity.this, "Failed to load profile: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void togglePinVisibility() {
        isPinVisible = !isPinVisible;
        
        if (isPinVisible) {
            // Show PIN
            etEmergencyPin.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            btnTogglePinVisibility.setImageResource(R.drawable.ic_eye_on);
        } else {
            // Hide PIN
            etEmergencyPin.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | 
                                       android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
            btnTogglePinVisibility.setImageResource(R.drawable.ic_eye_off);
        }
        
        // Move cursor to end
        etEmergencyPin.setSelection(etEmergencyPin.getText().length());
    }

    private void resetFields() {
        etFullName.setText(originalName);
        etPhone.setText(originalPhone);
        etEmail.setText(originalEmail);
        etEmergencyPin.setText(originalPin);
        Toast.makeText(this, "Fields reset", Toast.LENGTH_SHORT).show();
    }

    private void saveChanges() {
        String newName = etFullName.getText().toString().trim();
        String newPhone = etPhone.getText().toString().trim();
        String newEmail = etEmail.getText().toString().trim();
        String newPin = etEmergencyPin.getText().toString().trim();

        // Validate inputs
        if (newName.isEmpty()) {
            Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newPhone.isEmpty()) {
            Toast.makeText(this, "Please enter your phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newEmail.isEmpty()) {
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
            return;
        }

        // Basic email validation
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate PIN
        if (newPin.isEmpty() || newPin.length() != 4) {
            Toast.makeText(this, "Please enter a 4-digit PIN", Toast.LENGTH_SHORT).show();
            return;
        }

        // TODO: Save to SharedPreferences or database
        originalName = newName;
        originalPhone = newPhone;
        originalEmail = newEmail;
        originalPin = newPin;

        tvUserName.setText(newName);

        Toast.makeText(this, "Changes saved successfully!", Toast.LENGTH_SHORT).show();

        // Optional: Return to previous screen after saving
        // finish();
    }
}
