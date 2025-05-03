package com.example.internlink;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class AdminSettings extends AppCompatActivity {
    private AppCompatButton backButton;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean isDark = prefs.getBoolean("darkMode", false);
        AppCompatDelegate.setDefaultNightMode(
                isDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

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

        Switch darkModeSwitch = findViewById(R.id.darkModeSwitch);
        FirebaseAuth auth = FirebaseAuth.getInstance();
        String userId = auth.getCurrentUser().getUid();
        DatabaseReference userSettingsRef = FirebaseDatabase.getInstance().getReference("user_settings").child(userId);

        darkModeSwitch.setChecked(isDark);
        userSettingsRef.child("darkMode").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Boolean isDarkFirebase = task.getResult().getValue(Boolean.class);
                if (isDarkFirebase != null) {
                    darkModeSwitch.setChecked(isDarkFirebase);
                }
            }
        });

        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
            userSettingsRef.child("darkMode").setValue(isChecked);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("darkMode", isChecked);
            editor.apply();
        });

        findViewById(R.id.changeNameBtn).setOnClickListener(v -> showChangeNameDialog());
        findViewById(R.id.changeEmailBtn).setOnClickListener(v -> showChangeEmailDialog());
        findViewById(R.id.changePasswordBtn).setOnClickListener(v -> showChangePasswordDialog());

        findViewById(R.id.logoutBtn).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(AdminSettings.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(AdminSettings.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            finish();
        });


        findViewById(R.id.deactivateBtn).setOnClickListener(v -> {
            new AlertDialog.Builder(AdminSettings.this)
                    .setTitle("Deactivate Account")
                    .setMessage("Are you sure you want to deactivate your account?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                        if (currentUser != null) {
                            String user_Id = currentUser.getUid();
                            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(user_Id);
                            userRef.child("status").setValue("deactivated").addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    FirebaseAuth.getInstance().signOut();
                                    Toast.makeText(AdminSettings.this, "Account deactivated", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(AdminSettings.this, LoginActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                                    finish();
                                } else {
                                    Toast.makeText(AdminSettings.this, "Failed to deactivate account", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

    }

    private void addNewAdmin() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View popupView = LayoutInflater.from(this).inflate(R.layout.dialog_add_admin, null);
        builder.setView(popupView);
        AlertDialog dialog = builder.create();
        dialog.show();

        EditText nameEditText = popupView.findViewById(R.id.adminNameEditText);
        EditText emailEditText = popupView.findViewById(R.id.adminEmailEditText);
        EditText passwordEditText = popupView.findViewById(R.id.adminPasswordEditText);
        ImageView toggleSignupPassword1 = popupView.findViewById(R.id.toggle_password_signup1);
        Button adminAdd = popupView.findViewById(R.id.addAdmin);

        toggleSignupPassword1.setOnClickListener(v -> {
            if (passwordEditText.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                toggleSignupPassword1.setImageResource(R.drawable.ic_eye_open);
            } else {
                passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                toggleSignupPassword1.setImageResource(R.drawable.ic_eye_closed);
            }
            passwordEditText.setTypeface(Typeface.create("sans-serif-black", Typeface.NORMAL));
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
                                            dialog.dismiss();
                                        } else {
                                            Toast.makeText(this, "Failed to add admin.", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            Toast.makeText(this, "Signup failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });

    }

    private void showChangeNameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        EditText nameInput = new EditText(this);
        nameInput.setHint("Enter new name");
        builder.setTitle("Change Name");
        builder.setView(nameInput);
        builder.setPositiveButton("Update", (dialog, which) -> {
            String newName = nameInput.getText().toString().trim();
            if (!newName.isEmpty()) {
                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
                userRef.child("name").setValue(newName);
                Toast.makeText(this, "Name updated successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showChangeEmailDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        EditText emailInput = new EditText(this);
        emailInput.setHint("Enter new email");
        builder.setTitle("Change Email");
        builder.setView(emailInput);
        builder.setPositiveButton("Update", (dialog, which) -> {
            String newEmail = emailInput.getText().toString().trim();
            if (!newEmail.isEmpty()) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                user.updateEmail(newEmail).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseDatabase.getInstance().getReference("users")
                                .child(user.getUid()).child("email").setValue(newEmail);
                        Toast.makeText(this, "Email updated successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Failed to update email: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                Toast.makeText(this, "Email cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        EditText passwordInput = new EditText(this);
        passwordInput.setHint("Enter new password");
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setTitle("Change Password");
        builder.setView(passwordInput);
        builder.setPositiveButton("Update", (dialog, which) -> {
            String newPassword = passwordInput.getText().toString().trim();
            if (newPassword.length() < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            user.updatePassword(newPassword).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Password updated successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Failed to update password: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}
