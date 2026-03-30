package com.example.aapraksha;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmergencyContactsActivity extends AppCompatActivity {

    private LinearLayout contactsContainer;
    private LinearLayout priorityContactContainer;
    private FloatingActionButton fabAddContact;
    private List<EmergencyContact> contacts;
    private EmergencyContact priorityContact;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_contacts);

        contactsContainer = findViewById(R.id.contacts_container);
        priorityContactContainer = findViewById(R.id.priority_contact_container);
        fabAddContact = findViewById(R.id.fab_add_contact);

        contacts = new ArrayList<>();
        
        // Load contacts from Firestore
        loadContactsFromFirestore();

        fabAddContact.setOnClickListener(v -> showAddContactDialog());

        ImageView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        // Setup bottom navigation
        View navHome = findViewById(R.id.nav_home);
        if (navHome != null) {
            navHome.setOnClickListener(v -> finish());
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Reload contacts every time user comes back to this screen
        loadContactsFromFirestore();
    }

    private void displayContacts() {
        // Display priority contact
        priorityContactContainer.removeAllViews();
        if (priorityContact != null) {
            View contactView = getLayoutInflater().inflate(R.layout.item_emergency_contact, priorityContactContainer, false);
            setupContactView(contactView, priorityContact, -1, true);
            priorityContactContainer.addView(contactView);
        }

        // Display regular contacts
        contactsContainer.removeAllViews();
        for (int i = 0; i < contacts.size(); i++) {
            final int index = i;
            EmergencyContact contact = contacts.get(i);
            View contactView = getLayoutInflater().inflate(R.layout.item_emergency_contact, contactsContainer, false);
            setupContactView(contactView, contact, index, false);
            contactsContainer.addView(contactView);
        }
    }

    private void setupContactView(View contactView, EmergencyContact contact, int index, boolean isPriority) {
        TextView nameText = contactView.findViewById(R.id.contact_name);
        TextView phoneText = contactView.findViewById(R.id.contact_phone);
        ImageView btnEdit = contactView.findViewById(R.id.btn_edit_contact);
        ImageView btnDelete = contactView.findViewById(R.id.btn_delete_contact);

        nameText.setText(contact.getName());
        String phoneDisplay = contact.getPhone();
        if (contact.getRelation() != null && !contact.getRelation().isEmpty()) {
            phoneDisplay = contact.getRelation() + " • " + contact.getPhone();
        }
        phoneText.setText(phoneDisplay);

        if (isPriority) {
            btnEdit.setOnClickListener(v -> showEditContactDialog(-1, true));
            btnDelete.setOnClickListener(v -> deletePriorityContact());
        } else {
            btnEdit.setOnClickListener(v -> showEditContactDialog(index, false));
            btnDelete.setOnClickListener(v -> deleteContact(index));
        }
    }

    private void showAddContactDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialog);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_contact, null);
        builder.setView(dialogView);

        EditText editName = dialogView.findViewById(R.id.edit_contact_name);
        EditText editPhone = dialogView.findViewById(R.id.edit_contact_phone);
        EditText editRelation = dialogView.findViewById(R.id.edit_contact_relation);
        CheckBox checkboxPriority = dialogView.findViewById(R.id.checkbox_priority);
        Button btnSave = dialogView.findViewById(R.id.btn_save_contact);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);

        // Get current user's phone for validation
        FirebaseAuth auth = FirebaseAuth.getInstance();
        final String[] currentUserPhone = {""};
        if (auth.getCurrentUser() != null) {
            String uid = auth.getCurrentUser().getUid();
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String userPhone = doc.getString("phone");
                        if (userPhone != null) {
                            currentUserPhone[0] = userPhone;
                        }
                    }
                });
        }

        // Disable priority checkbox if priority contact already exists
        if (priorityContact != null) {
            checkboxPriority.setEnabled(false);
            checkboxPriority.setText("Set as Priority Contact (Already set)");
        }

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        btnSave.setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            String phone = editPhone.getText().toString().trim();
            String relation = editRelation.getText().toString().trim();
            boolean isPriority = checkboxPriority.isChecked();

            if (name.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Validate: User cannot add their own phone
            String phoneDigits = phone.replaceAll("[^0-9]", "");
            String currentDigits = currentUserPhone[0].replaceAll("[^0-9]", "");
            if (!phoneDigits.isEmpty() && phoneDigits.equals(currentDigits)) {
                Toast.makeText(this, "You cannot add your own phone number", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check for duplicate phone numbers
            checkDuplicateContact(phoneDigits, name, phone, relation, isPriority, dialog);
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void checkDuplicateContact(String phoneDigits, String name, String phone, String relation, boolean isPriority, AlertDialog dialog) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;

        String uid = auth.getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Query existing contacts to check for duplicates
        db.collection("users").document(uid).collection("emergencyContacts")
            .get()
            .addOnSuccessListener(querySnapshot -> {
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    // Skip placeholder documents
                    if (doc.getBoolean("placeholder") != null && doc.getBoolean("placeholder")) {
                        continue;
                    }

                    String existingPhone = doc.getString("phone");
                    if (existingPhone != null) {
                        String existingDigits = existingPhone.replaceAll("[^0-9]", "");
                        if (phoneDigits.equals(existingDigits)) {
                            Toast.makeText(this, "This phone number already exists in your contacts", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                }

                // No duplicate found - proceed to save
                saveContactToFirestore(name, phone, relation, isPriority);
                dialog.dismiss();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Error checking duplicates: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
    
    private void saveContactToFirestore(String name, String phone, String relation, boolean isPriority) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;

        // Add phone validation
        if (!SMSHelper.isValidIndianPhone(phone)) {
            Toast.makeText(this, "Invalid Indian phone format. Use: 10 digits or +91 format", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = auth.getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        java.util.Map<String, Object> contact = new java.util.HashMap<>();
        contact.put("name", name);
        contact.put("phone", phone);
        contact.put("relation", relation);
        contact.put("isPriority", isPriority);
        contact.put("timestamp", System.currentTimeMillis());

        db.collection("users").document(uid).collection("emergencyContacts")
            .add(contact)
            .addOnSuccessListener(docRef -> {
                Toast.makeText(this, "Contact saved successfully", Toast.LENGTH_SHORT).show();
                loadContactsFromFirestore();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to save contact: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void showEditContactDialog(int index, boolean isPriority) {
        EmergencyContact contact = isPriority ? priorityContact : contacts.get(index);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialog);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_contact, null);
        builder.setView(dialogView);

        EditText editName = dialogView.findViewById(R.id.edit_contact_name);
        EditText editPhone = dialogView.findViewById(R.id.edit_contact_phone);
        EditText editRelation = dialogView.findViewById(R.id.edit_contact_relation);
        CheckBox checkboxPriority = dialogView.findViewById(R.id.checkbox_priority);
        Button btnSave = dialogView.findViewById(R.id.btn_save_contact);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        TextView dialogTitle = dialogView.findViewById(R.id.dialog_title);

        dialogTitle.setText("Edit Contact");
        editName.setText(contact.getName());
        editPhone.setText(contact.getPhone());
        editRelation.setText(contact.getRelation());
        checkboxPriority.setChecked(contact.isPriority());
        
        // Disable priority checkbox when editing
        checkboxPriority.setEnabled(false);
        if (isPriority) {
            checkboxPriority.setText("Priority Contact");
        }

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        btnSave.setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            String phone = editPhone.getText().toString().trim();
            String relation = editRelation.getText().toString().trim();

            if (name.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isPriority) {
                priorityContact = new EmergencyContact(name, phone, relation, true);
            } else {
                contacts.set(index, new EmergencyContact(name, phone, relation, false));
            }
            
            displayContacts();
            Toast.makeText(this, "Contact updated successfully", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void deleteContact(int index) {
        if (index < 0 || index >= contacts.size()) return;
        
        EmergencyContact contactToDelete = contacts.get(index);
        String docId = contactToDelete.getContactId();
        
        new AlertDialog.Builder(this)
            .setTitle("Delete Contact")
            .setMessage("Are you sure you want to delete this contact?")
            .setPositiveButton("Delete", (dialog, which) -> {
                // Delete from Firestore first
                FirebaseAuth auth = FirebaseAuth.getInstance();
                if (auth.getCurrentUser() == null) {
                    Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                String uid = auth.getCurrentUser().getUid();
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                
                if (docId != null && !docId.isEmpty()) {
                    db.collection("users").document(uid).collection("emergencyContacts")
                        .document(docId)
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            // Remove from list after successful Firestore deletion
                            contacts.remove(index);
                            displayContacts();
                            Toast.makeText(this, "Contact deleted successfully", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Failed to delete contact: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                } else {
                    // No docId, just remove from UI
                    contacts.remove(index);
                    displayContacts();
                    Toast.makeText(this, "Contact removed from list", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void deletePriorityContact() {
        new AlertDialog.Builder(this)
            .setTitle("Delete Priority Contact")
            .setMessage("Are you sure you want to delete your priority contact?")
            .setPositiveButton("Delete", (dialog, which) -> {
                priorityContact = null;
                displayContacts();
                Toast.makeText(this, "Priority contact deleted", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void loadContactsFromFirestore() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;

        String uid = auth.getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(uid).collection("emergencyContacts")
            .get()
            .addOnSuccessListener(querySnapshot -> {
                contacts.clear();
                priorityContact = null;
                
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    // Skip placeholder document
                    if (doc.getBoolean("placeholder") != null && doc.getBoolean("placeholder")) {
                        continue;
                    }
                    
                    String name = doc.getString("name");
                    String phone = doc.getString("phone");
                    String relation = doc.getString("relation");
                    Boolean isPriority = doc.getBoolean("isPriority");
                    
                    // Skip if name or phone missing
                    if (name == null || phone == null) {
                        continue;
                    }
                    
                    EmergencyContact contact = new EmergencyContact(name, phone, relation, isPriority != null && isPriority);
                    contact.setContactId(doc.getId());  // Store the Firestore document ID
                    
                    if (isPriority != null && isPriority) {
                        priorityContact = contact;
                    } else {
                        contacts.add(contact);
                    }
                }
                displayContacts();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(EmergencyContactsActivity.this, "Failed to load contacts", Toast.LENGTH_SHORT).show();
            });
    }

    static class EmergencyContact {
        private String contactId;  // Firestore document ID
        private String name;
        private String phone;
        private String relation;
        private boolean isPriority;

        public EmergencyContact(String name, String phone) {
            this.name = name;
            this.phone = phone;
            this.relation = "";
            this.isPriority = false;
            this.contactId = "";
        }

        public EmergencyContact(String name, String phone, String relation, boolean isPriority) {
            this.name = name;
            this.phone = phone;
            this.relation = relation;
            this.isPriority = isPriority;
            this.contactId = "";
        }

        public String getName() { return name; }
        public String getPhone() { return phone; }
        public String getRelation() { return relation; }
        public boolean isPriority() { return isPriority; }
        public String getContactId() { return contactId; }
        public void setContactId(String contactId) { this.contactId = contactId; }
    }
}
