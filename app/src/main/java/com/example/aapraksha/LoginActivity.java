package com.example.aapraksha;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private ImageView ivTogglePassword;
    private EditText etEmail;
    private EditText etPassword;
    private AppCompatButton btnLogin;
    private TextView tvForgotPassword;
    private TextView tvSignUpLink;
    private CardView btnGoogle;
    
    private boolean isPasswordVisible = false;
    private FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;
    private UserRepository userRepository;
    private static final int RC_SIGN_IN = 9001;
    private static final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initViews();
        setupFirebase();
        setupListeners();
        setupValidators();
    }

    private void initViews() {
        ivTogglePassword = findViewById(R.id.iv_toggle_password);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);
        tvSignUpLink = findViewById(R.id.tv_sign_up_link);
        btnGoogle = findViewById(R.id.btn_google);
    }

    private void setupFirebase() {
        auth = FirebaseAuth.getInstance();
        userRepository = new UserRepository();
        
        // Configure Google Sign-In with the web client ID from google-services.json
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .requestProfile()
                .build();
        
        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void setupListeners() {
        ivTogglePassword.setOnClickListener(v -> togglePasswordVisibility());

        btnLogin.setOnClickListener(v -> handleLogin());

        tvForgotPassword.setOnClickListener(v -> handleForgotPassword());

        tvSignUpLink.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });

        btnGoogle.setOnClickListener(v -> startGoogleSignIn());
    }

    private void handleForgotPassword() {
        String email = etEmail.getText().toString().trim();
        if (email.isEmpty() || !isValidEmail(email)) {
            etEmail.setError("Enter your email first to reset password");
            etEmail.requestFocus();
            Toast.makeText(this, "Enter your email address first", Toast.LENGTH_SHORT).show();
            return;
        }
        
        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Password reset email sent to " + email, Toast.LENGTH_LONG).show();
                    } else {
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Failed to send reset email";
                        Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void startGoogleSignIn() {
        Log.d(TAG, "Google Sign-In button clicked");
        
        if (googleSignInClient == null) {
            Log.e(TAG, "googleSignInClient is null!");
            Toast.makeText(this, "Error: Google Sign-In not initialized", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Disable button to prevent double taps
        btnGoogle.setEnabled(false);
        Toast.makeText(this, "Opening Google Sign-In...", Toast.LENGTH_SHORT).show();
        
        // Sign out first to force account picker to show every time
        // This prevents issues with stale tokens and ensures a fresh sign-in
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            Log.d(TAG, "Starting Google Sign-In activity");
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);
        
        // Re-enable Google button
        btnGoogle.setEnabled(true);
        
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
                        Log.e(TAG, "DEVELOPER_ERROR (10): Ensure the debug/release SHA-1 is added to Firebase Console for this app");
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
        Toast.makeText(this, "Authenticating...", Toast.LENGTH_SHORT).show();
        
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Firebase authentication with Google successful");
                        FirebaseUser firebaseUser = auth.getCurrentUser();
                        if (firebaseUser != null) {
                            // Check if user profile already exists in Firestore
                            checkAndCreateUserProfile(firebaseUser, account);
                        }
                    } else {
                        Log.e(TAG, "Firebase authentication with Google failed", task.getException());
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Authentication failed";
                        Toast.makeText(LoginActivity.this, "Auth failed: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void checkAndCreateUserProfile(FirebaseUser firebaseUser, GoogleSignInAccount googleAccount) {
        String userId = firebaseUser.getUid();
        
        // Check if user document already exists in Firestore
        FirebaseFirestore.getInstance().collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // User already exists, just update lastLoginAt and navigate
                        Log.d(TAG, "Existing user logged in via Google: " + userId);
                        
                        java.util.Map<String, Object> loginUpdate = new java.util.HashMap<>();
                        loginUpdate.put("lastLoginAt", com.google.firebase.Timestamp.now());
                        
                        FirebaseFirestore.getInstance().collection("users").document(userId)
                                .update(loginUpdate)
                                .addOnCompleteListener(t -> {
                                    String name = googleAccount.getDisplayName();
                                    Toast.makeText(LoginActivity.this, "Welcome back, " + name + "!", Toast.LENGTH_SHORT).show();
                                    navigateToDashboard(false);
                                });
                    } else {
                        // New user via Google Sign-In — create profile in Firestore
                        Log.d(TAG, "New Google user, creating profile: " + userId);
                        showAddPhoneDialog(googleAccount);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking user existence", e);
                    // Still try to create profile on failure to read
                    showAddPhoneDialog(googleAccount);
                });
    }

    private void showAddPhoneDialog(GoogleSignInAccount account) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_phone, null);
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
                    Log.d(TAG, "Google user profile created: " + userId);
                    Toast.makeText(LoginActivity.this, "Welcome, " + fullName + "!", Toast.LENGTH_SHORT).show();
                    navigateToDashboard(true);
                },
                errorMessage -> {
                    Log.e(TAG, "Error creating Google user profile: " + errorMessage);
                    Toast.makeText(LoginActivity.this, "Error creating profile: " + errorMessage, Toast.LENGTH_LONG).show();
                });
    }

    private void navigateToDashboard(boolean isNewUser) {
        Intent intent;
        if (isNewUser) {
            intent = new Intent(LoginActivity.this, OnboardingActivity.class);
            intent.putExtra("isNewUser", true);
        } else {
            intent = new Intent(LoginActivity.this, DashboardActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupValidators() {
        // Email validation
        etEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    if (!isValidEmail(s.toString()) && !isValidPhone(s.toString())) {
                        etEmail.setError("Enter valid email or phone");
                    } else {
                        etEmail.setError(null);
                    }
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
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private boolean isValidEmail(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isValidPhone(String phone) {
        String digitsOnly = phone.replaceAll("[^0-9]", "");
        return digitsOnly.length() >= 10;
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

    private void handleLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }

        if (!isValidEmail(email)) {
            etEmail.setError("Enter valid email");
            etEmail.requestFocus();
            return;
        }

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

        btnLogin.setEnabled(false);
        Toast.makeText(this, "Logging in...", Toast.LENGTH_SHORT).show();
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    btnLogin.setEnabled(true);
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Email login successful");
                        Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                        navigateToDashboard(false);
                    } else {
                        Log.e(TAG, "Email login failed", task.getException());
                        String errorMsg = task.getException() != null ? 
                                task.getException().getMessage() : "Login failed";
                                
                        if (errorMsg.contains("auth credential is incorrect")) {
                            errorMsg = "Incorrect Password! Or use 'Continue with Google'.";
                        }
                        
                        Toast.makeText(LoginActivity.this, "Login failed: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
