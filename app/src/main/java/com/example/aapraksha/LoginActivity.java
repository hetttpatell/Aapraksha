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
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.example.aapraksha.models.User;

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
        
        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        
        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void setupListeners() {
        ivTogglePassword.setOnClickListener(v -> togglePasswordVisibility());

        btnLogin.setOnClickListener(v -> handleLogin());

        tvForgotPassword.setOnClickListener(v -> {
            Toast.makeText(this, "Forgot Password clicked", Toast.LENGTH_SHORT).show();
            // TODO: Navigate to Forgot Password screen
        });

        tvSignUpLink.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });

        btnGoogle.setOnClickListener(v -> startGoogleSignIn());
    }

    private void startGoogleSignIn() {
        Log.d(TAG, "Google Sign-In button clicked");
        Toast.makeText(this, "Opening Google Sign-In...", Toast.LENGTH_SHORT).show();
        
        if (googleSignInClient == null) {
            Log.e(TAG, "googleSignInClient is null!");
            Toast.makeText(this, "Error: Google Sign-In not initialized", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Intent signInIntent = googleSignInClient.getSignInIntent();
        Log.d(TAG, "Starting Google Sign-In activity");
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);
        
        if (requestCode == RC_SIGN_IN) {
            if (data == null) {
                Log.e(TAG, "Google Sign-In data is null");
                Toast.makeText(this, "Sign-In cancelled or error occurred", Toast.LENGTH_SHORT).show();
                return;
            }
            
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    Log.d(TAG, "Google Sign-In successful: " + account.getEmail());
                    Toast.makeText(this, "Google Sign-In successful!", Toast.LENGTH_SHORT).show();
                    firebaseAuthWithGoogle(account.getIdToken());
                } else {
                    Log.e(TAG, "Google account is null");
                    Toast.makeText(this, "Could not get Google account", Toast.LENGTH_SHORT).show();
                }
            } catch (ApiException e) {
                Log.e(TAG, "Google Sign-In failed: " + e.getStatusCode(), e);
                String errorMessage = "Sign in failed (Code: " + e.getStatusCode() + ")";
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        Toast.makeText(this, "Authenticating with Firebase...", Toast.LENGTH_SHORT).show();
        
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Firebase authentication successful");
                        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
                        if (account != null) {
                            createOrUpdateUserProfile(account);
                        }
                    } else {
                        Log.e(TAG, "Firebase authentication failed", task.getException());
                        Toast.makeText(LoginActivity.this, "Authentication failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void createOrUpdateUserProfile(GoogleSignInAccount account) {
        String userId = auth.getCurrentUser().getUid();
        String fullName = account.getDisplayName();
        String email = account.getEmail();
        String phone = ""; // Google Sign-In doesn't provide phone by default
        String emergencyPin = "0000"; // Default PIN, user should set their own
        
        Log.d(TAG, "Creating profile for: " + fullName + " (" + email + ")");
        
        userRepository.createUserProfile(fullName, email, phone, emergencyPin,
                new UserRepository.OnCompleteListener() {
                    @Override
                    public void onComplete(String userId) {
                        Log.d(TAG, "User profile created: " + userId);
                        Toast.makeText(LoginActivity.this, "Welcome, " + fullName + "!", Toast.LENGTH_SHORT).show();
                        navigateToDashboard();
                    }
                },
                new UserRepository.OnErrorListener() {
                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "Error creating user profile: " + errorMessage);
                        Toast.makeText(LoginActivity.this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
                        // Don't navigate on error - let user try again
                    }
                });
    }

    private void navigateToDashboard() {
        Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
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
        // Remove all non-digit characters
        String digitsOnly = phone.replaceAll("[^0-9]", "");
        // Check if it has at least 10 digits
        return digitsOnly.length() >= 10;
    }

    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            // Hide password
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            ivTogglePassword.setImageResource(R.drawable.ic_eye_off);
        } else {
            // Show password
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            ivTogglePassword.setImageResource(R.drawable.ic_eye_on);
        }
        isPasswordVisible = !isPasswordVisible;
        // Move cursor to end
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

        Toast.makeText(this, "Logging in...", Toast.LENGTH_SHORT).show();
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Email login successful");
                        Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                        navigateToDashboard();
                    } else {
                        Log.e(TAG, "Email login failed", task.getException());
                        String errorMsg = task.getException() != null ? 
                                task.getException().getMessage() : "Login failed";
                        Toast.makeText(LoginActivity.this, "Login failed: " + errorMsg, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
