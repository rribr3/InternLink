package com.example.internlink;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class CompanyHomeActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener,
        EnhancedApplicantsAdapter.OnApplicantActionListener {

    private DrawerLayout drawerLayout;
    private TextView welcomeText;
    private ImageView notificationBell;
    private TextView notificationBadge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_company_home);

        initializeViews();
        setupNavigationDrawer();
        setupDashboardContent();
        setupClickListeners();
        setupNotificationBell();
    }
    private void setupNotificationBell() {
        DatabaseReference globalRef = FirebaseDatabase.getInstance().getReference("announcements");
        DatabaseReference roleRef = FirebaseDatabase.getInstance().getReference("announcements_by_role").child("company");

        globalRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot globalSnapshot) {
                final int[] totalCount = {0};

                totalCount[0] += (int) globalSnapshot.getChildrenCount();

                roleRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot roleSnapshot) {
                        totalCount[0] += (int) roleSnapshot.getChildrenCount();

                        if (totalCount[0] > 0) {
                            notificationBadge.setVisibility(View.VISIBLE);
                            notificationBadge.setText(String.valueOf(totalCount[0]));
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


    private void initializeViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        welcomeText = findViewById(R.id.welcome_text);
        notificationBell = findViewById(R.id.notification_bell);
        notificationBadge = findViewById(R.id.notification_badge);
    }

    private void setupNavigationDrawer() {
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        View headerView = navigationView.getHeaderView(0);
        TextView companyName = headerView.findViewById(R.id.company_name);

        if (companyName != null) {
            companyName.setText("TechCorp Inc.");
        } else {
            Toast.makeText(this, "Company name TextView not found in header", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupDashboardContent() {
        welcomeText.setText("Welcome back, TechCorp");
        setupProjectsRecyclerView();
        setupApplicantsRecyclerView();
        updateNotificationBadge(5);
    }

    private void setupProjectsRecyclerView() {
        RecyclerView projectsRecycler = findViewById(R.id.projects_recycler_view);
        projectsRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        List<EmployerProject> projects = getSampleProjects();
        CompanyProjectsAdapter adapter = new CompanyProjectsAdapter(projects);
        projectsRecycler.setAdapter(adapter);
    }

    private void setupApplicantsRecyclerView() {
        RecyclerView applicantsRecycler = findViewById(R.id.applicants_recycler_view);
        applicantsRecycler.setLayoutManager(new LinearLayoutManager(this));
        List<Applicant> applicants = getSampleApplicants();
        ApplicantsAdapter adapter = new ApplicantsAdapter(applicants);
        applicantsRecycler.setAdapter(adapter);
    }

    private void updateNotificationBadge(int count) {
        if (count > 0) {
            notificationBadge.setVisibility(View.VISIBLE);
            notificationBadge.setText(String.valueOf(count));
        } else {
            notificationBadge.setVisibility(View.GONE);
        }
    }

    private List<EmployerProject> getSampleProjects() {
        List<EmployerProject> projects = new ArrayList<>();
        projects.add(new EmployerProject("Mobile Developer", 12, 3, R.drawable.ic_edit));
        projects.add(new EmployerProject("Data Scientist", 8, 5, R.drawable.ic_pending));
        projects.add(new EmployerProject("UX Designer", 15, 2, R.drawable.ic_approve));
        return projects;
    }

    private List<Applicant> getSampleApplicants() {
        List<Applicant> applicants = new ArrayList<>();
        applicants.add(new Applicant("Sarah Johnson", "Mobile Developer", "Shortlisted", R.drawable.ic_profile));
        applicants.add(new Applicant("Michael Chen", "Data Scientist", "New", R.drawable.ic_profile));
        applicants.add(new Applicant("Emma Wilson", "UX Designer", "Interview", R.drawable.ic_profile));
        return applicants;
    }

    private void setupClickListeners() {
        findViewById(R.id.logo).setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        notificationBell.setOnClickListener(v -> {
            Intent intent = new Intent(CompanyHomeActivity.this, CompanyAnnounce.class);
            startActivity(intent);
        });
        findViewById(R.id.fab_create_project).setOnClickListener(v -> Toast.makeText(this, "Create new project", Toast.LENGTH_SHORT).show());

        // Add click listeners for view all buttons
        findViewById(R.id.view_all_projects_button).setOnClickListener(v -> showAllProjectsDialog());
        findViewById(R.id.view_all_applicants_button).setOnClickListener(v -> showAllApplicantsDialog());
    }

    private void showAllProjectsDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_all_projects);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.setCancelable(true);

        RecyclerView recyclerView = dialog.findViewById(R.id.all_projects_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        List<EmployerProject> allProjects = getSampleProjects();
        ProjectDetailsAdapter adapter = new ProjectDetailsAdapter(allProjects, project ->
                Toast.makeText(this, "Selected: " + project.getTitle(), Toast.LENGTH_SHORT).show()
        );
        recyclerView.setAdapter(adapter);

        ImageView btnClose = dialog.findViewById(R.id.btn_close);
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showAllApplicantsDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_all_applicants);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
        dialog.setCancelable(true);

        RecyclerView recyclerView = dialog.findViewById(R.id.all_applicants_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        List<Applicant> allApplicants = getSampleApplicants();
        EnhancedApplicantsAdapter adapter = new EnhancedApplicantsAdapter(allApplicants, this);
        recyclerView.setAdapter(adapter);

        ImageView btnClose = dialog.findViewById(R.id.btn_close_applicants);
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    // Implement OnApplicantActionListener methods
    @Override
    public void onViewProfile(Applicant applicant) {
        Toast.makeText(this, "View profile: " + applicant.getName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onScheduleInterview(Applicant applicant) {
        Toast.makeText(this, "Schedule interview with: " + applicant.getName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onChat(Applicant applicant) {
        Toast.makeText(this, "Chat with: " + applicant.getName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onMoreOptions(Applicant applicant, View anchorView) {
        showApplicantOptionsMenu(applicant, anchorView);
    }

    @SuppressLint("NonConstantResourceId")
    private void showApplicantOptionsMenu(Applicant applicant, View anchorView) {
        PopupMenu popup = new PopupMenu(this, anchorView);
        popup.getMenuInflater().inflate(R.menu.applicant_options_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.menu_download_cv) {
                Toast.makeText(this, "Download CV: " + applicant.getName(), Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.menu_reject) {
                Toast.makeText(this, "Rejected: " + applicant.getName(), Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.menu_shortlist) {
                Toast.makeText(this, "Shortlisted: " + applicant.getName(), Toast.LENGTH_SHORT).show();
                return true;
            } else {
                return false;
            }
        });

        popup.show();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_dashboard) {
            // Dashboard clicked
        } else if (id == R.id.nav_profile) {
            showProfile();
        } else if (id == R.id.nav_projects) {
            Toast.makeText(this, "My Projects", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_applicant) {
            Toast.makeText(this, "Applicants", Toast.LENGTH_SHORT).show();
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

    private void showProfile() {
        Toast.makeText(this, "Opening profile", Toast.LENGTH_SHORT).show();
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