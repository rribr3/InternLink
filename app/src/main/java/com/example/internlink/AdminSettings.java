package com.example.internlink;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ScrollView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.compose.ui.graphics.Color;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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

        addAdminBtn.setOnClickListener(v -> addNewAdmin());

        Button editTermsBtn = findViewById(R.id.editTermsBtn);
        editTermsBtn.setOnClickListener(v -> showEditTermsPopup());

        Button saveTermsBtn = findViewById(R.id.saveTermsBtn);
        saveTermsBtn.setOnClickListener(v -> saveTermsAsPdf());

    }
    private void addNewAdmin() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        View popupView = LayoutInflater.from(this).inflate(R.layout.dialog_add_admin, null);
        builder.setView(popupView);
        AlertDialog dialog = builder.create();
        dialog.show();

        EditText nameEditText = dialog.findViewById(R.id.adminNameEditText);
        EditText emailEditText = dialog.findViewById(R.id.adminEmailEditText);
        EditText passwordEditText = dialog.findViewById(R.id.adminPasswordEditText);
        ImageView toggleSignupPassword1 = dialog.findViewById(R.id.toggle_password_signup1);
        Button adminAdd = dialog.findViewById(R.id.addAdmin);

        toggleSignupPassword1.setOnClickListener(v -> {
            if (passwordEditText.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                toggleSignupPassword1.setImageResource(R.drawable.ic_eye_open);
            } else {
                passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                toggleSignupPassword1.setImageResource(R.drawable.ic_eye_closed);
            }
            passwordEditText.setTypeface(android.graphics.Typeface.create("sans-serif-black", android.graphics.Typeface.NORMAL));
            passwordEditText.setSelection(passwordEditText.getText().length());
        });

        adminAdd.setOnClickListener(v -> {
            String name = nameEditText.getText().toString().trim();
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseAuth auth = FirebaseAuth.getInstance();
            DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

            auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            String userId = auth.getCurrentUser().getUid();
                            User user = new User(name, email, "admin");

                            usersRef.child(userId).setValue(user)
                                    .addOnCompleteListener(task1 -> {
                                        if (task1.isSuccessful()) {
                                            Toast.makeText(this, "Admin Added.", Toast.LENGTH_SHORT).show();

                                        } else {
                                            Toast.makeText(this, "Failed to add admin.", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            Exception e = task.getException();
                            Toast.makeText(this, "Signup failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            e.printStackTrace();
                        }
                    });
        });

    }

    private void showEditTermsPopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Terms and Conditions");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setMinLines(10);
        input.setHint("Enter terms and conditions...");

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(input);
        builder.setView(scrollView);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String updatedTerms = input.getText().toString().trim();
            if (!updatedTerms.isEmpty()) {
                // Save the updated HTML content to Firebase
                DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("settings").child("terms");

                dbRef.setValue(updatedTerms)
                        .addOnSuccessListener(aVoid -> Toast.makeText(this, "Terms updated", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(this, "Failed to update terms: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } else {
                Toast.makeText(this, "Terms cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        // Load the existing HTML content from Firebase if available
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("settings").child("terms");
        dbRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                input.setText(snapshot.getValue(String.class));  // Load existing HTML content into the EditText
            }
        });

        builder.create().show();
    }

    private void saveTermsAsPdf() {
        // Fetch the terms content from Firebase
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("settings").child("terms");
        dbRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                String htmlContent = snapshot.getValue(String.class);
                if (htmlContent != null) {
                    // Save as PDF
                    try {
                        createPdf(htmlContent);
                    } catch (IOException e) {
                        Toast.makeText(this, "Error saving PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                Toast.makeText(this, "No terms available to save", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createPdf(String htmlContent) throws IOException {
        // Create PDF document
        android.graphics.pdf.PdfDocument document = new android.graphics.pdf.PdfDocument();
        android.graphics.pdf.PdfDocument.PageInfo pageInfo = new android.graphics.pdf.PdfDocument.PageInfo.Builder(600, 800, 1).create();
        android.graphics.pdf.PdfDocument.Page page = document.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        paint.setTextSize(12);

        // Draw HTML content as plain text for now (you can improve this to render HTML properly)
        canvas.drawText(htmlContent, 10, 25, paint);

        document.finishPage(page);

        // Save PDF to storage
        File pdfFile = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Terms_Conditions.pdf");
        FileOutputStream outputStream = new FileOutputStream(pdfFile);
        document.writeTo(outputStream);
        document.close();

        Toast.makeText(this, "PDF Saved at: " + pdfFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
    }

}