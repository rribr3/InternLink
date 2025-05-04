package com.example.internlink;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class AdminNotification extends AppCompatActivity {
    private AppCompatButton backButton;
    private EditText titleInput, announcementInput;
    private AutoCompleteTextView specificUserDropdown;
    private Button sendAnnouncementBtn, notifyCompanyBtn, notifyStudentBtn, notifySpecificBtn;
    private List<String> userEmails = new ArrayList<>();
    private ArrayAdapter<String> emailAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_notification);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        titleInput = findViewById(R.id.titleInput);
        announcementInput = findViewById(R.id.announcementInput);
        sendAnnouncementBtn = findViewById(R.id.sendAnnouncementBtn);
        notifyCompanyBtn = findViewById(R.id.notifyCompanyBtn);
        notifyStudentBtn = findViewById(R.id.notifyStudentBtn);
        notifySpecificBtn = findViewById(R.id.notifySpecificBtn);
        specificUserDropdown = findViewById(R.id.specificUserDropdown);


        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference announcementsRef = database.getReference("announcements");

        // Load all user emails for dropdown
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users"); // adjust node if needed
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                userEmails.clear();
                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    String email = userSnap.child("email").getValue(String.class);
                    if (email != null) {
                        userEmails.add(email);
                    }
                }
                emailAdapter = new ArrayAdapter<>(AdminNotification.this,
                        android.R.layout.simple_dropdown_item_1line, userEmails);
                specificUserDropdown.setAdapter(emailAdapter);
                specificUserDropdown.setThreshold(1);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(AdminNotification.this, "Failed to load users", Toast.LENGTH_SHORT).show();
            }
        });

        sendAnnouncementBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = titleInput.getText().toString().trim();
                String message = announcementInput.getText().toString().trim();

                if (title.isEmpty() || message.isEmpty()) {
                    Toast.makeText(AdminNotification.this, "Please enter both title and message", Toast.LENGTH_SHORT).show();
                } else {
                    String announcementId = announcementsRef.push().getKey(); // Unique ID

                    Map<String, Object> announcement = new HashMap<>();
                    announcement.put("title", title);
                    announcement.put("message", message);
                    announcement.put("timestamp", System.currentTimeMillis());

                    if (announcementId != null) {
                        announcementsRef.child(announcementId).setValue(announcement)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(AdminNotification.this, "Announcement sent to all users", Toast.LENGTH_SHORT).show();
                                    titleInput.setText("");
                                    announcementInput.setText("");
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(AdminNotification.this, "Failed to send announcement", Toast.LENGTH_SHORT).show();
                                });
                    }
                }
            }
        });

        notifyCompanyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = titleInput.getText().toString().trim();
                String message = announcementInput.getText().toString().trim();

                if (title.isEmpty() || message.isEmpty()) {
                    Toast.makeText(AdminNotification.this, "Please enter both title and message", Toast.LENGTH_SHORT).show();
                } else {
                    DatabaseReference roleRef = FirebaseDatabase.getInstance()
                            .getReference("announcements_by_role/company");
                    String announcementId = roleRef.push().getKey();

                    Map<String, Object> announcement = new HashMap<>();
                    announcement.put("title", title);
                    announcement.put("message", message);
                    announcement.put("timestamp", System.currentTimeMillis());

                    if (announcementId != null) {
                        roleRef.child(announcementId).setValue(announcement)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(AdminNotification.this, "Announcement sent to companies", Toast.LENGTH_SHORT).show();
                                    titleInput.setText("");
                                    announcementInput.setText("");
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(AdminNotification.this, "Failed to send to companies", Toast.LENGTH_SHORT).show();
                                });
                    }
                }
            }
        });


        notifyStudentBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = titleInput.getText().toString().trim();
                String message = announcementInput.getText().toString().trim();

                if (title.isEmpty() || message.isEmpty()) {
                    Toast.makeText(AdminNotification.this, "Please enter both title and message", Toast.LENGTH_SHORT).show();
                } else {
                    DatabaseReference roleRef = FirebaseDatabase.getInstance()
                            .getReference("announcements_by_role/student");
                    String announcementId = roleRef.push().getKey();

                    Map<String, Object> announcement = new HashMap<>();
                    announcement.put("title", title);
                    announcement.put("message", message);
                    announcement.put("timestamp", System.currentTimeMillis());

                    if (announcementId != null) {
                        roleRef.child(announcementId).setValue(announcement)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(AdminNotification.this, "Announcement sent to students", Toast.LENGTH_SHORT).show();
                                    titleInput.setText("");
                                    announcementInput.setText("");
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(AdminNotification.this, "Failed to send to students", Toast.LENGTH_SHORT).show();
                                });
                    }
                }
            }
        });

        notifySpecificBtn.setOnClickListener(v -> {
            String title = titleInput.getText().toString().trim();
            String message = announcementInput.getText().toString().trim();
            String userEmail = specificUserDropdown.getText().toString().trim().replace(".", "_");

            if (title.isEmpty() || message.isEmpty() || userEmail.isEmpty()) {
                Toast.makeText(AdminNotification.this, "Please enter title, message, and select a user", Toast.LENGTH_SHORT).show();
            } else {
                DatabaseReference userNotifRef = FirebaseDatabase.getInstance()
                        .getReference("announcements_by_user").child(userEmail);
                String announcementId = userNotifRef.push().getKey();

                Map<String, Object> announcement = new HashMap<>();
                announcement.put("title", title);
                announcement.put("message", message);
                announcement.put("timestamp", System.currentTimeMillis());

                if (announcementId != null) {
                    userNotifRef.child(announcementId).setValue(announcement)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(AdminNotification.this, "Announcement sent to " + userEmail.replace("_", "."), Toast.LENGTH_SHORT).show();
                                titleInput.setText("");
                                announcementInput.setText("");
                                specificUserDropdown.setText("");
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(AdminNotification.this, "Failed to send to user", Toast.LENGTH_SHORT).show();
                            });
                }
            }
        });

    }
}