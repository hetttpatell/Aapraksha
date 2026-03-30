package com.example.aapraksha;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class SafeScreenActivity extends AppCompatActivity {

    private TextView tvContactName;
    private TextView tvContactRelation;
    private TextView tvContactPhone;
    private CardView btnCallContact;
    private CardView btnBackToDashboard;

    // Priority contact details
    private String contactName = "";
    private String contactRelation = "";
    private String contactPhone = "";
    private UserRepository userRepository;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_safe_screen);

        auth = FirebaseAuth.getInstance();
        userRepository = new UserRepository();
        db = FirebaseFirestore.getInstance();
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
        // Load priority contact from Firestore
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = auth.getCurrentUser().getUid();
        db.collection("users").document(uid).collection("emergencyContacts")
            .whereEqualTo("isPriority", true)
            .limit(1)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                if (!querySnapshot.isEmpty()) {
                    DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                    contactName = doc.getString("name") != null ? doc.getString("name") : "No contact";
                    contactRelation = doc.getString("relation") != null ? doc.getString("relation") : "";
                    contactPhone = doc.getString("phone") != null ? doc.getString("phone") : "";
                    
                    tvContactName.setText(contactName);
                    tvContactRelation.setText(contactRelation);
                    tvContactPhone.setText(contactPhone);
                } else {
                    tvContactName.setText("No priority contact set");
                    tvContactRelation.setText("");
                    tvContactPhone.setText("");
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to load contact", Toast.LENGTH_SHORT).show();
            });
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
