package com.example.internlink;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;

public class AdminActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        // Dashboard card click
        LinearLayout dashboardCard = findViewById(R.id.dashboard_card);
        dashboardCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AdminActivity.this, DashboardActivity.class);
                startActivity(intent);
            }
        });

        // User Management card click
        LinearLayout userCard = findViewById(R.id.User_card);
        userCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AdminActivity.this, UserManagementActivity.class);
                startActivity(intent);
            }
        });

        LinearLayout projectCard = findViewById(R.id.Project_card);
        projectCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AdminActivity.this, ProjectManagementActivity.class);
                startActivity(intent);
            }
        });

        LinearLayout reportCard = findViewById(R.id.Report_card);
        reportCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AdminActivity.this, ReportsActivity.class);
                startActivity(intent);
            }
        });

        LinearLayout feedbackCard = findViewById(R.id.Feedback_card);
        feedbackCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AdminActivity.this, AdminFeedbackActivity.class);
                startActivity(intent);
            }
        });

        LinearLayout notificationCard = findViewById(R.id.Notification_card);
        notificationCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AdminActivity.this, AdminNotification.class);
                startActivity(intent);
            }
        });

        LinearLayout settingCard = findViewById(R.id.Setting_card);
        settingCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AdminActivity.this, AdminSettings.class);
                startActivity(intent);
            }
        });
    }
}