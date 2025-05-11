package com.example.internlink;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import android.Manifest;


public class CompanySettingsActivity extends AppCompatActivity {

    private TextView tvCompanyName, tvCompanyEmail, tvNotifications;
    private LinearLayout layoutChangePassword, layoutNotifications, layoutTerms, layoutLogout, layoutDeactivateAccount;
    private DatabaseReference databaseReference;
    private SwitchCompat switchNotifications;
    private ImageButton backButton;
    private CompoundButton.OnCheckedChangeListener notificationSwitchListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_company_settings);

        tvCompanyName = findViewById(R.id.tv_company_name);
        tvCompanyEmail = findViewById(R.id.tv_company_email);
        layoutChangePassword = findViewById(R.id.layout_change_password);
        layoutNotifications = findViewById(R.id.layout_notifications);
        layoutTerms = findViewById(R.id.layout_terms);
        layoutLogout = findViewById(R.id.layout_logout);
        layoutDeactivateAccount = findViewById(R.id.layout_deactivate_account);
        tvNotifications = findViewById(R.id.tv_notifications);
        switchNotifications = findViewById(R.id.switch_notifications);
        backButton = findViewById(R.id.btn_back);

        backButton.setOnClickListener(v -> finish());


        String user_Id = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference notifRef = FirebaseDatabase.getInstance().getReference("users").child(user_Id).child("notifications");

        notifRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                switchNotifications.setOnCheckedChangeListener(null); // remove temporarily

                if (snapshot.exists()) {
                    Boolean enabled = snapshot.getValue(Boolean.class);
                    switchNotifications.setChecked(enabled != null && enabled);
                } else {
                    notifRef.setValue(false);
                    switchNotifications.setChecked(false);
                }

                switchNotifications.setOnCheckedChangeListener(notificationSwitchListener); // reattach
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });



        notificationSwitchListener = (buttonView, isChecked) -> {
            notifRef.setValue(isChecked);
            if (isChecked) {
                sendLocalNotification("Notifications enabled", "You will now receive updates.");
            } else {
                cancelAllNotifications();
            }
            Toast.makeText(this, "Notifications " + (isChecked ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
        };




        fetchCompanyInfo();

        layoutChangePassword.setOnClickListener(v -> {
            View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null);
            EditText currentPass = dialogView.findViewById(R.id.current_password);
            EditText newPass = dialogView.findViewById(R.id.new_password);
            TextView forgotPassword = dialogView.findViewById(R.id.forgot_password);

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("Change Password")
                    .setView(dialogView)
                    .setPositiveButton("Submit", (dialogInterface, i) -> {
                        String current = currentPass.getText().toString().trim();
                        String newP = newPass.getText().toString().trim();
                        // TODO: Validate and call update password logic
                        updatePassword(current, newP);
                    })
                    .setNegativeButton("Cancel", null)
                    .create();

            dialog.show();

            forgotPassword.setOnClickListener(fv -> {
                FirebaseAuth auth = FirebaseAuth.getInstance();
                String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
                if (email != null) {
                    auth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Reset link sent to your email", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Error sending reset email", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        });

        layoutNotifications.setOnClickListener(v -> {
            Toast.makeText(this, "Notification settings clicked", Toast.LENGTH_SHORT).show();
        });

        layoutTerms.setOnClickListener(v -> {
            Toast.makeText(this, "Terms and Privacy", Toast.LENGTH_SHORT).show();
        });

        layoutLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
        layoutDeactivateAccount.setOnClickListener(v -> {
            new AlertDialog.Builder(CompanySettingsActivity.this)
                    .setTitle("Deactivate Account")
                    .setMessage("Are you sure you want to deactivate your account?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                        if (currentUser != null) {
                            String userId = currentUser.getUid();
                            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

                            // Set "status" and a timestamp
                            userRef.child("status").setValue("deactivated");
                            userRef.child("deactivationTimestamp").setValue(System.currentTimeMillis());

                            FirebaseAuth.getInstance().signOut();
                            Toast.makeText(CompanySettingsActivity.this, "Account deactivated", Toast.LENGTH_SHORT).show();

                            startActivity(new Intent(CompanySettingsActivity.this, LoginActivity.class)
                                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                            finish();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

    }
    @SuppressLint("MissingPermission")
    private void sendLocalNotification(String title, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "notif_channel_id")
                .setSmallIcon(R.drawable.notification) // use your own icon
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(1, builder.build());
    }
    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }


    private void cancelAllNotifications() {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.cancelAll();
    }

    private void updatePassword(String currentPassword, String newPassword) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) return;

        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);

        user.reauthenticate(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                user.updatePassword(newPassword).addOnCompleteListener(updateTask -> {
                    if (updateTask.isSuccessful()) {
                        Toast.makeText(this, "Password updated successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Failed to update password", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(this, "Reauthentication failed. Check current password.", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void fetchCompanyInfo() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseReference = FirebaseDatabase.getInstance().getReference("users").child(uid);

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String name = snapshot.child("name").getValue(String.class);
                String email = snapshot.child("email").getValue(String.class);

                if (name != null) tvCompanyName.setText(name);
                if (email != null) tvCompanyEmail.setText(email);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CompanySettingsActivity.this, "Failed to load info", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
