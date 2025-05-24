package com.example.internlink;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.search.SearchBar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import android.graphics.Paint;
import com.google.android.material.search.SearchView;
import android.text.Editable;
import android.text.TextWatcher;

import java.util.ArrayList;
import java.util.List;

public class StudentHomeActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private TextView welcomeText, navMenuName, navMenuMail;
    private ImageView notificationBell;
    private TextView notificationBadge;
    private RecyclerView projectsRecyclerView;
    private ProjectAdapterHome projectAdapterHome;
    private List<Project> allProjects;
    private ProgressBar loadingIndicator;
    private View mainContent, headerView;
    private LinearLayout dotIndicatorLayout;
    private Intent intent;
    private String studentId;
    private SwipeRefreshLayout swipeRefreshLayout;
    private NavigationView navigationView;
    private BottomNavigationView bottomNavigationView;
    private boolean isBottomNavVisible = true;
    private SearchView searchView;
    private SearchBar searchBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_home);
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        studentId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        loadingIndicator = findViewById(R.id.home_loading_indicator);
        mainContent = findViewById(R.id.home_main_content);
        dotIndicatorLayout = findViewById(R.id.dotIndicatorLayout);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        navigationView = findViewById(R.id.nav_view);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        headerView = navigationView.getHeaderView(0);
        navMenuName = headerView.findViewById(R.id.menu_name);
        navMenuMail = headerView.findViewById(R.id.menu_mail);
        searchBar = findViewById(R.id.search_bar);
        searchView = findViewById(R.id.search_view);

        loadingIndicator.setVisibility(View.VISIBLE);
        mainContent.setVisibility(View.GONE);

        // Initialize
        allProjects = getSampleProjects();

        initializeViews();
        setupNavigationDrawer();
        setupBottomNavigation();
        setWelcomeMessage();
        setupNotificationBell();
        setupProjectsRecyclerView();
        setupClickListeners();
        setupSwipeRefresh();


        if (searchView != null && searchBar != null) {
            // Connect SearchBar with SearchView properly
            searchView.setupWithSearchBar(searchBar);

            // Set up the SearchBar click listener
            searchBar.setOnClickListener(v -> {
                searchView.show();
            });

            // Configure the SearchView's EditText
            EditText editText = searchView.getEditText();
            if (editText != null) {
                editText.setTextColor(Color.BLACK);
                editText.setHintTextColor(Color.GRAY);
                editText.setTextSize(16f);

                // Set up the search listener
                editText.setOnEditorActionListener((v, actionId, event) -> {
                    String query = v.getText().toString().trim().toLowerCase();
                    if (!query.isEmpty()) {
                        performSearch(query);
                        searchView.hide();  // hide overlay after search
                    }
                    return true;
                });
            }

            // Optional: Add text change listener for real-time search
            searchView.getEditText().addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // Optional: Implement real-time search here if needed
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }



    }
    private void performSearch(String query) {
        List<Project> matchingProjects = new ArrayList<>();
        List<String> matchingCompanyIds = new ArrayList<>();

        DatabaseReference projectsRef = FirebaseDatabase.getInstance().getReference("projects");
        DatabaseReference companiesRef = FirebaseDatabase.getInstance().getReference("users");

        projectsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot projectSnapshot) {
                for (DataSnapshot snap : projectSnapshot.getChildren()) {
                    Project project = snap.getValue(Project.class);
                    if (project != null && "approved".equals(project.getStatus())) {
                        boolean match = false;

                        // Match title
                        if (project.getTitle().toLowerCase().contains(query)) {
                            match = true;
                        }

                        // Match skills
                        if (!match && project.getSkills() != null) {
                            for (String skill : project.getSkills()) {
                                if (skill.toLowerCase().contains(query)) {
                                    match = true;
                                    break;
                                }
                            }
                        }

                        if (match) {
                            project.setProjectId(snap.getKey());
                            matchingProjects.add(project);
                        }
                    }
                }

                // Now search for matching companies
                companiesRef.orderByChild("role").equalTo("company")
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot companySnapshot) {
                                for (DataSnapshot companySnap : companySnapshot.getChildren()) {
                                    String companyId = companySnap.getKey();
                                    String companyName = companySnap.child("name").getValue(String.class);

                                    if (companyName != null && companyName.toLowerCase().contains(query)) {
                                        matchingCompanyIds.add(companyId);
                                    }
                                }

                                // Add projects belonging to matching companies
                                for (DataSnapshot snap : projectSnapshot.getChildren()) {
                                    Project project = snap.getValue(Project.class);
                                    if (project != null && "approved".equals(project.getStatus())) {
                                        if (matchingCompanyIds.contains(project.getCompanyId())) {
                                            project.setProjectId(snap.getKey());
                                            if (!matchingProjects.contains(project)) {
                                                matchingProjects.add(project);
                                            }
                                        }
                                    }
                                }

                                List<String> matchingCompanyNames = new ArrayList<>();
                                for (DataSnapshot companySnap : companySnapshot.getChildren()) {
                                    String companyName = companySnap.child("name").getValue(String.class);
                                    if (companyName != null && companyName.toLowerCase().contains(query)) {
                                        matchingCompanyNames.add(companyName);
                                    }
                                }
                                showSearchResults(matchingProjects, matchingCompanyNames);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Toast.makeText(StudentHomeActivity.this, "Failed to search companies", Toast.LENGTH_SHORT).show();
                            }
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(StudentHomeActivity.this, "Failed to search projects", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void showSearchResults(List<Project> matchingProjects, List<String> matchingCompanyNames) {
        View popupView = LayoutInflater.from(this).inflate(R.layout.popup_search_results, null);

        // Setup RecyclerView for projects
        RecyclerView rvProjects = popupView.findViewById(R.id.rv_matching_projects);
        rvProjects.setLayoutManager(new LinearLayoutManager(this));
        ProjectAdapterHome projectAdapter = new ProjectAdapterHome(matchingProjects, project -> {
            Toast.makeText(this, "Selected Project: " + project.getTitle(), Toast.LENGTH_SHORT).show();
        }, false);
        rvProjects.setAdapter(projectAdapter);

        // Setup RecyclerView for companies
        RecyclerView rvCompanies = popupView.findViewById(R.id.rv_matching_companies);
        rvCompanies.setLayoutManager(new LinearLayoutManager(this));
        CompanyNameAdapter companyAdapter = new CompanyNameAdapter(matchingCompanyNames);
        rvCompanies.setAdapter(companyAdapter);

        // Popup setup
        PopupWindow popupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );

        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setAnimationStyle(R.style.PopupAnimation);
        popupWindow.showAtLocation(findViewById(android.R.id.content), Gravity.CENTER, 0, 0);
    }


    private void setupBottomNavigation() {
        // Set up bottom navigation listener
        bottomNavigationView.setOnItemSelectedListener(item -> {
            // Apply animation to the selected item
            applyItemSelectionAnimation(item.getItemId());

            int id = item.getItemId();
            if (id == R.id.navigation_home) {
                // We're already on home screen, refresh data
                refreshAllData();
                return true;
            } else if (id == R.id.navigation_projects) {
                showAllProjectsPopup();
                return true;
            } else if (id == R.id.navigation_applications) {
                showAllApplications();
                return true;
            } else if (id == R.id.navigation_profile) {
                intent = new Intent(this, StudentProfileActivity.class);
                startActivity(intent);
                return true;
            }
            return false;
        });

        // Set up scroll behavior for bottom navigation
        swipeRefreshLayout.getViewTreeObserver().addOnScrollChangedListener(() -> {
            if (swipeRefreshLayout.getScrollY() > 0 && isBottomNavVisible) {
                // Scrolling down, hide bottom navigation and FAB
                hideBottomNavigation();
            } else if (swipeRefreshLayout.getScrollY() == 0 && !isBottomNavVisible) {
                // At top, show bottom navigation and FAB
                showBottomNavigation();
            }
        });

        // Initialize with home selected
        bottomNavigationView.setSelectedItemId(R.id.navigation_home);
    }

    private void applyItemSelectionAnimation(int itemId) {
        // Find the view for the selected item
        View itemView = bottomNavigationView.findViewById(itemId);

        // Apply scale animation to the selected item
        if (itemView != null) {
            itemView.animate()
                    .scaleX(1.2f)
                    .scaleY(1.2f)
                    .setDuration(200)
                    .withEndAction(() -> {
                        itemView.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(100)
                                .start();
                    })
                    .start();
        }
    }

    private void showBottomNavigation() {
        bottomNavigationView.animate()
                .translationY(0f)
                .setInterpolator(new DecelerateInterpolator(2f))
                .setDuration(300)
                .start();
        isBottomNavVisible = true;
    }

    private void hideBottomNavigation() {
        bottomNavigationView.animate()
                .translationY(bottomNavigationView.getHeight())
                .setInterpolator(new AccelerateInterpolator(2f))
                .setDuration(300)
                .start();


        isBottomNavVisible = false;
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            // Refresh all data
            refreshAllData();
        });

        // Customize the progress indicator color
        swipeRefreshLayout.setColorSchemeResources(
                R.color.blue_500,
                R.color.green,
                R.color.red,
                R.color.yellow
        );
    }

    private void refreshAllData() {
        // Show the main loading indicator if content isn't visible yet
        if (mainContent.getVisibility() != View.VISIBLE) {
            loadingIndicator.setVisibility(View.VISIBLE);
        }

        // Refresh all the data components
        setWelcomeMessage();
        setupNotificationBell();
        setupProjectsRecyclerView();

        // Hide the swipe refresh indicator when all operations are complete
        swipeRefreshLayout.setRefreshing(false);
    }

    private void addDots(int count) {
        if (dotIndicatorLayout == null || count <= 0) return;

        dotIndicatorLayout.removeAllViews();

        int sizeInPx = (int) (10 * getResources().getDisplayMetrics().density);
        int marginInPx = (int) (4 * getResources().getDisplayMetrics().density);

        for (int i = 0; i < count; i++) {
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
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(studentId);
        NavigationView navigationView = findViewById(R.id.nav_view);
        View headerView = navigationView.getHeaderView(0);
        TextView navMenuName = headerView.findViewById(R.id.menu_name);
        TextView navMenuMail = headerView.findViewById(R.id.menu_mail);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String name = snapshot.child("name").getValue(String.class);
                String email = snapshot.child("email").getValue(String.class);

                if (name != null) {
                    welcomeText.setText("Welcome back, " + name);
                    navMenuName.setText(name);
                } else {
                    welcomeText.setText("Welcome back");
                }

                if (email != null) {
                    navMenuMail.setText(email);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                welcomeText.setText("Welcome back");
            }
        });
    }


    private void setupNotificationBell() {
        String userId = studentId;
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

        String userId = studentId;
        DatabaseReference projectsRef = FirebaseDatabase.getInstance().getReference("projects");
        DatabaseReference appliedRef = FirebaseDatabase.getInstance().getReference("users").child(userId).child("appliedProjects");

        appliedRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot appliedSnapshot) {
                List<String> appliedProjectIds = new ArrayList<>();
                for (DataSnapshot snap : appliedSnapshot.getChildren()) {
                    appliedProjectIds.add(snap.getKey());
                }

                projectsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Project> filteredProjects = new ArrayList<>();
                        long currentTime = System.currentTimeMillis();

                        for (DataSnapshot snap : snapshot.getChildren()) {
                            Project project = snap.getValue(Project.class);
                            String projectId = snap.getKey();

                            if (project == null || projectId == null) continue;
                            if (!"approved".equals(project.getStatus())) continue;

                            int applicants = project.getApplicants();
                            int needed = project.getStudentsRequired();
                            long startDate = project.getStartDate();

                            boolean hasStarted = startDate <= currentTime;
                            boolean isFull = applicants >= needed;
                            boolean alreadyApplied = appliedProjectIds.contains(projectId);

                            if ((isFull && hasStarted) || isFull || alreadyApplied) continue;

                            project.setProjectId(projectId);
                            filteredProjects.add(project);
                        }


                        // Limit to max 3 projects
                        if (filteredProjects.size() > 3) {
                            filteredProjects = filteredProjects.subList(0, 3);
                        }

                        projectAdapterHome = new ProjectAdapterHome(filteredProjects, project -> {
                            Toast.makeText(StudentHomeActivity.this, "Clicked: " + project.getTitle(), Toast.LENGTH_SHORT).show();
                        }, true);

                        projectsRecyclerView.setAdapter(projectAdapterHome);
                        addDots(filteredProjects.size());

                        projectsRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                            @Override
                            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                                if (layoutManager != null) {
                                    int firstVisibleItem = layoutManager.findFirstVisibleItemPosition();
                                    updateActiveDot(firstVisibleItem);
                                }

                                // Handle bottom navigation visibility based on recyclerview scroll
                                if (dy > 0 && isBottomNavVisible) {
                                    // Scrolling down, hide bottom navigation
                                    hideBottomNavigation();
                                } else if (dy < 0 && !isBottomNavVisible) {
                                    // Scrolling up, show bottom navigation
                                    showBottomNavigation();
                                }
                            }
                        });

                        loadingIndicator.setVisibility(View.GONE);
                        mainContent.setVisibility(View.VISIBLE);
                        swipeRefreshLayout.setRefreshing(false);

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(StudentHomeActivity.this, "Failed to load projects", Toast.LENGTH_SHORT).show();
                        swipeRefreshLayout.setRefreshing(false);

                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(StudentHomeActivity.this, "Failed to load applied projects", Toast.LENGTH_SHORT).show();
                swipeRefreshLayout.setRefreshing(false);

            }
        });
    }

    private List<Project> getSampleProjects() {
        return java.util.Collections.emptyList();
    }

    private void setupClickListeners() {
        findViewById(R.id.view_all_applications_btn).setOnClickListener(v -> {
            bottomNavigationView.setSelectedItemId(R.id.navigation_applications);
        });

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


    private void showAllApplications() {
        View popupView = LayoutInflater.from(this).inflate(R.layout.popup_all_applications, null);

        RecyclerView rvApplications = popupView.findViewById(R.id.rv_applications);
        FloatingActionButton addButton = popupView.findViewById(R.id.btn_add_application);
        ImageView closeButton = popupView.findViewById(R.id.btn_close_popup);

        rvApplications.setLayoutManager(new LinearLayoutManager(this));

        List<Application> applications = new ArrayList<>();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("applications");

        ref.orderByChild("userId").equalTo(studentId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot appSnap : snapshot.getChildren()) {
                            Application app = appSnap.getValue(Application.class);
                            if (app != null) {
                                app.setApplicationId(appSnap.getKey());
                                applications.add(app);
                            }
                        }

                        ApplicationAdapter adapter = new ApplicationAdapter(applications, studentId);
                        rvApplications.setAdapter(adapter);

                        float SWIPE_THRESHOLD = 0.5f;

                        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
                            @Override
                            public boolean onMove(@NonNull RecyclerView recyclerView,
                                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                                  @NonNull RecyclerView.ViewHolder target) {
                                return false;
                            }

                            @Override
                            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                                // Prevent default removal behavior
                                adapter.notifyItemChanged(viewHolder.getAdapterPosition());
                            }

                            @Override
                            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                                    @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                                    int actionState, boolean isCurrentlyActive) {

                                View itemView = viewHolder.itemView;
                                Drawable icon;
                                int iconMargin = (itemView.getHeight() - 48) / 2;
                                int iconTop = itemView.getTop() + iconMargin;
                                int iconBottom = iconTop + 48;

                                if (dX < 0) { // Swipe Left → DELETE
                                    ColorDrawable background = new ColorDrawable(Color.RED);
                                    background.setBounds(itemView.getRight() + (int) dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
                                    background.draw(c);

                                    icon = ContextCompat.getDrawable(StudentHomeActivity.this, R.drawable.ic_delete_white);
                                    if (icon != null) {
                                        int iconRight = itemView.getRight() - iconMargin;
                                        int iconLeft = iconRight - icon.getIntrinsicWidth();
                                        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                                        icon.draw(c);
                                    }

                                    // If swipe passed threshold, show confirmation dialog
                                    if (Math.abs(dX) > itemView.getWidth() * SWIPE_THRESHOLD && isCurrentlyActive) {
                                        int position = viewHolder.getAdapterPosition();
                                        Application app = applications.get(position);

                                        // ❌ Prevent deletion of Accepted/Rejected applications
                                        if ("Accepted".equalsIgnoreCase(app.getStatus()) || "Rejected".equalsIgnoreCase(app.getStatus())) {
                                            Toast.makeText(StudentHomeActivity.this, "You cannot delete an accepted or rejected application.", Toast.LENGTH_SHORT).show();
                                            adapter.notifyItemChanged(position); // Restore the item
                                            return;
                                        }

                                        // ✅ Proceed with deletion confirmation for other statuses
                                        new AlertDialog.Builder(StudentHomeActivity.this)
                                                .setTitle("Delete Application")
                                                .setMessage("Are you sure you want to delete this application?")
                                                .setPositiveButton("Delete", (dialog, which) -> {
                                                    FirebaseDatabase.getInstance().getReference("applications")
                                                            .child(app.getApplicationId())
                                                            .removeValue()
                                                            .addOnSuccessListener(aVoid -> {
                                                                applications.remove(position);
                                                                adapter.notifyItemRemoved(position);
                                                                Toast.makeText(StudentHomeActivity.this, "Application deleted", Toast.LENGTH_SHORT).show();
                                                            })
                                                            .addOnFailureListener(e -> {
                                                                Toast.makeText(StudentHomeActivity.this, "Failed to delete", Toast.LENGTH_SHORT).show();
                                                                adapter.notifyItemChanged(position);
                                                            });
                                                })
                                                .setNegativeButton("Cancel", (dialog, which) -> {
                                                    adapter.notifyItemChanged(position);
                                                    dialog.dismiss();
                                                })
                                                .setCancelable(false)
                                                .show();
                                    }

                                } else if (dX > 0) { // Swipe Right → VIEW
                                    ColorDrawable background = new ColorDrawable(Color.parseColor("#2196F3")); // Blue
                                    background.setBounds(itemView.getLeft(), itemView.getTop(), itemView.getLeft() + (int) dX, itemView.getBottom());
                                    background.draw(c);

                                    icon = ContextCompat.getDrawable(StudentHomeActivity.this, R.drawable.ic_eye_open);
                                    if (icon != null) {
                                        int iconLeft = itemView.getLeft() + iconMargin;
                                        int iconRight = iconLeft + icon.getIntrinsicWidth();
                                        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                                        icon.draw(c);
                                    }

                                    if (dX > itemView.getWidth() * SWIPE_THRESHOLD && isCurrentlyActive) {
                                        int position = viewHolder.getAdapterPosition();
                                        Application app = applications.get(position);
                                        Intent intent = new Intent(StudentHomeActivity.this, ViewApplications.class);
                                        intent.putExtra("APPLICATION_ID", app.getApplicationId());
                                        startActivity(intent);
                                    }
                                }

                                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                            }
                        };

                        new ItemTouchHelper(simpleCallback).attachToRecyclerView(rvApplications);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(StudentHomeActivity.this, "Error loading applications", Toast.LENGTH_SHORT).show();
                    }
                });

        addButton.setOnClickListener(v -> {
            Toast.makeText(this, "Add new application clicked", Toast.LENGTH_SHORT).show();
        });

        final PopupWindow popupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                true
        );
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setAnimationStyle(R.style.PopupAnimation);
        popupWindow.showAtLocation(findViewById(android.R.id.content), Gravity.CENTER, 0, 0);

        closeButton.setOnClickListener(v -> popupWindow.dismiss());
    }

    private void showAllTips() {
        Toast.makeText(this, "Showing all tips", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_profile) {
            bottomNavigationView.setSelectedItemId(R.id.navigation_profile);
        }
        else if (id == R.id.nav_applications) {
            bottomNavigationView.setSelectedItemId(R.id.navigation_applications);
        } else if (id == R.id.nav_messages) {
            Toast.makeText(this, "Messages", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_notifications) {
            Toast.makeText(this, "Notifications", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_settings) {
            intent = new Intent(this, StudentSettingsActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_help) {
            intent = new Intent(this, HelpCenterStudentActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_logout) {
            finish();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void updateActiveDot(int position) {
        if (dotIndicatorLayout == null) return;

        for (int i = 0; i < dotIndicatorLayout.getChildCount(); i++) {
            dotIndicatorLayout.getChildAt(i).setBackgroundResource(R.drawable.circle_dot);
        }

        if (position >= 0 && position < dotIndicatorLayout.getChildCount()) {
            dotIndicatorLayout.getChildAt(position).setBackgroundResource(R.drawable.circle_dot_active);
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();}
    }
}