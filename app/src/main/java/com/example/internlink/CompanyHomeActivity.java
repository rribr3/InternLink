package com.example.internlink;


import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.util.Util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

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
    private Intent intent;

    // ✅ NEW: SwipeRefreshLayout for pull-to-refresh
    private SwipeRefreshLayout swipeRefreshLayout;

    // ✅ NEW: References to adapters for refreshing
    private CompanyProjectsAdapter projectsAdapter;
    private ApplicantsAdapter applicantsAdapter;
    private RecyclerView projectsRecyclerView;
    private RecyclerView applicantsRecyclerView;

    // ✅ NEW: Track refresh state
    private boolean isRefreshing = false;
    private int refreshTasksCount = 0;
    private int completedRefreshTasks = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("CompanyHomeActivity", "onCreate started");
        setContentView(R.layout.activity_company_home);

        initializeViews();
        setupSwipeRefresh(); // ✅ NEW: Setup pull-to-refresh

        loadingIndicator.setVisibility(View.VISIBLE);
        mainContent.setVisibility(View.GONE);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            checkUserStatus(currentUser);
        }

        setupClickListeners();
        setupNavigationDrawer();
        loadAllData(); // ✅ NEW: Centralized data loading
        createNotificationChannel();
    }

    // ✅ NEW: Initialize SwipeRefreshLayout
    private void setupSwipeRefresh() {
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::refreshAllData);
            swipeRefreshLayout.setColorSchemeResources(
                    R.color.blueDark,
                    R.color.blueLight,
                    R.color.blue
            );
        }
    }

    // ✅ NEW: Public method to refresh all data
    private void loadAllData() {
        refreshTasksCount = 8; // Increased to include status update
        completedRefreshTasks = 0;

        loadCompanyInfo();
        setupDashboardContent();
        fetchRecentCompanyAnnouncements();
        setupBottomNavigation();
        setupNotificationBell();
        fetchCompanyStats();
        updateProjectStatuses(); // ✅ Add this line
        onRefreshTaskCompleted();
    }


    public void refreshAllData() {
        if (isRefreshing) {
            return;
        }

        isRefreshing = true;
        refreshTasksCount = 8; // Increased to include status update
        completedRefreshTasks = 0;

        Log.d("CompanyHomeActivity", "Starting data refresh...");

        if (swipeRefreshLayout != null && !swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(true);
        }

        clearExistingData();

        loadCompanyInfo();
        refreshDashboardContent();
        refreshCompanyAnnouncements();
        refreshNotificationBell();
        refreshCompanyStats();
        refreshNavigationDrawer();
        refreshMessagesBadge();
        updateProjectStatuses(); // ✅ Add this line
    }
    public void checkProjectStatusesManually() {
        updateProjectStatuses();
        Toast.makeText(this, "Checking project statuses...", Toast.LENGTH_SHORT).show();
    }
    // ✅ NEW: Refresh messages badge
    private void refreshMessagesBadge() {
        setupUnreadMessagesBadge();
        onRefreshTaskCompleted();
    }

    // ✅ NEW: Clear existing data before refresh
    private void clearExistingData() {
        // Clear projects adapter
        if (projectsAdapter != null) {
            projectsAdapter.clearData();
        }

        // Clear applicants adapter
        if (applicantsAdapter != null) {
            applicantsAdapter.clearData();
        }

        // Clear notification feed
        LinearLayout feedLayout = findViewById(R.id.notification_feed_container);
        if (feedLayout != null) {
            feedLayout.removeAllViews();
        }

        // Clear dots indicator
        if (dotIndicatorLayout != null) {
            dotIndicatorLayout.removeAllViews();
        }
    }

    // ✅ NEW: Track completion of refresh tasks
    private void onRefreshTaskCompleted() {
        completedRefreshTasks++;
        Log.d("CompanyHomeActivity", "Refresh task completed: " + completedRefreshTasks + "/" + refreshTasksCount);

        if (completedRefreshTasks >= refreshTasksCount) {
            finishRefresh();
        }
    }

    // ✅ NEW: Complete the refresh process
    private void finishRefresh() {
        isRefreshing = false;

        // Hide refresh indicator
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }

        // Hide loading indicator and show content
        if (loadingIndicator != null) {
            loadingIndicator.setVisibility(View.GONE);
        }
        if (mainContent != null) {
            mainContent.setVisibility(View.VISIBLE);
        }

        Log.d("CompanyHomeActivity", "Data refresh completed");
    }

    // ✅ UPDATED: Load company info with refresh tracking
    private void loadCompanyInfo() {
        String companyUid = getCurrentCompanyUid();
        databaseReference = FirebaseDatabase.getInstance().getReference("users");

        databaseReference.child(companyUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String name = "Welcome, " + dataSnapshot.child("name").getValue(String.class);
                    if (welcomeText != null) {
                        welcomeText.setText(name);
                    }
                }
                onRefreshTaskCompleted();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("CompanyHomeActivity", "Failed to load company info", databaseError.toException());
                onRefreshTaskCompleted();
            }
        });
    }

    // ✅ UPDATED: Refresh dashboard content
    private void refreshDashboardContent() {
        refreshProjectsRecyclerView();
        refreshApplicantsRecyclerView();
        refreshProjectStats();
    }

    // ✅ NEW: Refresh projects with tracking
    private void refreshProjectsRecyclerView() {
        getSampleProjectsFromFirebase(new ProjectsCallback() {
            @Override
            public void onProjectsLoaded(List<EmployerProject> projects) {
                if (projectsAdapter == null) {
                    projectsAdapter = new CompanyProjectsAdapter(projects);
                    if (projectsRecyclerView != null) {
                        projectsRecyclerView.setAdapter(projectsAdapter);
                    }
                } else {
                    projectsAdapter.updateProjects(projects);
                }

                // Update dots
                addDots(projects.size());
                onRefreshTaskCompleted();
            }
        });
    }

    // ✅ NEW: Refresh applicants with tracking
    private void refreshApplicantsRecyclerView() {
        fetchRecentApplicantsFromFirebase(applicants -> {
            if (applicantsAdapter == null) {
                applicantsAdapter = new ApplicantsAdapter(applicants);
                if (applicantsRecyclerView != null) {
                    applicantsRecyclerView.setAdapter(applicantsAdapter);
                }
            } else {
                applicantsAdapter.updateApplicants(applicants);
            }
            onRefreshTaskCompleted();
        });
    }

    // ✅ NEW: Refresh project stats
    private void refreshProjectStats() {
        fetchProjectStats();
        // Note: fetchProjectStats calls onRefreshTaskCompleted internally
    }

    // ✅ NEW: Refresh company announcements
    private void refreshCompanyAnnouncements() {
        fetchRecentCompanyAnnouncements();
        // Note: fetchRecentCompanyAnnouncements calls onRefreshTaskCompleted internally
    }

    // ✅ NEW: Refresh notification bell
    private void refreshNotificationBell() {
        setupNotificationBell();
        // Note: setupNotificationBell calls onRefreshTaskCompleted internally
    }

    // ✅ NEW: Refresh company stats
    private void refreshCompanyStats() {
        fetchCompanyStats();
        // Note: fetchCompanyStats calls onRefreshTaskCompleted internally
    }

    // ✅ NEW: Refresh navigation drawer
    private void refreshNavigationDrawer() {
        setupNavigationDrawer();
        // Note: setupNavigationDrawer calls onRefreshTaskCompleted internally
    }

    // ✅ NEW: Manual refresh button (you can add this to toolbar)
    public void onRefreshButtonClicked(View view) {
        refreshAllData();
    }

    private void checkUserStatus(FirebaseUser currentUser) {
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
                        snapshot.getRef().removeValue();
                        currentUser.delete();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("CompanyHomeActivity", "Failed to check user status", error.toException());
            }
        });
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

    // ✅ UPDATED: Setup notification bell with refresh tracking
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
                            String key = snap.getKey();
                            if (key != null && !readsSnapshot.hasChild(key)) {
                                unreadIds.add(key);
                            }
                        }

                        roleRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot roleSnapshot) {
                                for (DataSnapshot snap : roleSnapshot.getChildren()) {
                                    String key = snap.getKey();
                                    if (key != null && !readsSnapshot.hasChild(key)) {
                                        unreadIds.add(key);
                                    }
                                }

                                updateNotificationBadge(unreadIds.size());
                                if (isRefreshing) {
                                    onRefreshTaskCompleted();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e("NotificationBell", "Failed to read role announcements", error.toException());
                                updateNotificationBadge(unreadIds.size());
                                if (isRefreshing) {
                                    onRefreshTaskCompleted();
                                }
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("NotificationBell", "Failed to read global announcements", error.toException());
                        updateNotificationBadge(0);
                        if (isRefreshing) {
                            onRefreshTaskCompleted();
                        }
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("NotificationBell", "Failed to read user reads", error.toException());
                updateNotificationBadge(0);
                if (isRefreshing) {
                    onRefreshTaskCompleted();
                }
            }
        };

        userReadsRef.addValueEventListener(unreadListener);

        notificationBell.setOnClickListener(v -> {
            Intent intent = new Intent(CompanyHomeActivity.this, CompanyAnnounce.class);
            startActivity(intent);
        });
    }

    private void updateNotificationBadge(int unreadCount) {
        if (notificationBadge != null) {
            if (unreadCount > 0) {
                notificationBadge.setVisibility(View.VISIBLE);
                // Remove text and just show as a red dot
                notificationBadge.setText("");
            } else {
                notificationBadge.setVisibility(View.GONE);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        // Automatically refresh data when returning to activity
        if (!isRefreshing) {
            refreshAllData();
        }
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.navigation_home);
        }
        setupUnreadMessagesBadge();
    }

    private void initializeViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        welcomeText = findViewById(R.id.welcome_text);
        notificationBell = findViewById(R.id.notification_bell);
        notificationBadge = findViewById(R.id.notification_badge);
        loadingIndicator = findViewById(R.id.home_loading_indicator);
        mainContent = findViewById(R.id.home_main_content);
        dotIndicatorLayout = findViewById(R.id.dotIndicatorLayout);
        companyLogo = findViewById(R.id.companyLogo);

        // ✅ NEW: Initialize RecyclerViews references
        projectsRecyclerView = findViewById(R.id.projects_recycler_view);
        applicantsRecyclerView = findViewById(R.id.applicants_recycler_view);
    }

    // ✅ UPDATED: Setup navigation drawer with refresh tracking
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
                if (isRefreshing) {
                    onRefreshTaskCompleted();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CompanyHomeActivity.this, "Failed to load company info", Toast.LENGTH_SHORT).show();
                if (isRefreshing) {
                    onRefreshTaskCompleted();
                }
            }
        });
    }

    private void setupDashboardContent() {
        setupProjectsRecyclerView();
        setupApplicantsRecyclerView();
        updateNotificationBadge(5);
        fetchProjectStats();
    }

    private void updateProjectStatuses() {
        String companyId = getCurrentCompanyUid();
        DatabaseReference projectsRef = FirebaseDatabase.getInstance().getReference("projects");
        DatabaseReference applicationsRef = FirebaseDatabase.getInstance().getReference("applications");

        // Get all projects for this company
        projectsRef.orderByChild("companyId").equalTo(companyId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot projectSnapshot : snapshot.getChildren()) {
                            String projectId = projectSnapshot.getKey();
                            String currentStatus = projectSnapshot.child("status").getValue(String.class);
                            Long deadline = projectSnapshot.child("deadline").getValue(Long.class);
                            Long studentsRequired = projectSnapshot.child("studentsRequired").getValue(Long.class);

                            // Only update if project is currently "approved" (active)
                            if ("approved".equals(currentStatus) && projectId != null) {
                                checkAndUpdateProjectStatus(projectId, deadline, studentsRequired, applicationsRef);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("CompanyHomeActivity", "Failed to load projects for status update", error.toException());
                    }
                });
    }

    // ✅ NEW: Check individual project status and update if needed
    private void checkAndUpdateProjectStatus(String projectId, Long deadline, Long studentsRequired,
                                             DatabaseReference applicationsRef) {
        long currentTime = System.currentTimeMillis();
        boolean shouldComplete = false;
        String completionReason = "";

        // Check if deadline has passed
        if (deadline != null && currentTime > deadline) {
            shouldComplete = true;
            completionReason = "Deadline reached";
        }

        // Check if enough students have been accepted
        if (studentsRequired != null && studentsRequired > 0) {
            applicationsRef.orderByChild("projectId").equalTo(projectId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            int acceptedCount = 0;

                            // Count accepted applications for this project
                            for (DataSnapshot appSnapshot : snapshot.getChildren()) {
                                String status = appSnapshot.child("status").getValue(String.class);
                                if ("Accepted".equals(status)) {
                                    acceptedCount++;
                                }
                            }

                            // Check if we have enough accepted students
                            boolean shouldCompleteByAcceptance = acceptedCount >= studentsRequired;
                            boolean shouldCompleteByDeadline = deadline != null && currentTime > deadline;

                            // Update project status if either condition is met
                            if (shouldCompleteByDeadline || shouldCompleteByAcceptance) {
                                String reason = shouldCompleteByDeadline ? "Deadline reached" :
                                        shouldCompleteByAcceptance ? "Positions filled" : "Auto-completed";

                                updateProjectToCompleted(projectId, reason, acceptedCount, studentsRequired.intValue());
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("CompanyHomeActivity", "Failed to check applications for project: " + projectId, error.toException());
                        }
                    });
        } else {
            // If no students required info, just check deadline
            if (shouldComplete) {
                updateProjectToCompleted(projectId, completionReason, 0, 0);
            }
        }
    }

    // ✅ NEW: Update project status to completed
    private void updateProjectToCompleted(String projectId, String reason, int acceptedCount, int requiredCount) {
        DatabaseReference projectRef = FirebaseDatabase.getInstance().getReference("projects").child(projectId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "completed");
        updates.put("completedAt", System.currentTimeMillis());
        updates.put("completionReason", reason);
        updates.put("finalAcceptedCount", acceptedCount);

        projectRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d("CompanyHomeActivity", "Project " + projectId + " marked as completed. Reason: " + reason);

                    // Optionally show a toast notification
                    if (reason.equals("Positions filled")) {
                        Toast.makeText(this, "Project completed - All positions filled (" + acceptedCount + "/" + requiredCount + ")",
                                Toast.LENGTH_SHORT).show();
                    } else if (reason.equals("Deadline reached")) {
                        Toast.makeText(this, "Project completed - Deadline reached", Toast.LENGTH_SHORT).show();
                    }

                    // Refresh the data to show updated status
                    refreshAllData();
                })
                .addOnFailureListener(e -> {
                    Log.e("CompanyHomeActivity", "Failed to update project status", e);
                });
    }



    // ✅ UPDATED: Fetch project stats with refresh tracking
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
                            case "completed":  // ✅ Now properly counts completed projects
                                completedCount++;
                                break;
                        }
                    }
                }

                updateProjectStats(activeCount, pendingCount, completedCount);
                if (isRefreshing) {
                    onRefreshTaskCompleted();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CompanyHomeActivity.this, "Failed to load project stats: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                if (isRefreshing) {
                    onRefreshTaskCompleted();
                }
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
        if (projectsRecyclerView != null) {
            projectsRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

            getSampleProjectsFromFirebase(new ProjectsCallback() {
                @Override
                public void onProjectsLoaded(List<EmployerProject> projects) {
                    projectsAdapter = new CompanyProjectsAdapter(projects);
                    projectsRecyclerView.setAdapter(projectsAdapter);

                    addDots(projects.size());

                    projectsRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
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

                    loadingIndicator.setVisibility(View.GONE);
                    mainContent.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    private void addDots(int count) {
        if (dotIndicatorLayout == null || count <= 0) return;

        dotIndicatorLayout.removeAllViews();

        // ✅ NEW: Limit maximum dots to 3
        int maxDots = Math.min(count, 3);

        int sizeInPx = (int) (10 * getResources().getDisplayMetrics().density);
        int marginInPx = (int) (4 * getResources().getDisplayMetrics().density);

        for (int i = 0; i < maxDots; i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(sizeInPx, sizeInPx);
            params.setMargins(marginInPx, 0, marginInPx, 0);
            dot.setLayoutParams(params);
            dot.setBackgroundResource(R.drawable.circle_dot);
            dotIndicatorLayout.addView(dot);
        }

        if (dotIndicatorLayout.getChildCount() > 0) {
            dotIndicatorLayout.getChildAt(0).setBackgroundResource(R.drawable.circle_dot_active);
        }
    }


    private void updateActiveDot(int position) {
        if (dotIndicatorLayout == null || dotIndicatorLayout.getChildCount() == 0) {
            return;
        }

        // ✅ NEW: Map position to dot index when there are more projects than dots
        int dotIndex = Math.min(position, dotIndicatorLayout.getChildCount() - 1);

        // Reset all dots to inactive
        for (int i = 0; i < dotIndicatorLayout.getChildCount(); i++) {
            View dot = dotIndicatorLayout.getChildAt(i);
            dot.setBackgroundResource(R.drawable.circle_dot);
        }

        // Set the appropriate dot as active
        if (dotIndex >= 0 && dotIndex < dotIndicatorLayout.getChildCount()) {
            View activeDot = dotIndicatorLayout.getChildAt(dotIndex);
            activeDot.setBackgroundResource(R.drawable.circle_dot_active);
        }
    }

    private void setupApplicantsRecyclerView() {
        if (applicantsRecyclerView != null) {
            applicantsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

            fetchRecentApplicantsFromFirebase(applicants -> {
                applicantsAdapter = new ApplicantsAdapter(applicants);
                applicantsRecyclerView.setAdapter(applicantsAdapter);
            });
        }
    }

    // Add these methods to your CompanyHomeActivity class

    private void setupUnreadMessagesBadge() {
        String companyId = getCurrentCompanyUid();
        DatabaseReference notificationsRef = FirebaseDatabase.getInstance()
                .getReference("notifications")
                .child(companyId);

        notificationsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int unreadCount = 0;

                // Count unread chat messages
                for (DataSnapshot notificationSnap : snapshot.getChildren()) {
                    String type = notificationSnap.child("type").getValue(String.class);
                    Boolean isRead = notificationSnap.child("read").getValue(Boolean.class);

                    if ("chat_message".equals(type) && (isRead == null || !isRead)) {
                        unreadCount++;
                    }
                }

                // Update the badge
                updateMessagesBadge(unreadCount);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("CompanyHomeActivity", "Failed to load message notifications", error.toException());
                // Handle error - hide badge or show 0
                updateMessagesBadge(0);
            }
        });
    }

    private void updateMessagesBadge(int count) {
        runOnUiThread(() -> {
            // Get the BottomNavigationView
            BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

            if (bottomNav != null) {
                // Remove any existing badge first
                bottomNav.removeBadge(R.id.navigation_message);

                if (count > 0) {
                    // Create and show badge
                    com.google.android.material.badge.BadgeDrawable badge =
                            bottomNav.getOrCreateBadge(R.id.navigation_message);

                    badge.setNumber(count);
                    badge.setBackgroundColor(ContextCompat.getColor(this, R.color.red));
                    badge.setBadgeTextColor(ContextCompat.getColor(this, android.R.color.white));
                    badge.setMaxCharacterCount(2); // Show "99+" for numbers > 99
                    badge.setVisible(true);
                }
            }
        });
    }

    // Alternative method using custom badge view if Material badges don't work
    private void updateMessagesBadgeCustom(int count) {
        // Find the messages menu item view
        View messagesView = findViewById(R.id.navigation_message);

        if (messagesView != null && messagesView instanceof ViewGroup) {
            ViewGroup messagesContainer = (ViewGroup) messagesView;

            // Remove existing badge
            View existingBadge = messagesContainer.findViewWithTag("messages_badge");
            if (existingBadge != null) {
                messagesContainer.removeView(existingBadge);
            }

            if (count > 0) {
                // Create custom badge
                TextView badge = new TextView(this);
                badge.setTag("messages_badge");
                badge.setText(count > 99 ? "99+" : String.valueOf(count));
                badge.setTextSize(10f);
                badge.setTextColor(Color.WHITE);
                badge.setTypeface(null, Typeface.BOLD);
                badge.setGravity(Gravity.CENTER);

                // Style the badge
                int size = (int) (20 * getResources().getDisplayMetrics().density);
                badge.setWidth(size);
                badge.setHeight(size);
                badge.setBackgroundResource(R.drawable.circle_badge_background);

                // Position the badge
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                        size, size, Gravity.TOP | Gravity.END);
                params.setMargins(0, 5, 5, 0);
                badge.setLayoutParams(params);

                messagesContainer.addView(badge);
            }
        }
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
        String companyId = getCurrentCompanyUid();
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.navigation_home);

            bottomNavigation.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();

                if (itemId == R.id.navigation_home) {
                    return true;
                } else if (itemId == R.id.navigation_message) {
                    // Clear the badge when opening messages
                    bottomNavigation.removeBadge(R.id.navigation_message);

                    Intent intent = new Intent(this, CompanyMessagesActivity.class);
                    startActivity(intent);
                    return true;
                }
                else if (itemId == R.id.navigation_Schedule) {
                    Intent intent = new Intent(CompanyHomeActivity.this, ScheduleActivity.class);
                    startActivity(intent);
                    return true;
                } else if (itemId == R.id.navigation_profile) {
                    Intent intent = new Intent(CompanyHomeActivity.this, CompanyProfileActivity.class);
                    intent.putExtra("companyId", companyId);
                    startActivity(intent);
                    return true;
                }

                return false;
            });

            // ✅ NEW: Setup unread messages badge after bottom navigation is ready
            setupUnreadMessagesBadge();

        } else {
            Log.e("CompanyHomeActivity", "Bottom navigation view not found!");
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

                        for (DataSnapshot appSnapshot : snapshot.getChildren()) {
                            applicationsList.add(appSnapshot);
                        }

                        Collections.sort(applicationsList, (a, b) -> {
                            Long dateA = a.child("appliedDate").getValue(Long.class);
                            Long dateB = b.child("appliedDate").getValue(Long.class);
                            if (dateA == null) dateA = 0L;
                            if (dateB == null) dateB = 0L;
                            return Long.compare(dateB, dateA);
                        });

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
                                usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                                        String userName = userSnapshot.child("name").getValue(String.class);

                                        projectsRef.child(projectId).addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot projectSnapshot) {
                                                String projectTitle = projectSnapshot.child("title").getValue(String.class);

                                                if (userName != null && projectTitle != null) {
                                                    Applicant applicant = new Applicant(
                                                            userName,
                                                            projectTitle,
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

                // First, update project statuses automatically
                updateProjectStatuses();

                for (DataSnapshot projectSnapshot : snapshot.getChildren()) {
                    String projectId = projectSnapshot.getKey();
                    String title = projectSnapshot.child("title").getValue(String.class);
                    Long positionsCount = projectSnapshot.child("studentsRequired").getValue(Long.class);

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
                                        projectIcon = R.drawable.ic_approve; // ✅ Use check/approve icon for completed
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
                                    project.setStatus(status); // ✅ Make sure to set the status
                                    projects.add(project);

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
                                                    if (!projectsMap.containsKey(projectId)) {
                                                        ProjectWithApplicants project = new ProjectWithApplicants();
                                                        project.setProjectId(projectId);
                                                        project.setProjectTitle(projectTitle);
                                                        project.setApplicants(new ArrayList<>());
                                                        projectsMap.put(projectId, project);
                                                    }

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
                                                    List<ProjectWithApplicants> result = new ArrayList<>(projectsMap.values());
                                                    for (ProjectWithApplicants project : result) {
                                                        Collections.sort(project.getApplicants(), (a, b) -> {
                                                            Long dateA = a.getAppliedDate();
                                                            Long dateB = b.getAppliedDate();
                                                            if (dateA == null) dateA = 0L;
                                                            if (dateB == null) dateB = 0L;
                                                            return Long.compare(dateB, dateA);
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

        if (position.length() == 0) {
            return "Student Applicant";
        }

        return position.toString();
    }

    public interface ProjectsCallback {
        void onProjectsLoaded(List<EmployerProject> projects);
    }

    public interface ApplicantsCallback {
        void onApplicantsLoaded(List<Applicant> applicants);
    }

    public interface ProjectsWithApplicantsCallback {
        void onProjectsWithApplicantsLoaded(List<ProjectWithApplicants> projects);
    }

    public static class ProjectWithApplicants {
        private String projectId;
        private String projectTitle;
        private List<Applicant> applicants;

        public ProjectWithApplicants() {
            this.applicants = new ArrayList<>();
        }

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

        View viewAllProjectsButton = findViewById(R.id.view_all_projects_button);
        if (viewAllProjectsButton != null) {
            viewAllProjectsButton.setOnClickListener(v -> showAllProjectsDialog());
        }

        View viewAllApplicantsButton = findViewById(R.id.view_all_applicants_button);
        if (viewAllApplicantsButton != null) {
            viewAllApplicantsButton.setOnClickListener(v -> {
                Intent intent = new Intent(CompanyHomeActivity.this, MyApplicants.class);
                startActivity(intent);
            });
        }

        // ✅ NEW: Add refresh button click listener
        TextView viewNotification = findViewById(R.id.view_notification);
        if (viewNotification != null) {
            viewNotification.setOnClickListener(v -> {
                Intent intent = new Intent(CompanyHomeActivity.this, CompanyAnnounce.class);
                startActivity(intent);
            });
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

    // ✅ UPDATED: Fetch recent company announcements with refresh tracking
    private void fetchRecentCompanyAnnouncements() {
        DatabaseReference globalRef = FirebaseDatabase.getInstance().getReference("announcements");
        DatabaseReference roleRef = FirebaseDatabase.getInstance().getReference("announcements_by_role").child("company");

        List<Announcement> announcements = new ArrayList<>();

        ValueEventListenerCollector collector = new ValueEventListenerCollector(2, announcements, () -> {
            // Sort announcements by timestamp (most recent first)
            Collections.sort(announcements, (a1, a2) -> Long.compare(a2.getTimestamp(), a1.getTimestamp()));

            // Take only the most recent announcements (e.g., top 5)
            int limit = Math.min(announcements.size(), 5);
            List<Announcement> recentAnnouncements = announcements.subList(0, limit);

            // Process and display the announcements
            displayRecentAnnouncements(recentAnnouncements);

            if (isRefreshing) {
                onRefreshTaskCompleted();
            }
        });

        globalRef.addListenerForSingleValueEvent(collector.getListener());
        roleRef.addListenerForSingleValueEvent(collector.getListener());
    }
    private void displayRecentAnnouncements(List<Announcement> announcements) {
        LinearLayout feedContainer = findViewById(R.id.notification_feed_container);
        if (feedContainer == null) return;

        feedContainer.removeAllViews();

        for (Announcement announcement : announcements) {
            View announcementView = LayoutInflater.from(this).inflate(R.layout.item_notification, feedContainer, false);

            TextView titleView = announcementView.findViewById(R.id.notification_text);
            TextView dateView = announcementView.findViewById(R.id.notification_time);

            if (titleView != null) titleView.setText(announcement.getTitle());
            if (dateView != null) dateView.setText(announcement.getDate());

            feedContainer.addView(announcementView);
        }
    }

    private static class ValueEventListenerCollector {
        private final int expected;
        private int count = 0;
        private final List<Announcement> announcements;
        private final Runnable onAllCollected;

        public ValueEventListenerCollector(int expectedCalls, List<Announcement> announcements, Runnable callback) {
            this.expected = expectedCalls;
            this.announcements = announcements;
            this.onAllCollected = callback;
        }

        public ValueEventListener getListener() {
            return new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot announcementSnap : snapshot.getChildren()) {
                        try {
                            String id = announcementSnap.getKey();
                            String title = announcementSnap.child("title").getValue(String.class);
                            String message = announcementSnap.child("message").getValue(String.class);
                            Long timestamp = announcementSnap.child("timestamp").getValue(Long.class);

                            Announcement announcement = new Announcement();
                            announcement.setId(id);
                            announcement.setTitle(title);
                            announcement.setBody(message);
                            announcement.setTimestamp(timestamp != null ? timestamp : 0);
                            announcement.setDate(formatTimestamp(timestamp != null ? timestamp : 0));

                            announcements.add(announcement);
                        } catch (Exception e) {
                            Log.e("CompanyHomeActivity", "Error processing announcement", e);
                        }
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

    private void processAndDisplayRecentAnnouncements(DataSnapshot snapshot) {
        try {
            for (DataSnapshot announcementSnap : snapshot.getChildren()) {
                try {
                    String id = announcementSnap.getKey();
                    String title = announcementSnap.child("title").getValue(String.class);
                    String message = announcementSnap.child("message").getValue(String.class);

                    // Safe timestamp conversion
                    long timestamp = getTimestampFromSnapshot(announcementSnap.child("timestamp"));

                    // Create your announcement object or process the data as needed
                    // Example:
                    Announcement announcement = new Announcement();
                    announcement.setId(id);
                    announcement.setTitle(title);
                    announcement.setBody(message);
                    announcement.setTimestamp(timestamp);
                    announcement.setDate(formatTimestamp(timestamp));

                    // Add to your list or process further
                    // announcements.add(announcement);

                } catch (Exception e) {
                    Log.e("CompanyHome", "Error processing individual announcement: " + e.getMessage());
                    continue; // Skip this announcement but continue processing others
                }
            }
        } catch (Exception e) {
            Log.e("CompanyHome", "Error in processAndDisplayRecentAnnouncements: " + e.getMessage());
        }
    }
    private long getTimestampFromSnapshot(DataSnapshot timestampSnap) {
        if (!timestampSnap.exists()) return 0;

        try {
            Object value = timestampSnap.getValue();
            if (value == null) return 0;

            if (value instanceof Long) {
                return (Long) value;
            } else if (value instanceof Double) {
                return ((Double) value).longValue();
            } else if (value instanceof String) {
                String timestampStr = (String) value;
                if (timestampStr.contains("T")) {
                    // ISO 8601 format
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                    Date date = sdf.parse(timestampStr);
                    return date != null ? date.getTime() : 0;
                } else {
                    // Try parsing as numeric string
                    return Long.parseLong(timestampStr);
                }
            }
        } catch (Exception e) {
            Log.e("CompanyHome", "Error converting timestamp: " + timestampSnap.getValue(), e);
        }
        return 0;
    }
        private String formatTimestamp(long timestamp) {
            if (timestamp <= 0) return "Unknown date";
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.US);
                return sdf.format(new Date(timestamp));
            } catch (Exception e) {
                Log.e("CompanyHomeActivity", "Error formatting timestamp: " + timestamp, e);
                return "Unknown date";
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

    @Override
    public void onViewProfile(Applicant applicant) {
        Intent intent = new Intent(this, ApplicantProfileActivity.class);
        intent.putExtra("APPLICANT_ID", applicant.getUserId());
        startActivity(intent);
    }

    @Override
    public void onScheduleInterview(Applicant applicant) {
        showScheduleInterviewDialog(applicant);
    }

    @Override
    public void onChat(Applicant applicant) {
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
        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users").child(applicant.getUserId());

        userRef.child("cvUrl").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String cvUrl = snapshot.getValue(String.class);
                if (cvUrl != null && !cvUrl.isEmpty()) {
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
        DatabaseReference applicationsRef = FirebaseDatabase.getInstance().getReference("applications");

        applicationsRef.orderByChild("userId").equalTo(applicant.getUserId())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot appSnapshot : snapshot.getChildren()) {
                            String projectId = appSnapshot.child("projectId").getValue(String.class);
                            String companyId = appSnapshot.child("companyId").getValue(String.class);

                            if (projectId != null && projectId.equals(applicant.getProjectId()) &&
                                    companyId != null && companyId.equals(getCurrentCompanyUid())) {

                                appSnapshot.getRef().child("status").setValue(newStatus)
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(CompanyHomeActivity.this,
                                                    applicant.getName() + " " + newStatus.toLowerCase(),
                                                    Toast.LENGTH_SHORT).show();

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

    // ✅ UPDATED: Refresh applicants list with new data
    private void refreshApplicantsList() {
        if (applicantsAdapter != null) {
            fetchRecentApplicantsFromFirebase(applicants -> {
                applicantsAdapter.updateApplicants(applicants);
            });
        } else {
            setupApplicantsRecyclerView();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Intent intent;
        String companyId = getCurrentCompanyUid();

        if (id == R.id.nav_profile) {
            intent = new Intent(this, CompanyProfileActivity.class);
            intent.putExtra("companyId", companyId);
            startActivity(intent);
        } else if (id == R.id.nav_projects) {
            intent = new Intent(this, MyProjectsActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_applicant) {
            intent = new Intent(this, MyApplicants.class);
            startActivity(intent);
        } else if (id == R.id.nav_messages) {
            Toast.makeText(this, "Messages", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_notifications) {
            intent = new Intent(this, CompanyAnnounce.class);
            startActivity(intent);
        } else if (id == R.id.nav_feedback) {
            intent = new Intent(this, CompanyFeedbackActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_settings) {
            intent = new Intent(this, CompanySettingsActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_help) {
            intent = new Intent(this, CompanyHelpCenterActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_logout) {
            finish();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    // ✅ UPDATED: Fetch company stats with refresh tracking
    private void fetchCompanyStats() {
        String companyId = getCurrentCompanyUid();
        DatabaseReference applicationsRef = FirebaseDatabase.getInstance().getReference("applications");

        applicationsRef.orderByChild("companyId").equalTo(companyId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int totalApplicants = (int) snapshot.getChildrenCount();
                        int totalHired = 0;

                        for (DataSnapshot appSnapshot : snapshot.getChildren()) {
                            String status = appSnapshot.child("status").getValue(String.class);
                            if ("Accepted".equals(status)) {
                                totalHired++;
                            }
                        }

                        DatabaseReference projectsRef = FirebaseDatabase.getInstance().getReference("projects");
                        int finalTotalHired = totalHired;
                        projectsRef.orderByChild("companyId").equalTo(companyId)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot projectSnapshot) {
                                        int totalProjects = (int) projectSnapshot.getChildrenCount();
                                        updateCompanyStats(totalProjects, totalApplicants, finalTotalHired);
                                        if (isRefreshing) {
                                            onRefreshTaskCompleted();
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        updateCompanyStats(0, totalApplicants, finalTotalHired);
                                        if (isRefreshing) {
                                            onRefreshTaskCompleted();
                                        }
                                    }
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(CompanyHomeActivity.this, "Failed to load company stats", Toast.LENGTH_SHORT).show();
                        if (isRefreshing) {
                            onRefreshTaskCompleted();
                        }
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