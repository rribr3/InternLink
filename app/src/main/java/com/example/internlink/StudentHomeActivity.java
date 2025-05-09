package com.example.internlink;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class StudentHomeActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private TextView welcomeText;
    private ImageView notificationBell;
    private TextView notificationBadge;
    private RecyclerView projectsRecyclerView;
    private ProjectAdapterHome projectAdapterHome;
    private List<Project> allProjects;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_home);

        // Initialize
        allProjects = getSampleProjects();

        initializeViews();
        setupNavigationDrawer();
        setWelcomeMessage();
        setupNotificationBell();
        setupProjectsRecyclerView();
        setupClickListeners();
    }

    private void initializeViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        welcomeText = findViewById(R.id.welcome_text);
        notificationBell = findViewById(R.id.notification_bell);
        notificationBadge = findViewById(R.id.notification_badge);
        projectsRecyclerView = findViewById(R.id.projects_recycler_view);

        // Set up logo click to open navigation drawer
        ImageView logo = findViewById(R.id.logo);
        logo.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
    }

    private void setupNavigationDrawer() {
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Remove toolbar navigation since we're using the logo
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(null); // Remove hamburger icon
    }

    private void setWelcomeMessage() {
        String userName = "John Doe"; // Replace with actual user name
        welcomeText.setText(String.format("Welcome back, %s", userName));
    }

    private void setupNotificationBell() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userReadsRef = FirebaseDatabase.getInstance().getReference("user_reads").child(userId);
        DatabaseReference globalRef = FirebaseDatabase.getInstance().getReference("announcements");
        DatabaseReference roleRef = FirebaseDatabase.getInstance().getReference("announcements_by_role").child("student");

        userReadsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot readsSnapshot) {
                final List<String> unreadIds = new ArrayList<>();

                globalRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot globalSnapshot) {
                        for (DataSnapshot snap : globalSnapshot.getChildren()) {
                            if (!readsSnapshot.hasChild(snap.getKey())) {
                                unreadIds.add(snap.getKey());
                            }
                        }

                        roleRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot roleSnapshot) {
                                for (DataSnapshot snap : roleSnapshot.getChildren()) {
                                    if (!readsSnapshot.hasChild(snap.getKey())) {
                                        unreadIds.add(snap.getKey());
                                    }
                                }

                                // Update the badge
                                if (!unreadIds.isEmpty()) {
                                    notificationBadge.setVisibility(View.VISIBLE);
                                    notificationBadge.setText(String.valueOf(unreadIds.size()));
                                } else {
                                    notificationBadge.setVisibility(View.GONE);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                notificationBadge.setVisibility(View.GONE);
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        notificationBadge.setVisibility(View.GONE);
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                notificationBadge.setVisibility(View.GONE);
            }
        });

        notificationBell.setOnClickListener(v -> {
            Intent intent = new Intent(StudentHomeActivity.this, StudentAnnounce.class);
            startActivity(intent);
        });
    }
    @Override
    protected void onResume() {
        super.onResume();
        setupNotificationBell(); // Refresh the badge when returning to this screen
    }


    private void setupProjectsRecyclerView() {
        projectsRecyclerView.setLayoutManager(new LinearLayoutManager(
                this, LinearLayoutManager.HORIZONTAL, false));

        projectAdapterHome = new ProjectAdapterHome(allProjects, project -> {
            Toast.makeText(this, "Clicked: " + project.getTitle(), Toast.LENGTH_SHORT).show();
        }, true); // true for horizontal layout
        projectsRecyclerView.setAdapter(projectAdapterHome);
    }

    private List<Project> getSampleProjects() {
        return java.util.Collections.emptyList();
    }

    private void setupClickListeners() {
        findViewById(R.id.view_all_applications_btn).setOnClickListener(v -> showAllApplications());
        findViewById(R.id.see_all_tips_btn).setOnClickListener(v -> showAllTips());
        findViewById(R.id.btn_view_all_projects).setOnClickListener(v -> showAllProjectsPopup());
        notificationBell.setOnClickListener(v -> {
            // Navigate to CompanyAnnounce.java
            Intent intent = new Intent(StudentHomeActivity.this, StudentAnnounce.class);
            startActivity(intent);
        });
    }

    private void showAllProjectsPopup() {
        // Inflate the popup layout
        View popupView = LayoutInflater.from(this).inflate(R.layout.popup_all_projects, null);

        // Setup RecyclerView
        RecyclerView rvAllProjects = popupView.findViewById(R.id.rv_all_projects);
        rvAllProjects.setLayoutManager(new LinearLayoutManager(this));

        ProjectAdapterHome adapter = new ProjectAdapterHome(allProjects, project -> {
            Toast.makeText(this, "Selected: " + project.getTitle(), Toast.LENGTH_SHORT).show();
        }, false); // false for vertical layout

        rvAllProjects.setAdapter(adapter);

        // Create and show the popup
        PopupWindow popupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );

        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setAnimationStyle(R.style.PopupAnimation);

        popupWindow.showAtLocation(
                findViewById(android.R.id.content),
                Gravity.CENTER,
                0,
                0
        );
    }

    private void showProfile() {
        Toast.makeText(this, "Opening profile", Toast.LENGTH_SHORT).show();
    }

    private void showAllApplications() {
        // Inflate the popup layout
        View popupView = LayoutInflater.from(this).inflate(R.layout.popup_all_applications, null);

        // Initialize views
        RecyclerView rvApplications = popupView.findViewById(R.id.rv_applications);
        FloatingActionButton addButton = popupView.findViewById(R.id.btn_add_application);
        ImageView closeButton = popupView.findViewById(R.id.btn_close_popup);

        // Sample data – replace with actual application model
        List<String> applications = new ArrayList<>();
        applications.add("UI/UX Designer – Pending");
        applications.add("Android Developer – Interview");
        applications.add("Backend Intern – Accepted");

        // Setup RecyclerView
        rvApplications.setLayoutManager(new LinearLayoutManager(this));
        rvApplications.setAdapter(new ApplicationAdapter(applications));

        // Add new application click
        addButton.setOnClickListener(v -> {
            Toast.makeText(this, "Add new application clicked", Toast.LENGTH_SHORT).show();
            // TODO: Launch add application screen
        });

        // Create full-screen PopupWindow
        final PopupWindow popupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                true
        );
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setAnimationStyle(R.style.PopupAnimation);
        popupWindow.showAtLocation(findViewById(android.R.id.content), Gravity.CENTER, 0, 0);

        // Close popup
        closeButton.setOnClickListener(v -> popupWindow.dismiss());
    }



    private void showAllTips() {
        Toast.makeText(this, "Showing all tips", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_profile) {
            showProfile();
        } else if (id == R.id.nav_projects) {
            Toast.makeText(this, "My Projects", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_applications) {
            showAllApplications();
        } else if (id == R.id.nav_messages) {
            Toast.makeText(this, "Messages", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_notifications) {
            Toast.makeText(this, "Notifications", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_settings) {
            Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_help) {
            Toast.makeText(this, "Help", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_logout) {
            finish();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }


    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}