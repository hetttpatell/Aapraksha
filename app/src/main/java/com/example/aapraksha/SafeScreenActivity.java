package com.example.aapraksha;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class SafeScreenActivity extends AppCompatActivity {

    private TextView tvContactName;
    private TextView tvContactRelation;
    private TextView tvContactPhone;
    private CardView btnCallContact;
    private CardView btnBackToDashboard;

    // Priority contact details (TODO: Load from SharedPreferences or Database)
    private String contactName = "John Doe";
    private String contactRelation = "Father";
    private String contactPhone = "+91 98765 43210";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_safe_screen);

        initializeViews();
        loadPriorityContact();
        setupClickListeners();
    }

    private void initializeViews() {
        tvContactName = findViewById(R.id.tv_contact_name);
        tvContactRelation = findViewById(R.id.tv_contact_relation);
        tvContactPhone = findViewById(R.id.tv_contact_phone);
        btnCallContact = findViewById(R.id.btn_call_contact);
        btnBackToDashboard = findViewById(R.id.btn_back_to_dashboard);
    }

    private void loadPriorityContact() {
        // TODO: Load from SharedPreferences or Database
        // For now using hardcoded data
        tvContactName.setText(contactName);
        tvContactRelation.setText(contactRelation);
        tvContactPhone.setText(contactPhone);
    }

    private void setupClickListeners() {
        // Call contact button
        btnCallContact.setOnClickListener(v -> {
            callContact();
        });

        // Back to dashboard button
        btnBackToDashboard.setOnClickListener(v -> {
            navigateToDashboard();
        });
    }

    private void callContact() {
        try {
            Intent callIntent = new Intent(Intent.ACTION_DIAL);
            callIntent.setData(Uri.parse("tel:" + contactPhone));
            startActivity(callIntent);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to make call", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void navigateToDashboard() {
        Intent intent = new Intent(SafeScreenActivity.this, DashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        // Override back button to go to dashboard
        navigateToDashboard();
    }
}
