package com.example.aapraksha;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.cardview.widget.CardView;

import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {

    private ImageView ivTogglePassword;
    private EditText etEmail;
    private EditText etPassword;
    private AppCompatButton btnLogin;
    private TextView tvForgotPassword;
    private TextView tvSignUpLink;
    private CardView btnGoogle;
    
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initViews();
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

        btnGoogle.setOnClickListener(v -> {
            Toast.makeText(this, "Google Sign In", Toast.LENGTH_SHORT).show();
            // TODO: Implement Google Sign In
        });
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

        // Validate email/phone
        if (email.isEmpty()) {
            etEmail.setError("Email or phone is required");
            etEmail.requestFocus();
            return;
        }

        if (!isValidEmail(email) && !isValidPhone(email)) {
            etEmail.setError("Enter valid email or phone");
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

        // TODO: Implement actual login logic here
        // For now, just navigate to MainActivity
        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
