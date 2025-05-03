package com.example.internlink;

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

import com.google.android.material.navigation.NavigationView;

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
    }

    private void setupNavigationDrawer() {
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(view -> drawerLayout.openDrawer(GravityCompat.START));
    }

    private void setWelcomeMessage() {
        String userName = "John Doe";
        welcomeText.setText(String.format("Welcome back, %s", userName));
    }

    private void setupNotificationBell() {
        boolean hasNewNotifications = true;
        if (hasNewNotifications) {
            notificationBadge.setVisibility(View.VISIBLE);
            notificationBadge.setText("3");
        }

        notificationBell.setOnClickListener(v -> {
            Toast.makeText(this, "Opening notifications", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupProjectsRecyclerView() {
        projectsRecyclerView.setLayoutManager(new LinearLayoutManager(
                this, LinearLayoutManager.HORIZONTAL, false));

        projectAdapterHome = new ProjectAdapterHome(allProjects, project -> {
            Toast.makeText(this, "Clicked: " + project.getTitle(), Toast.LENGTH_SHORT).show();
        }, false);
        projectsRecyclerView.setAdapter(projectAdapterHome);
    }

    private List<Project> getSampleProjects() {
        List<Project> projects = new ArrayList<>();
        projects.add(new Project("Mobile App Development", "Google", R.drawable.googlelogo,
                new String[]{"Android", "Kotlin", "UI/UX"}, "3 days left"));
        projects.add(new Project("Backend API Development", "Amazon", R.drawable.googlelogo,
                new String[]{"Java", "Spring Boot", "AWS"}, "5 days left"));
        projects.add(new Project("Data Science Internship", "Microsoft", R.drawable.ic_microsoft,
                new String[]{"Python", "Machine Learning", "Pandas"}, "2 days left"));
        return projects;
    }

    private void setupClickListeners() {
        findViewById(R.id.profile_avatar).setOnClickListener(v -> showProfile());
        findViewById(R.id.view_all_applications_btn).setOnClickListener(v -> showAllApplications());
        findViewById(R.id.see_all_tips_btn).setOnClickListener(v -> showAllTips());

        // Add click listener for the + button
        findViewById(R.id.btn_view_all_projects).setOnClickListener(v -> showAllProjectsPopup());
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
        Toast.makeText(this, "Showing all applications", Toast.LENGTH_SHORT).show();
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