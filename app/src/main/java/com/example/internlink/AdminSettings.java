package com.example.internlink;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class AdminSettings extends AppCompatActivity {
    private AppCompatButton backButton;
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_settings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        Button addAdminBtn = findViewById(R.id.addAdminBtn);

        addAdminBtn.setOnClickListener(v -> {
            LayoutInflater inflater = LayoutInflater.from(this);
            View dialogView = inflater.inflate(R.layout.dialog_add_admin, null);

            EditText nameInput = dialogView.findViewById(R.id.adminNameEditText);
            EditText emailInput = dialogView.findViewById(R.id.adminEmailEditText);
            EditText passwordInput = dialogView.findViewById(R.id.adminPasswordEditText);

            new AlertDialog.Builder(this)
                    .setTitle("Add New Admin")
                    .setView(dialogView)
                    .setPositiveButton("Add", (dialog, which) -> {
                        String name = nameInput.getText().toString().trim();
                        String email = emailInput.getText().toString().trim();
                        String password = passwordInput.getText().toString().trim();

                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });


    }
    private void addNewAdmin(String email, String name, String password) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Users");

        String userId = databaseReference.push().getKey();  // generate a unique ID
        if (userId == null) return;

        HashMap<String, String> adminData = new HashMap<>();
        adminData.put("email", email);
        adminData.put("name", name);
        adminData.put("password", password);
        adminData.put("role", "admin");

        databaseReference.child(userId).setValue(adminData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getApplicationContext(), "Admin added successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getApplicationContext(), "Failed to add admin", Toast.LENGTH_SHORT).show();
                });
    }

}