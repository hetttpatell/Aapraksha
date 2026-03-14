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
import java.util.ArrayList;
import java.util.List;

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
        
        // Sample data
        addSampleContacts();

        fabAddContact.setOnClickListener(v -> showAddContactDialog());

        ImageView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        // Setup bottom navigation
        View navHome = findViewById(R.id.nav_home);
        if (navHome != null) {
            navHome.setOnClickListener(v -> finish());
        }
    }

    private void addSampleContacts() {
        priorityContact = new EmergencyContact("John Doe", "+91 98765 43210", "Father", true);
        contacts.add(new EmergencyContact("Aman Sharma", "+91 87654 32109", "Brother", false));
        contacts.add(new EmergencyContact("Priya Verma", "+91 76543 21098", "Friend", false));
        displayContacts();
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

            EmergencyContact newContact = new EmergencyContact(name, phone, relation, isPriority);
            
            if (isPriority) {
                priorityContact = newContact;
                Toast.makeText(this, "Priority contact added successfully", Toast.LENGTH_SHORT).show();
            } else {
                contacts.add(newContact);
                Toast.makeText(this, "Contact added successfully", Toast.LENGTH_SHORT).show();
            }
            
            displayContacts();
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
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
        new AlertDialog.Builder(this)
            .setTitle("Delete Contact")
            .setMessage("Are you sure you want to delete this contact?")
            .setPositiveButton("Delete", (dialog, which) -> {
                contacts.remove(index);
                displayContacts();
                Toast.makeText(this, "Contact deleted", Toast.LENGTH_SHORT).show();
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

    static class EmergencyContact {
        private String name;
        private String phone;
        private String relation;
        private boolean isPriority;

        public EmergencyContact(String name, String phone) {
            this.name = name;
            this.phone = phone;
            this.relation = "";
            this.isPriority = false;
        }

        public EmergencyContact(String name, String phone, String relation, boolean isPriority) {
            this.name = name;
            this.phone = phone;
            this.relation = relation;
            this.isPriority = isPriority;
        }

        public String getName() { return name; }
        public String getPhone() { return phone; }
        public String getRelation() { return relation; }
        public boolean isPriority() { return isPriority; }
    }
}
