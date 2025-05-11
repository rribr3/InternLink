package com.example.internlink;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CompanyHomeActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener,
        EnhancedApplicantsAdapter.OnApplicantActionListener {

    private DrawerLayout drawerLayout;
    private TextView welcomeText;
    private ImageView notificationBell;
    private TextView notificationBadge;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("CompanyHomeActivity", "onCreate started");
        setContentView(R.layout.activity_company_home);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String status = snapshot.child("status").getValue(String.class);
                    Long timestamp = snapshot.child("deactivationTimestamp").getValue(Long.class);
                    if ("deactivated".equals(status) && timestamp != null) {
                        long now = System.currentTimeMillis();
                        long monthMillis = 30L * 24 * 60 * 60 * 1000;
                        if (now - timestamp > monthMillis) {
                            snapshot.getRef().removeValue(); // deletes user data
                            currentUser.delete(); // deletes Firebase Auth account
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // Log error if needed
                }
            });
        }

        /*findViewById(R.id.view_details_button).setOnClickListener(v -> {
            // For demonstration, we'll use the first project ID from Firebase
            DatabaseReference projectsRef = FirebaseDatabase.getInstance().getReference("projects");
            projectsRef.limitToFirst(1).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        for (DataSnapshot projectSnapshot : snapshot.getChildren()) {
                            String projectId = projectSnapshot.getKey();
                            Intent intent = new Intent(CompanyHomeActivity.this, ProjectDetailsActivity.class);
                            intent.putExtra("PROJECT_ID", projectId);
                            startActivity(intent);
                        }
                    } else {
                        Toast.makeText(CompanyHomeActivity.this, "No projects found", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(CompanyHomeActivity.this, "Failed to load projects", Toast.LENGTH_SHORT).show();
                }
            });
        });*/

        welcomeText = findViewById(R.id.welcome_text);

        databaseReference = FirebaseDatabase.getInstance().getReference("users");

        String companyUid = getCurrentCompanyUid();

        databaseReference.child(companyUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String name = "Welcome, "+dataSnapshot.child("name").getValue(String.class);
                    welcomeText.setText(name);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle error
            }
        });

        initializeViews();
        setupNavigationDrawer();
        setupDashboardContent();
        fetchRecentCompanyAnnouncements();
        setupClickListeners();
        setupNotificationBell();
        fetchCompanyStats();
        createNotificationChannel();
    }
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "notif_channel_id",
                    "App Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Channel for user notifications");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private String getCurrentCompanyUid() {
        return FirebaseAuth.getInstance().getCurrentUser().getUid();
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
        TextView companyMail = headerView.findViewById(R.id.company_mail);

        String companyUid = getCurrentCompanyUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(companyUid);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);

                    if (name != null) {
                        companyName.setText(name);
                    }

                    if (email != null) {
                        companyMail.setText(email);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CompanyHomeActivity.this, "Failed to load company info", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void setupDashboardContent() {
        setupProjectsRecyclerView();
        setupApplicantsRecyclerView();
        updateNotificationBadge(5);
        fetchProjectStats();
    }
    private void fetchProjectStats() {
        String companyId = getCurrentCompanyUid();
        DatabaseReference projectsRef = FirebaseDatabase.getInstance().getReference("projects");

        projectsRef.orderByChild("companyId").equalTo(companyId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int activeCount = 0;
                int pendingCount = 0;
                int completedCount = 0;

                for (DataSnapshot projectSnapshot : snapshot.getChildren()) {
                    String status = projectSnapshot.child("status").getValue(String.class);
                    if (status != null) {
                        switch (status) {
                            case "approved":
                                activeCount++;
                                break;
                            case "pending":
                                pendingCount++;
                                break;
                            case "completed":
                                completedCount++;
                                break;
                        }
                    }
                }

                updateProjectStats(activeCount, pendingCount, completedCount);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CompanyHomeActivity.this, "Failed to load project stats: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateProjectStats(int active, int pending, int completed) {
        // Update the StatItem views with the fetched counts
        StatItem activeProjects = findViewById(R.id.activeProjects);
        StatItem pendingProjects = findViewById(R.id.pendingProjects);
        StatItem completedProjects = findViewById(R.id.completedProjects);

        activeProjects.setCount(active);
        pendingProjects.setCount(pending);
        completedProjects.setCount(completed);
    }

    private void setupProjectsRecyclerView() {
        RecyclerView projectsRecycler = findViewById(R.id.projects_recycler_view);
        projectsRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        getSampleProjectsFromFirebase(new ProjectsCallback() {
            @Override
            public void onProjectsLoaded(List<EmployerProject> projects) {
                CompanyProjectsAdapter adapter = new CompanyProjectsAdapter(projects);
                projectsRecycler.setAdapter(adapter);
            }
        });
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

    private void getSampleProjectsFromFirebase(ProjectsCallback callback) {
        String companyId = getCurrentCompanyUid();
        DatabaseReference projectsRef = FirebaseDatabase.getInstance().getReference("projects");
        List<EmployerProject> projects = new ArrayList<>();

        projectsRef.orderByChild("companyId").equalTo(companyId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                projects.clear();
                for (DataSnapshot projectSnapshot : snapshot.getChildren()) {
                    String projectId = projectSnapshot.getKey();
                    String title = projectSnapshot.child("title").getValue(String.class);
                    Long positionsCount = projectSnapshot.child("studentsRequired").getValue(Long.class);
                    Long applicantsCount = projectSnapshot.child("applicants").getChildrenCount();
                    String status = projectSnapshot.child("status").getValue(String.class);

                    int projectIcon;
                    if ("approved".equals(status)) {
                        projectIcon = R.drawable.ic_edit;
                    } else if ("pending".equals(status)) {
                        projectIcon = R.drawable.ic_pending;
                    } else if ("completed".equals(status)) {
                        projectIcon = R.drawable.ic_approve;
                    } else if ("rejected".equals(status)) {
                        projectIcon = R.drawable.ic_reject;
                    } else {
                        projectIcon = R.drawable.ic_project;
                    }

                    EmployerProject project = new EmployerProject(
                            title != null ? title : "Untitled Project",
                            applicantsCount != null ? applicantsCount.intValue() : 0,
                            positionsCount != null ? positionsCount.intValue() : 0,
                            projectIcon
                    );

                    project.setProjectId(projectId); // Set the project ID for deletion
                    projects.add(project);
                }
                callback.onProjectsLoaded(projects);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CompanyHomeActivity.this, "Failed to load projects: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public interface ProjectsCallback {
        void onProjectsLoaded(List<EmployerProject> projects);
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
        findViewById(R.id.fab_create_project).setOnClickListener(v -> {
            Intent intent = new Intent(CompanyHomeActivity.this, CreateProject.class);
            startActivity(intent);
        });

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

        getSampleProjectsFromFirebase(new ProjectsCallback() {
            @Override
            public void onProjectsLoaded(List<EmployerProject> projects) {
                ProjectDetailsAdapter adapter = new ProjectDetailsAdapter(projects, project ->
                        Toast.makeText(CompanyHomeActivity.this, "Selected: " + project.getTitle(), Toast.LENGTH_SHORT).show()
                );
                recyclerView.setAdapter(adapter);
            }
        });

        ImageView btnClose = dialog.findViewById(R.id.btn_close);
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void fetchRecentCompanyAnnouncements() {
        DatabaseReference globalRef = FirebaseDatabase.getInstance().getReference("announcements");
        DatabaseReference roleRef = FirebaseDatabase.getInstance().getReference("announcements_by_role").child("company");

        List<DataSnapshot> allAnnouncements = new ArrayList<>();

        ValueEventListenerCollector collector = new ValueEventListenerCollector(2, allAnnouncements, () -> {
            processAndDisplayRecentAnnouncements(allAnnouncements);
        });

        globalRef.addListenerForSingleValueEvent(collector.getListener());
        roleRef.addListenerForSingleValueEvent(collector.getListener());
    }
    private static class ValueEventListenerCollector {
        private final int expected;
        private int count = 0;
        private final List<DataSnapshot> collectedSnapshots;
        private final Runnable onAllCollected;

        public ValueEventListenerCollector(int expectedCalls, List<DataSnapshot> snapshots, Runnable callback) {
            this.expected = expectedCalls;
            this.collectedSnapshots = snapshots;
            this.onAllCollected = callback;
        }

        public ValueEventListener getListener() {
            return new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot snap : snapshot.getChildren()) {
                        collectedSnapshots.add(snap);
                    }
                    count++;
                    if (count == expected) {
                        onAllCollected.run();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    count++;
                    if (count == expected) {
                        onAllCollected.run();
                    }
                }
            };
        }
    }



    private void processAndDisplayRecentAnnouncements(List<DataSnapshot> allAnnouncements) {
        List<AnnouncementItem> announcementList = new ArrayList<>();

        for (DataSnapshot snap : allAnnouncements) {
            String title = snap.child("title").getValue(String.class);
            String message = snap.child("message").getValue(String.class);
            Long timestamp = snap.child("timestamp").getValue(Long.class);
            if (title != null && message != null && timestamp != null) {
                announcementList.add(new AnnouncementItem(title, message, timestamp));
            }
        }

        // Sort by timestamp descending
        Collections.sort(announcementList, (a1, a2) -> Long.compare(a2.timestamp, a1.timestamp));

        // Limit to 3
        List<AnnouncementItem> topThree = announcementList.subList(0, Math.min(3, announcementList.size()));

        LinearLayout feedLayout = findViewById(R.id.notification_feed_container); // Add this ID in XML
        feedLayout.removeAllViews();

        for (AnnouncementItem item : topThree) {
            View view = LayoutInflater.from(this).inflate(R.layout.item_notification, feedLayout, false);
            ((TextView) view.findViewById(R.id.notification_text)).setText(item.title + ": " + item.message);

            long now = System.currentTimeMillis();
            long diffMillis = now - item.timestamp;

            String timeAgo;
            long hours = diffMillis / (1000 * 60 * 60);
            if (hours < 24) {
                timeAgo = hours + " hours ago";
            } else {
                long days = hours / 24;
                if (days < 30) {
                    timeAgo = days + (days == 1 ? " day ago" : " days ago");
                } else {
                    long months = days / 30;
                    timeAgo = months + (months == 1 ? " month ago" : " months ago");
                }
            }

            ((TextView) view.findViewById(R.id.notification_time)).setText(timeAgo);


            feedLayout.addView(view);
        }
    }

    private static class AnnouncementItem {
        String title, message;
        long timestamp;

        AnnouncementItem(String title, String message, long timestamp) {
            this.title = title;
            this.message = message;
            this.timestamp = timestamp;
        }
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
        Intent intent;
        if (id == R.id.nav_dashboard) {
            // Dashboard clicked
        } else if (id == R.id.nav_profile) {
            Toast.makeText(this, "Profile", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_projects) {
            intent = new Intent(this, MyProjectsActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_applicant) {
            Toast.makeText(this, "Applicants", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_messages) {
            Toast.makeText(this, "Messages", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_notifications) {
            intent = new Intent(this, CompanyAnnounce.class);
            startActivity(intent);
        } else if (id == R.id.nav_settings) {
            intent = new Intent(this, CompanySettingsActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_help) {
            Toast.makeText(this, "Help", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_logout) {
            finish();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
    private void fetchCompanyStats() {
        String companyId = getCurrentCompanyUid();
        DatabaseReference projectsRef = FirebaseDatabase.getInstance().getReference("projects");

        projectsRef.orderByChild("companyId").equalTo(companyId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int totalProjects = 0;
                int totalApplicants = 0;
                int totalHired = 0;

                // Count all projects and applicants for this company
                for (DataSnapshot projectSnapshot : snapshot.getChildren()) {
                    totalProjects++;

                    // Get number of applicants for this project
                    Long applicants = projectSnapshot.child("applicants").getValue(Long.class);
                    if (applicants != null) {
                        totalApplicants += applicants;
                    }

                    // If you have a "hired" field in your project, count hired applicants
                    if (projectSnapshot.hasChild("hired")) {
                        Long hired = projectSnapshot.child("hired").getValue(Long.class);
                        if (hired != null) {
                            totalHired += hired;
                        }
                    }
                }

                // Update the UI
                updateCompanyStats(totalProjects, totalApplicants, totalHired);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CompanyHomeActivity.this, "Failed to load company stats", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateCompanyStats(int totalProjects, int totalApplicants, int totalHired) {
        TextView tvTotalProjects = findViewById(R.id.total_projects);
        TextView tvTotalApplicants = findViewById(R.id.total_applicants);
        TextView tvTotalHired = findViewById(R.id.total_hired);

        tvTotalProjects.setText(String.valueOf(totalProjects));
        tvTotalApplicants.setText(String.valueOf(totalApplicants));
        tvTotalHired.setText(String.valueOf(totalHired));
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