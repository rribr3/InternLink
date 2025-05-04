package com.example.internlink;

import android.os.Bundle;
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

        // Send to All Users
        sendAnnouncementBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = titleInput.getText().toString().trim();
                String message = announcementInput.getText().toString().trim();
                if (title.isEmpty() || message.isEmpty()) {
                    Toast.makeText(AdminNotification.this, "Please enter both title and message", Toast.LENGTH_SHORT).show();
                } else {
                    // Send to all users (replace with real logic)
                    Toast.makeText(AdminNotification.this, "Announcement sent to all users", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Notify Active Users
        notifyCompanyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = titleInput.getText().toString().trim();
                String message = announcementInput.getText().toString().trim();
                if (title.isEmpty() || message.isEmpty()) {
                    Toast.makeText(AdminNotification.this, "Please enter both title and message", Toast.LENGTH_SHORT).show();
                } else {
                    // Replace this with actual logic to notify only active users
                    Toast.makeText(AdminNotification.this, "Notified all companies", Toast.LENGTH_SHORT).show();
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
                    // Replace this with actual logic to notify only active users
                    Toast.makeText(AdminNotification.this, "Notified all students", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Notify Specific Users
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