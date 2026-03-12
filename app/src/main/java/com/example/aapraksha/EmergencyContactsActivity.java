package com.example.aapraksha;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
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
    private FloatingActionButton fabAddContact;
    private List<EmergencyContact> contacts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_contacts);

        contactsContainer = findViewById(R.id.contacts_container);
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
        contacts.add(new EmergencyContact("Aman Sharma", "+91 98765 43210"));
        contacts.add(new EmergencyContact("Priya Verma", "+91 87654 32109"));
        contacts.add(new EmergencyContact("Rajesh Kumar", "+91 76543 21098"));
        displayContacts();
    }

    private void displayContacts() {
        contactsContainer.removeAllViews();
        
        for (int i = 0; i < contacts.size(); i++) {
            final int index = i;
            EmergencyContact contact = contacts.get(i);
            View contactView = getLayoutInflater().inflate(R.layout.item_emergency_contact, contactsContainer, false);

            TextView nameText = contactView.findViewById(R.id.contact_name);
            TextView phoneText = contactView.findViewById(R.id.contact_phone);
            ImageView btnEdit = contactView.findViewById(R.id.btn_edit_contact);
            ImageView btnDelete = contactView.findViewById(R.id.btn_delete_contact);

            nameText.setText(contact.getName());
            phoneText.setText(contact.getPhone());

            btnEdit.setOnClickListener(v -> showEditContactDialog(index));
            btnDelete.setOnClickListener(v -> deleteContact(index));

            contactsContainer.addView(contactView);
        }
    }

    private void showAddContactDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialog);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_contact, null);
        builder.setView(dialogView);

        EditText editName = dialogView.findViewById(R.id.edit_contact_name);
        EditText editPhone = dialogView.findViewById(R.id.edit_contact_phone);
        EditText editRelation = dialogView.findViewById(R.id.edit_contact_relation);
        Button btnSave = dialogView.findViewById(R.id.btn_save_contact);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);

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

            contacts.add(new EmergencyContact(name, phone, relation));
            displayContacts();
            Toast.makeText(this, "Contact added successfully", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showEditContactDialog(int index) {
        EmergencyContact contact = contacts.get(index);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialog);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_contact, null);
        builder.setView(dialogView);

        EditText editName = dialogView.findViewById(R.id.edit_contact_name);
        EditText editPhone = dialogView.findViewById(R.id.edit_contact_phone);
        EditText editRelation = dialogView.findViewById(R.id.edit_contact_relation);
        Button btnSave = dialogView.findViewById(R.id.btn_save_contact);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        TextView dialogTitle = dialogView.findViewById(R.id.dialog_title);

        dialogTitle.setText("Edit Contact");
        editName.setText(contact.getName());
        editPhone.setText(contact.getPhone());
        editRelation.setText(contact.getRelation());

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

            contacts.set(index, new EmergencyContact(name, phone, relation));
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

    static class EmergencyContact {
        private String name;
        private String phone;
        private String relation;

        public EmergencyContact(String name, String phone) {
            this.name = name;
            this.phone = phone;
            this.relation = "";
        }

        public EmergencyContact(String name, String phone, String relation) {
            this.name = name;
            this.phone = phone;
            this.relation = relation;
        }

        public String getName() { return name; }
        public String getPhone() { return phone; }
        public String getRelation() { return relation; }
    }
}
