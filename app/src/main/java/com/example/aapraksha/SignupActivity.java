package com.example.aapraksha;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.cardview.widget.CardView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

public class SignupActivity extends AppCompatActivity {

    private ImageView ivTogglePassword;
    private ImageView ivToggleConfirmPassword;
    private EditText etFullName;
    private EditText etPhone;
    private EditText etEmail;
    private EditText etPassword;
    private EditText etConfirmPassword;
    private AppCompatButton btnRegister;
    private TextView tvLoginLink;
    private CardView btnGoogle;
    
    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;
    private FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;
    private UserRepository userRepository;
    private static final int RC_SIGN_IN = 9001;
    private static final String TAG = "SignupActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        auth = FirebaseAuth.getInstance();
        userRepository = new UserRepository();
        initViews();
        setupGoogleSignIn();
        setupListeners();
        setupValidators();
    }

    private void initViews() {
        ivTogglePassword = findViewById(R.id.iv_toggle_password);
        ivToggleConfirmPassword = findViewById(R.id.iv_toggle_confirm_password);
        etFullName = findViewById(R.id.et_full_name);
        etPhone = findViewById(R.id.et_phone);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnRegister = findViewById(R.id.btn_register);
        tvLoginLink = findViewById(R.id.tv_login_link);
        btnGoogle = findViewById(R.id.btn_google);
    }

    private void setupGoogleSignIn() {
        // Configure Google Sign-In with web client ID from google-services.json
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .requestProfile()
                .build();
        
        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void setupListeners() {
        ivTogglePassword.setOnClickListener(v -> togglePasswordVisibility());
        ivToggleConfirmPassword.setOnClickListener(v -> toggleConfirmPasswordVisibility());
        btnRegister.setOnClickListener(v -> handleRegister());
        tvLoginLink.setOnClickListener(v -> finish());
        
        // Google Sign-In button
        if (btnGoogle != null) {
            btnGoogle.setOnClickListener(v -> startGoogleSignIn());
        }
    }

    private void startGoogleSignIn() {
        Log.d(TAG, "Google Sign-In button clicked from Signup");
        
        if (googleSignInClient == null) {
            Log.e(TAG, "googleSignInClient is null!");
            Toast.makeText(this, "Error: Google Sign-In not initialized", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Disable button to prevent double taps
        if (btnGoogle != null) btnGoogle.setEnabled(false);
        Toast.makeText(this, "Opening Google Sign-In...", Toast.LENGTH_SHORT).show();
        
        // Sign out first to force account picker
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            Log.d(TAG, "Starting Google Sign-In activity from Signup");
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);
        
        // Re-enable Google button
        if (btnGoogle != null) btnGoogle.setEnabled(true);
        
        if (requestCode == RC_SIGN_IN) {
            if (data == null) {
                Log.e(TAG, "Google Sign-In data is null");
                Toast.makeText(this, "Sign-In was cancelled", Toast.LENGTH_SHORT).show();
                return;
            }
            
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null && account.getIdToken() != null) {
                    Log.d(TAG, "Google Sign-In successful: " + account.getEmail());
                    firebaseAuthWithGoogle(account);
                } else {
                    Log.e(TAG, "Google account or ID token is null");
                    Toast.makeText(this, "Could not get Google account info", Toast.LENGTH_SHORT).show();
                }
            } catch (ApiException e) {
                Log.e(TAG, "Google Sign-In failed with status code: " + e.getStatusCode(), e);
                String errorMessage;
                switch (e.getStatusCode()) {
                    case 12501:
                        errorMessage = "Sign-In was cancelled";
                        break;
                    case 12500:
                        errorMessage = "Sign-In failed. Please try again.";
                        break;
                    case 10:
                        errorMessage = "Developer error: Check SHA-1 fingerprint in Firebase Console";
                        Log.e(TAG, "DEVELOPER_ERROR (10): Ensure the debug/release SHA-1 is added to Firebase Console");
                        break;
                    case 7:
                        errorMessage = "Network error. Check your internet connection.";
                        break;
                    default:
                        errorMessage = "Sign in failed (Code: " + e.getStatusCode() + ")";
                        break;
                }
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        Log.d(TAG, "firebaseAuthWithGoogle: " + account.getEmail());
        Toast.makeText(this, "Creating your account...", Toast.LENGTH_SHORT).show();
        
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Firebase authentication with Google successful");
                        FirebaseUser firebaseUser = auth.getCurrentUser();
                        if (firebaseUser != null) {
                            checkAndCreateUserProfile(firebaseUser, account);
                        }
                    } else {
                        Log.e(TAG, "Firebase authentication with Google failed", task.getException());
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Authentication failed";
                        Toast.makeText(SignupActivity.this, "Auth failed: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void checkAndCreateUserProfile(FirebaseUser firebaseUser, GoogleSignInAccount googleAccount) {
        String userId = firebaseUser.getUid();
        
        // Check if user already exists in Firestore
        FirebaseFirestore.getInstance().collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // User already exists — just log them in
                        Log.d(TAG, "User already exists, logging in: " + userId);
                        
                        java.util.Map<String, Object> loginUpdate = new java.util.HashMap<>();
                        loginUpdate.put("lastLoginAt", com.google.firebase.Timestamp.now());
                        
                        FirebaseFirestore.getInstance().collection("users").document(userId)
                                .update(loginUpdate)
                                .addOnCompleteListener(t -> {
                                    String name = googleAccount.getDisplayName();
                                    Toast.makeText(SignupActivity.this, "Welcome back, " + name + "! Account already exists.", Toast.LENGTH_SHORT).show();
                                    navigateToDashboard(false);
                                });
                    } else {
                        // New user — create profile in Firestore
                        Log.d(TAG, "New Google user from signup, creating profile: " + userId);
                        showAddPhoneDialog(googleAccount);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking user existence", e);
                    showAddPhoneDialog(googleAccount);
                });
    }

    private void showAddPhoneDialog(GoogleSignInAccount account) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_phone, null);
        builder.setView(dialogView);
        
        android.app.AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        EditText etPhone = dialogView.findViewById(R.id.edit_phone);
        android.widget.Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        android.widget.Button btnSavePhone = dialogView.findViewById(R.id.btn_save_phone);

        btnCancel.setOnClickListener(v -> {
            dialog.dismiss();
            auth.signOut();
            googleSignInClient.signOut();
            Toast.makeText(this, "Registration cancelled", Toast.LENGTH_SHORT).show();
        });

        btnSavePhone.setOnClickListener(v -> {
            String phone = etPhone.getText().toString().trim();
            String digitsOnly = phone.replaceAll("[^0-9]", "");
            if (digitsOnly.length() < 10) {
                etPhone.setError("Please enter a valid 10-digit mobile number");
                return;
            }
            dialog.dismiss();
            createGoogleUserProfileWithPhone(account, phone);
        });

        dialog.show();
    }

    private void createGoogleUserProfileWithPhone(GoogleSignInAccount account, String phone) {
        String fullName = account.getDisplayName() != null ? account.getDisplayName() : "User";
        String email = account.getEmail() != null ? account.getEmail() : "";
        
        Log.d(TAG, "Creating Google user profile for: " + fullName + " (" + email + ") with phone: " + phone);
        
        userRepository.createUserProfile(fullName, email, phone, "",
                userId -> {
                    Log.d(TAG, "Google user profile created successfully: " + userId);
                    Toast.makeText(SignupActivity.this, "Account created! Welcome, " + fullName + "!", Toast.LENGTH_SHORT).show();
                    navigateToDashboard(true);
                },
                errorMessage -> {
                    Log.e(TAG, "Error creating Google user profile: " + errorMessage);
                    Toast.makeText(SignupActivity.this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
                });
    }

    private void navigateToDashboard(boolean isNewUser) {
        Intent intent;
        if (isNewUser) {
            intent = new Intent(SignupActivity.this, OnboardingActivity.class);
            intent.putExtra("isNewUser", true);
        } else {
            intent = new Intent(SignupActivity.this, DashboardActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupValidators() {
        // Full Name validation
        etFullName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0 && s.length() < 3) {
                    etFullName.setError("Name must be at least 3 characters");
                } else if (s.length() > 0 && !s.toString().matches("^[a-zA-Z\\s]+$")) {
                    etFullName.setError("Name can only contain letters");
                } else {
                    etFullName.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Phone validation
        etPhone.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    String digitsOnly = s.toString().replaceAll("[^0-9]", "");
                    if (digitsOnly.length() < 10) {
                        etPhone.setError("Phone must be at least 10 digits");
                    } else {
                        etPhone.setError(null);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Email validation
        etEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0 && !Patterns.EMAIL_ADDRESS.matcher(s).matches()) {
                    etEmail.setError("Enter a valid email address");
                } else {
                    etEmail.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Password validation
        etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0 && s.length() < 6) {
                    etPassword.setError("Password must be at least 6 characters");
                } else {
                    etPassword.setError(null);
                }
                
                // Re-validate confirm password if it has content
                String confirmPass = etConfirmPassword.getText().toString();
                if (!confirmPass.isEmpty() && !confirmPass.equals(s.toString())) {
                    etConfirmPassword.setError("Passwords do not match");
                } else if (!confirmPass.isEmpty()) {
                    etConfirmPassword.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Confirm Password validation
        etConfirmPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String password = etPassword.getText().toString();
                if (s.length() > 0 && !s.toString().equals(password)) {
                    etConfirmPassword.setError("Passwords do not match");
                } else {
                    etConfirmPassword.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            ivTogglePassword.setImageResource(R.drawable.ic_eye_off);
        } else {
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            ivTogglePassword.setImageResource(R.drawable.ic_eye_on);
        }
        isPasswordVisible = !isPasswordVisible;
        etPassword.setSelection(etPassword.getText().length());
    }

    private void toggleConfirmPasswordVisibility() {
        if (isConfirmPasswordVisible) {
            etConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            ivToggleConfirmPassword.setImageResource(R.drawable.ic_lock);
        } else {
            etConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            ivToggleConfirmPassword.setImageResource(R.drawable.ic_eye_on);
        }
        isConfirmPasswordVisible = !isConfirmPasswordVisible;
        etConfirmPassword.setSelection(etConfirmPassword.getText().length());
    }

    private void handleRegister() {
        String fullName = etFullName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Validate full name
        if (fullName.isEmpty()) {
            etFullName.setError("Full name is required");
            etFullName.requestFocus();
            return;
        }

        if (fullName.length() < 3) {
            etFullName.setError("Name must be at least 3 characters");
            etFullName.requestFocus();
            return;
        }

        if (!fullName.matches("^[a-zA-Z\\s]+$")) {
            etFullName.setError("Name can only contain letters");
            etFullName.requestFocus();
            return;
        }

        // Validate phone
        if (phone.isEmpty()) {
            etPhone.setError("Phone number is required");
            etPhone.requestFocus();
            return;
        }

        String digitsOnly = phone.replaceAll("[^0-9]", "");
        if (digitsOnly.length() < 10) {
            etPhone.setError("Phone must be at least 10 digits");
            etPhone.requestFocus();
            return;
        }

        // Validate email
        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email address");
            etEmail.requestFocus();
            return;
        }

        // Validate password
        if (password.isEmpty()) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return;
        }

        // Validate confirm password
        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return;
        }

        // First, create Firebase Auth user
        registerWithFirebase(email, password, fullName, phone);
    }

    private void registerWithFirebase(String email, String password, String fullName, String phone) {
        btnRegister.setEnabled(false);
        Toast.makeText(this, "Creating account...", Toast.LENGTH_SHORT).show();
        
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Firebase registration successful");
                        createUserProfile(fullName, email, phone, "");
                    } else {
                        btnRegister.setEnabled(true);
                        Log.e(TAG, "Firebase registration failed", task.getException());
                        String errorMessage = task.getException() != null ? 
                                task.getException().getMessage() : "Registration failed";
                        Toast.makeText(SignupActivity.this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void createUserProfile(String fullName, String email, String phone, String emergencyPin) {
        userRepository.createUserProfile(fullName, email, phone, emergencyPin,
                userId -> {
                    Log.d(TAG, "User profile created: " + userId);
                    Toast.makeText(SignupActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                    navigateToDashboard(true);
                },
                errorMessage -> {
                    btnRegister.setEnabled(true);
                    Log.e(TAG, "Error creating profile: " + errorMessage);
                    Toast.makeText(SignupActivity.this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
                }
        );
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
