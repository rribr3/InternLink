package com.example.internlink;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.net.Uri;
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
import android.widget.ProgressBar;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CompanyHomeActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener,
        EnhancedApplicantsAdapter.OnApplicantActionListener {

    private DrawerLayout drawerLayout;
    private TextView welcomeText;
    private ImageView notificationBell, companyLogo;
    private TextView notificationBadge;
    private DatabaseReference databaseReference;
    private LinearLayout dotIndicatorLayout;
    private ProgressBar loadingIndicator;
    private View mainContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("CompanyHomeActivity", "onCreate started");
        setContentView(R.layout.activity_company_home);
        loadingIndicator = findViewById(R.id.home_loading_indicator);
        mainContent = findViewById(R.id.home_main_content);

        loadingIndicator.setVisibility(View.VISIBLE);
        mainContent.setVisibility(View.GONE);

        // Initialize the dot indicator layout
        dotIndicatorLayout = findViewById(R.id.dotIndicatorLayout);

        // For demonstration, let's assume you want 5 dots (e.g., for 5 pages in a view pager)
        int numberOfDots = 5; // This can be dynamically set based on your content
        addDots(numberOfDots);

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

        TextView viewNotification = findViewById(R.id.view_notification);
        viewNotification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CompanyHomeActivity.this, CompanyAnnounce.class);
                startActivity(intent);
            }
        });

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

        companyLogo = findViewById(R.id.companyLogo);

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
        notificationBell = findViewById(R.id.notification_bell);
        notificationBadge = findViewById(R.id.notification_badge);

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userReadsRef = FirebaseDatabase.getInstance().getReference("user_reads").child(userId);
        DatabaseReference globalRef = FirebaseDatabase.getInstance().getReference("announcements");
        DatabaseReference roleRef = FirebaseDatabase.getInstance().getReference("announcements_by_role").child("company");

        ValueEventListener unreadListener = new ValueEventListener() {
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
                                updateNotificationBadge(unreadIds.size());
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e("NotificationBell", "Failed to read role announcements", error.toException());
                                updateNotificationBadge(unreadIds.size());
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("NotificationBell", "Failed to read global announcements", error.toException());
                        updateNotificationBadge(0);
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("NotificationBell", "Failed to read user reads", error.toException());
                updateNotificationBadge(0);
            }
        };

        userReadsRef.addValueEventListener(unreadListener);

        notificationBell.setOnClickListener(v -> {
            markAllAnnouncementsAsRead(userId);
            Intent intent = new Intent(CompanyHomeActivity.this, CompanyAnnounce.class);
            startActivity(intent);
        });
    }

    private void updateNotificationBadge(int unreadCount) {
        if (unreadCount > 0) {
            notificationBadge.setVisibility(View.VISIBLE);
            notificationBadge.setText(unreadCount > 99 ? "99+" : String.valueOf(unreadCount));
        } else {
            notificationBadge.setVisibility(View.GONE);
        }
    }

    private void markAllAnnouncementsAsRead(String userId) {
        DatabaseReference userReadsRef = FirebaseDatabase.getInstance().getReference("user_reads").child(userId);
        DatabaseReference globalRef = FirebaseDatabase.getInstance().getReference("announcements");
        DatabaseReference roleRef = FirebaseDatabase.getInstance().getReference("announcements_by_role").child("company");

        // First get all announcements
        globalRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot globalSnapshot) {
                Map<String, Object> updates = new HashMap<>();

                // Mark global announcements as read
                for (DataSnapshot snap : globalSnapshot.getChildren()) {
                    updates.put(snap.getKey(), true);
                }

                // Then get role-specific announcements
                roleRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot roleSnapshot) {
                        // Mark role announcements as read
                        for (DataSnapshot snap : roleSnapshot.getChildren()) {
                            updates.put(snap.getKey(), true);
                        }

                        // Update user_reads with all announcements
                        if (!updates.isEmpty()) {
                            userReadsRef.updateChildren(updates)
                                    .addOnSuccessListener(aVoid -> {
                                        // Badge will update automatically through the listener
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("NotificationBell", "Failed to mark announcements as read", e);
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("NotificationBell", "Failed to read role announcements", error.toException());
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("NotificationBell", "Failed to read global announcements", error.toException());
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
        StatItem activeProjects = findViewById(R.id.activeProjects);
        StatItem pendingProjects = findViewById(R.id.pendingProjects);
        StatItem completedProjects = findViewById(R.id.completedProjects);

        if (activeProjects != null) activeProjects.setCount(active);
        if (pendingProjects != null) pendingProjects.setCount(pending);
        if (completedProjects != null) completedProjects.setCount(completed);
    }

    private void setupProjectsRecyclerView() {
        RecyclerView projectsRecycler = findViewById(R.id.projects_recycler_view);
        if (projectsRecycler != null) {
            projectsRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

            getSampleProjectsFromFirebase(new ProjectsCallback() {
                @Override
                public void onProjectsLoaded(List<EmployerProject> projects) {
                    CompanyProjectsAdapter adapter = new CompanyProjectsAdapter(projects);
                    projectsRecycler.setAdapter(adapter);

                    // Add dots based on number of projects
                    addDots(projects.size());

                    // Setup RecyclerView scroll listener to update active dot
                    projectsRecycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
                        @Override
                        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                            super.onScrolled(recyclerView, dx, dy);
                            LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                            if (layoutManager != null) {
                                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
                                updateActiveDot(firstVisibleItemPosition);
                            }
                        }
                    });

                    // ✅ Show the content and hide the loader
                    loadingIndicator.setVisibility(View.GONE);
                    mainContent.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    private void addDots(int count) {
        if (dotIndicatorLayout == null || count <= 0) return;

        dotIndicatorLayout.removeAllViews(); // Clear any previous dots

        // Convert dp to pixels
        int sizeInPx = (int) (10 * getResources().getDisplayMetrics().density);
        int marginInPx = (int) (4 * getResources().getDisplayMetrics().density);

        // Loop to create 'count' dots
        for (int i = 0; i < count; i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(sizeInPx, sizeInPx);
            params.setMargins(marginInPx, 0, marginInPx, 0);
            dot.setLayoutParams(params);
            dot.setBackgroundResource(R.drawable.circle_dot);
            dotIndicatorLayout.addView(dot);
        }

        // Set first dot as active initially
        if (dotIndicatorLayout.getChildCount() > 0) {
            dotIndicatorLayout.getChildAt(0).setBackgroundResource(R.drawable.circle_dot_active);
        }
    }

    private void updateActiveDot(int position) {
        if (dotIndicatorLayout == null || position < 0 || position >= dotIndicatorLayout.getChildCount()) {
            return;
        }

        // Reset all dots
        for (int i = 0; i < dotIndicatorLayout.getChildCount(); i++) {
            View dot = dotIndicatorLayout.getChildAt(i);
            dot.setBackgroundResource(R.drawable.circle_dot);
        }

        // Highlight the active dot
        View activeDot = dotIndicatorLayout.getChildAt(position);
        activeDot.setBackgroundResource(R.drawable.circle_dot_active);
    }

    private void setupApplicantsRecyclerView() {
        RecyclerView applicantsRecycler = findViewById(R.id.applicants_recycler_view);
        if (applicantsRecycler != null) {
            applicantsRecycler.setLayoutManager(new LinearLayoutManager(this));

            // Fetch real applicants from Firebase
            fetchRecentApplicantsFromFirebase(applicants -> {
                ApplicantsAdapter adapter = new ApplicantsAdapter(applicants);
                applicantsRecycler.setAdapter(adapter);
            });
        }
    }

    private void fetchRecentApplicantsFromFirebase(ApplicantsCallback callback) {
        String companyId = getCurrentCompanyUid();
        DatabaseReference applicationsRef = FirebaseDatabase.getInstance().getReference("applications");
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        DatabaseReference projectsRef = FirebaseDatabase.getInstance().getReference("projects");

        List<Applicant> applicants = new ArrayList<>();

        applicationsRef.orderByChild("companyId").equalTo(companyId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<DataSnapshot> applicationsList = new ArrayList<>();

                        // Collect all applications
                        for (DataSnapshot appSnapshot : snapshot.getChildren()) {
                            applicationsList.add(appSnapshot);
                        }

                        // Sort by appliedDate (most recent first)
                        Collections.sort(applicationsList, (a, b) -> {
                            Long dateA = a.child("appliedDate").getValue(Long.class);
                            Long dateB = b.child("appliedDate").getValue(Long.class);
                            if (dateA == null) dateA = 0L;
                            if (dateB == null) dateB = 0L;
                            return Long.compare(dateB, dateA); // Descending order
                        });

                        // Take only first 3 applications
                        int limit = Math.min(3, applicationsList.size());
                        final int[] processedCount = {0};

                        if (limit == 0) {
                            callback.onApplicantsLoaded(applicants);
                            return;
                        }

                        for (int i = 0; i < limit; i++) {
                            DataSnapshot appSnapshot = applicationsList.get(i);
                            String userId = appSnapshot.child("userId").getValue(String.class);
                            String projectId = appSnapshot.child("projectId").getValue(String.class);
                            String status = appSnapshot.child("status").getValue(String.class);
                            Long appliedDate = appSnapshot.child("appliedDate").getValue(Long.class);

                            if (userId != null && projectId != null) {
                                // Get user details
                                usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                                        String userName = userSnapshot.child("name").getValue(String.class);

                                        // Get project title
                                        projectsRef.child(projectId).addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot projectSnapshot) {
                                                String projectTitle = projectSnapshot.child("title").getValue(String.class);

                                                if (userName != null && projectTitle != null) {
                                                    Applicant applicant = new Applicant(
                                                            userName,
                                                            projectTitle, // Using project title as position
                                                            status != null ? status : "Pending",
                                                            R.drawable.ic_profile
                                                    );
                                                    applicant.setUserId(userId);
                                                    applicant.setProjectId(projectId);
                                                    applicant.setAppliedDate(appliedDate);
                                                    applicants.add(applicant);
                                                }

                                                processedCount[0]++;
                                                if (processedCount[0] == limit) {
                                                    callback.onApplicantsLoaded(applicants);
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {
                                                processedCount[0]++;
                                                if (processedCount[0] == limit) {
                                                    callback.onApplicantsLoaded(applicants);
                                                }
                                            }
                                        });
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        processedCount[0]++;
                                        if (processedCount[0] == limit) {
                                            callback.onApplicantsLoaded(applicants);
                                        }
                                    }
                                });
                            } else {
                                processedCount[0]++;
                                if (processedCount[0] == limit) {
                                    callback.onApplicantsLoaded(applicants);
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(CompanyHomeActivity.this,
                                "Failed to load applicants: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        callback.onApplicantsLoaded(applicants);
                    }
                });
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

                    // Count actual applications for this project
                    DatabaseReference applicationsRef = FirebaseDatabase.getInstance().getReference("applications");
                    applicationsRef.orderByChild("projectId").equalTo(projectId)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot appSnapshot) {
                                    int applicantsCount = (int) appSnapshot.getChildrenCount();
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
                                            applicantsCount,
                                            positionsCount != null ? positionsCount.intValue() : 0,
                                            projectIcon
                                    );

                                    project.setProjectId(projectId);
                                    projects.add(project);

                                    // Update the adapter after adding each project
                                    callback.onProjectsLoaded(new ArrayList<>(projects));
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    // Handle error
                                }
                            });
                }

                if (!snapshot.exists()) {
                    callback.onProjectsLoaded(projects);
                    loadingIndicator.setVisibility(View.GONE);
                    mainContent.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CompanyHomeActivity.this, "Failed to load projects: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                callback.onProjectsLoaded(projects);
                loadingIndicator.setVisibility(View.GONE);
                mainContent.setVisibility(View.VISIBLE);
            }
        });
    }

    private void fetchProjectsWithApplicants(ProjectsWithApplicantsCallback callback) {
        String companyId = getCurrentCompanyUid();
        DatabaseReference applicationsRef = FirebaseDatabase.getInstance().getReference("applications");
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        DatabaseReference projectsRef = FirebaseDatabase.getInstance().getReference("projects");

        Map<String, ProjectWithApplicants> projectsMap = new HashMap<>();

        applicationsRef.orderByChild("companyId").equalTo(companyId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            callback.onProjectsWithApplicantsLoaded(new ArrayList<>());
                            return;
                        }

                        List<DataSnapshot> applicationsList = new ArrayList<>();
                        for (DataSnapshot appSnapshot : snapshot.getChildren()) {
                            applicationsList.add(appSnapshot);
                        }

                        final int[] processedCount = {0};
                        final int totalApplications = applicationsList.size();

                        for (DataSnapshot appSnapshot : applicationsList) {
                            String userId = appSnapshot.child("userId").getValue(String.class);
                            String projectId = appSnapshot.child("projectId").getValue(String.class);
                            String status = appSnapshot.child("status").getValue(String.class);
                            Long appliedDate = appSnapshot.child("appliedDate").getValue(Long.class);

                            if (userId != null && projectId != null) {
                                // Get user and project details
                                usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                                        String userName = userSnapshot.child("name").getValue(String.class);
                                        String userEmail = userSnapshot.child("email").getValue(String.class);
                                        String userBio = userSnapshot.child("bio").getValue(String.class);
                                        String userUniversity = userSnapshot.child("university").getValue(String.class);
                                        String userDegree = userSnapshot.child("degree").getValue(String.class);

                                        projectsRef.child(projectId).addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot projectSnapshot) {
                                                String projectTitle = projectSnapshot.child("title").getValue(String.class);

                                                if (userName != null && projectTitle != null) {
                                                    // Create or get existing project
                                                    if (!projectsMap.containsKey(projectId)) {
                                                        ProjectWithApplicants project = new ProjectWithApplicants();
                                                        project.setProjectId(projectId);
                                                        project.setProjectTitle(projectTitle);
                                                        project.setApplicants(new ArrayList<>());
                                                        projectsMap.put(projectId, project);
                                                    }

                                                    // Create applicant with more details
                                                    Applicant applicant = new Applicant(
                                                            userName,
                                                            createApplicantPosition(userBio, userUniversity, userDegree),
                                                            status != null ? status : "Pending",
                                                            R.drawable.ic_profile
                                                    );
                                                    applicant.setUserId(userId);
                                                    applicant.setProjectId(projectId);
                                                    applicant.setAppliedDate(appliedDate);

                                                    projectsMap.get(projectId).getApplicants().add(applicant);
                                                }

                                                processedCount[0]++;
                                                if (processedCount[0] == totalApplications) {
                                                    // Convert map to list and sort applicants by date
                                                    List<ProjectWithApplicants> result = new ArrayList<>(projectsMap.values());
                                                    for (ProjectWithApplicants project : result) {
                                                        Collections.sort(project.getApplicants(), (a, b) -> {
                                                            Long dateA = a.getAppliedDate();
                                                            Long dateB = b.getAppliedDate();
                                                            if (dateA == null) dateA = 0L;
                                                            if (dateB == null) dateB = 0L;
                                                            return Long.compare(dateB, dateA); // Most recent first
                                                        });
                                                    }
                                                    callback.onProjectsWithApplicantsLoaded(result);
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {
                                                processedCount[0]++;
                                                if (processedCount[0] == totalApplications) {
                                                    callback.onProjectsWithApplicantsLoaded(new ArrayList<>(projectsMap.values()));
                                                }
                                            }
                                        });
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        processedCount[0]++;
                                        if (processedCount[0] == totalApplications) {
                                            callback.onProjectsWithApplicantsLoaded(new ArrayList<>(projectsMap.values()));
                                        }
                                    }
                                });
                            } else {
                                processedCount[0]++;
                                if (processedCount[0] == totalApplications) {
                                    callback.onProjectsWithApplicantsLoaded(new ArrayList<>(projectsMap.values()));
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(CompanyHomeActivity.this,
                                "Failed to load projects with applicants: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        callback.onProjectsWithApplicantsLoaded(new ArrayList<>());
                    }
                });
    }

    // Helper method to create meaningful position text
    private String createApplicantPosition(String bio, String university, String degree) {
        StringBuilder position = new StringBuilder();

        if (bio != null && !bio.trim().isEmpty()) {
            position.append(bio.trim());
        }

        if (university != null && !university.trim().isEmpty()) {
            if (position.length() > 0) {
                position.append(" at ");
            }
            position.append(university.trim());
        }

        if (degree != null && !degree.trim().isEmpty()) {
            if (position.length() > 0) {
                position.append(" - ");
            }
            position.append(degree.trim());
        }

        // If no details available, return a default
        if (position.length() == 0) {
            return "Student Applicant";
        }

        return position.toString();
    }

    // Interface definitions
    public interface ProjectsCallback {
        void onProjectsLoaded(List<EmployerProject> projects);
    }

    public interface ApplicantsCallback {
        void onApplicantsLoaded(List<Applicant> applicants);
    }

    public interface ProjectsWithApplicantsCallback {
        void onProjectsWithApplicantsLoaded(List<ProjectWithApplicants> projects);
    }

    // Model class for projects with applicants
    public static class ProjectWithApplicants {
        private String projectId;
        private String projectTitle;
        private List<Applicant> applicants;

        public ProjectWithApplicants() {
            this.applicants = new ArrayList<>();
        }

        // Getters and setters
        public String getProjectId() { return projectId; }
        public void setProjectId(String projectId) { this.projectId = projectId; }

        public String getProjectTitle() { return projectTitle; }
        public void setProjectTitle(String projectTitle) { this.projectTitle = projectTitle; }

        public List<Applicant> getApplicants() { return applicants; }
        public void setApplicants(List<Applicant> applicants) { this.applicants = applicants; }
    }

    private void setupClickListeners() {
        View logo = findViewById(R.id.logo);
        if (logo != null) {
            logo.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        }

        if (notificationBell != null) {
            notificationBell.setOnClickListener(v -> {
                Intent intent = new Intent(CompanyHomeActivity.this, CompanyAnnounce.class);
                startActivity(intent);
            });
        }

        View fabCreateProject = findViewById(R.id.fab_create_project);
        if (fabCreateProject != null) {
            fabCreateProject.setOnClickListener(v -> {
                Intent intent = new Intent(CompanyHomeActivity.this, CreateProject.class);
                startActivity(intent);
            });
        }

        // Add click listeners for view all buttons
        View viewAllProjectsButton = findViewById(R.id.view_all_projects_button);
        if (viewAllProjectsButton != null) {
            viewAllProjectsButton.setOnClickListener(v -> showAllProjectsDialog());
        }

        View viewAllApplicantsButton = findViewById(R.id.view_all_applicants_button);
        if (viewAllApplicantsButton != null) {
            viewAllApplicantsButton.setOnClickListener(v -> showAllApplicantsDialog());
        }
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
                ProjectDetailsAdapter adapter = new ProjectDetailsAdapter(projects, project -> {
                    Intent intent = new Intent(CompanyHomeActivity.this, ProjectDetailsActivity.class);
                    intent.putExtra("PROJECT_ID", project.getProjectId());
                    startActivity(intent);
                });
                recyclerView.setAdapter(adapter);
            }
        });

        ImageView btnClose = dialog.findViewById(R.id.btn_close);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private void showAllApplicantsDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_all_applicants_by_project);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
        dialog.setCancelable(true);

        RecyclerView recyclerView = dialog.findViewById(R.id.projects_with_applicants_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Fetch all projects with their applicants
        fetchProjectsWithApplicants(projectsWithApplicants -> {
            ProjectsWithApplicantsAdapter adapter = new ProjectsWithApplicantsAdapter(
                    projectsWithApplicants, this);
            recyclerView.setAdapter(adapter);
        });

        View btnClose = dialog.findViewById(R.id.btn_close_applicants);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

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

        LinearLayout feedLayout = findViewById(R.id.notification_feed_container);
        if (feedLayout != null) {
            feedLayout.removeAllViews();

            for (AnnouncementItem item : topThree) {
                View view = LayoutInflater.from(this).inflate(R.layout.item_notification, feedLayout, false);
                TextView notificationText = view.findViewById(R.id.notification_text);
                TextView notificationTime = view.findViewById(R.id.notification_time);

                if (notificationText != null) {
                    notificationText.setText(item.title + ": " + item.message);
                }

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

                if (notificationTime != null) {
                    notificationTime.setText(timeAgo);
                }
                feedLayout.addView(view);
            }
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

    // Implement OnApplicantActionListener methods
    @Override
    public void onViewProfile(Applicant applicant) {
        // Create intent to view student profile
        Intent intent = new Intent(this, StudentProfileActivity.class);
        intent.putExtra("STUDENT_ID", applicant.getUserId());
        intent.putExtra("STUDENT_NAME", applicant.getName());
        startActivity(intent);
    }

    @Override
    public void onScheduleInterview(Applicant applicant) {
        showScheduleInterviewDialog(applicant);
    }

    @Override
    public void onChat(Applicant applicant) {
        // Navigate to chat activity
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("CHAT_WITH_ID", applicant.getUserId());
        intent.putExtra("CHAT_WITH_NAME", applicant.getName());
        startActivity(intent);
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

            if (itemId == R.id.menu_view_cv) {
                viewApplicantCV(applicant);
                return true;
            } else if (itemId == R.id.menu_shortlist) {
                updateApplicantStatus(applicant, "Shortlisted");
                return true;
            } else if (itemId == R.id.menu_accept) {
                updateApplicantStatus(applicant, "Accepted");
                return true;
            } else if (itemId == R.id.menu_reject) {
                updateApplicantStatus(applicant, "Rejected");
                return true;
            } else {
                return false;
            }
        });

        popup.show();
    }

    private void viewApplicantCV(Applicant applicant) {
        // Get CV URL from user profile
        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users").child(applicant.getUserId());

        userRef.child("cvUrl").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String cvUrl = snapshot.getValue(String.class);
                if (cvUrl != null && !cvUrl.isEmpty()) {
                    // Start viewing CV
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(cvUrl));
                    startActivity(intent);
                    Toast.makeText(CompanyHomeActivity.this,
                            "Opening CV for " + applicant.getName(), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(CompanyHomeActivity.this,
                            "CV not available for " + applicant.getName(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CompanyHomeActivity.this,
                        "Failed to get CV link", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateApplicantStatus(Applicant applicant, String newStatus) {
        // Find the application in Firebase and update its status
        DatabaseReference applicationsRef = FirebaseDatabase.getInstance().getReference("applications");

        applicationsRef.orderByChild("userId").equalTo(applicant.getUserId())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot appSnapshot : snapshot.getChildren()) {
                            String projectId = appSnapshot.child("projectId").getValue(String.class);
                            String companyId = appSnapshot.child("companyId").getValue(String.class);

                            // Check if this is the right application
                            if (projectId != null && projectId.equals(applicant.getProjectId()) &&
                                    companyId != null && companyId.equals(getCurrentCompanyUid())) {

                                // Update the status
                                appSnapshot.getRef().child("status").setValue(newStatus)
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(CompanyHomeActivity.this,
                                                    applicant.getName() + " " + newStatus.toLowerCase(),
                                                    Toast.LENGTH_SHORT).show();

                                            // Refresh the applicants list
                                            refreshApplicantsList();
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(CompanyHomeActivity.this,
                                                    "Failed to update status",
                                                    Toast.LENGTH_SHORT).show();
                                        });
                                break;
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(CompanyHomeActivity.this,
                                "Failed to update applicant status",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showScheduleInterviewDialog(Applicant applicant) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_schedule_interview);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView applicantName = dialog.findViewById(R.id.dialog_applicant_name);
        Button btnSchedule = dialog.findViewById(R.id.btn_schedule);
        Button btnCancel = dialog.findViewById(R.id.btn_cancel);

        if (applicantName != null) {
            applicantName.setText("Schedule interview with " + applicant.getName());
        }

        if (btnSchedule != null) {
            btnSchedule.setOnClickListener(v -> {
                // Implement scheduling logic here
                Toast.makeText(this, "Interview scheduled with " + applicant.getName(),
                        Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private void refreshApplicantsList() {
        // Refresh the applicants in the dialog if it's open
        setupApplicantsRecyclerView();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Intent intent;
        String companyId = getCurrentCompanyUid();

        if (id == R.id.nav_dashboard) {
            // Dashboard clicked - already here
        } else if (id == R.id.nav_profile) {
            intent = new Intent(this, CompanyProfileActivity.class);
            intent.putExtra("companyId", companyId);
            startActivity(intent);
        } else if (id == R.id.nav_projects) {
            intent = new Intent(this, MyProjectsActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_applicant) {
            showAllApplicantsDialog();
        } else if (id == R.id.nav_messages) {
            Toast.makeText(this, "Messages", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_notifications) {
            intent = new Intent(this, CompanyAnnounce.class);
            startActivity(intent);
        } else if (id == R.id.nav_settings) {
            intent = new Intent(this, CompanySettingsActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_help) {
            intent = new Intent(this, CompanyHelpCenterActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_logout) {
            FirebaseAuth.getInstance().signOut();
            Intent logoutIntent = new Intent(this, LoginActivity.class);
            logoutIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(logoutIntent);
            finish();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void fetchCompanyStats() {
        String companyId = getCurrentCompanyUid();
        DatabaseReference applicationsRef = FirebaseDatabase.getInstance().getReference("applications");

        applicationsRef.orderByChild("companyId").equalTo(companyId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int totalApplicants = (int) snapshot.getChildrenCount();
                        int totalHired = 0;

                        // Count accepted applications as hired
                        for (DataSnapshot appSnapshot : snapshot.getChildren()) {
                            String status = appSnapshot.child("status").getValue(String.class);
                            if ("Accepted".equals(status)) {
                                totalHired++;
                            }
                        }

                        // Get total projects separately
                        DatabaseReference projectsRef = FirebaseDatabase.getInstance().getReference("projects");
                        int finalTotalHired = totalHired;
                        projectsRef.orderByChild("companyId").equalTo(companyId)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot projectSnapshot) {
                                        int totalProjects = (int) projectSnapshot.getChildrenCount();
                                        updateCompanyStats(totalProjects, totalApplicants, finalTotalHired);
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        updateCompanyStats(0, totalApplicants, finalTotalHired);
                                    }
                                });
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

        if (tvTotalProjects != null) {
            tvTotalProjects.setText(String.valueOf(totalProjects));
        }
        if (tvTotalApplicants != null) {
            tvTotalApplicants.setText(String.valueOf(totalApplicants));
        }
        if (tvTotalHired != null) {
            tvTotalHired.setText(String.valueOf(totalHired));
        }
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