package com.example.internlink;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
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
    private Button sendAnnouncementBtn, notifyCompanyBtn, notifyStudentBtn, notifySpecificBtn;
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

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference announcementsRef = database.getReference("announcements");

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

        notifySpecificBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = titleInput.getText().toString().trim();
                String message = announcementInput.getText().toString().trim();
                if (title.isEmpty() || message.isEmpty()) {
                    Toast.makeText(AdminNotification.this, "Please enter both title and message", Toast.LENGTH_SHORT).show();
                } else {
                    // Replace this with logic to pick specific users or groups by email
                    Toast.makeText(AdminNotification.this, "Opened specific user/group selection", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}