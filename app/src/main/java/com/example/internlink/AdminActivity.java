package com.example.internlink;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class AdminActivity extends AppCompatActivity {
    private TextView adminName;
    private DatabaseReference databaseReference;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        adminName = findViewById(R.id.adminName);

        // Get Firebase reference for admins
        databaseReference = FirebaseDatabase.getInstance().getReference("users");

        // Get current admin's UID (assuming the UID is stored in SharedPreferences or FirebaseAuth)
        String adminUid = getCurrentAdminUid(); // You need to implement this method

        // Fetch admin data from Firebase
        databaseReference.child(adminUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String name = "Welcome, "+dataSnapshot.child("name").getValue(String.class);
                    adminName.setText(name);  // Set the admin's name to the TextView
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle error
            }
        });

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
    private String getCurrentAdminUid() {
        return FirebaseAuth.getInstance().getCurrentUser().getUid();
    }
}