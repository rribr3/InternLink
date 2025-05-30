package com.example.internlink;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
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
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.search.SearchBar;
import com.google.android.material.search.SearchView;
import com.google.firebase.auth.FirebaseAuth;
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
    private List<Application> allApplications = new ArrayList<>();
    private List<Application> filteredApplications = new ArrayList<>();
    private ApplicationAdapter applicationAdapter;
    private ChipGroup filterChips;

    // Public static classes for search functionality
    public static class SearchResult {
        public String type; // "PROJECT" or "COMPANY"
        public String projectId;
        public Project project;
        public CompanyInfo company;
        public List<String> matchReasons;
        private List<Project> allProjects = new ArrayList<>();

    }

    public static class CompanyInfo {
        public String id;
        public String name;
        public String industry;
        public String description;
        public String location;
    }

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
        setupEnhancedSearch();
        setupClickListeners1();
    }

    private void setupEnhancedSearch() {
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
                    String query = v.getText().toString().trim();
                    if (!query.isEmpty() && query.length() >= 2) {
                        performEnhancedSearch(query);
                        searchView.hide();  // hide overlay after search
                    } else if (query.length() < 2) {
                        Toast.makeText(StudentHomeActivity.this, "Please enter at least 2 characters", Toast.LENGTH_SHORT).show();
                    }
                    return true;
                });
            }

            // Real-time search with debouncing
            searchView.getEditText().addTextChangedListener(new TextWatcher() {
                private Handler handler = new Handler();
                private Runnable searchRunnable;

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // Cancel previous search
                    if (searchRunnable != null) {
                        handler.removeCallbacks(searchRunnable);
                    }

                    // Create new search with delay
                    if (s.length() >= 2) {
                        searchRunnable = () -> performEnhancedSearch(s.toString().trim());
                        handler.postDelayed(searchRunnable, 500); // 500ms delay
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void performEnhancedSearch(String query) {
        List<SearchResult> searchResults = new ArrayList<>();

        DatabaseReference projectsRef = FirebaseDatabase.getInstance().getReference("projects");
        DatabaseReference companiesRef = FirebaseDatabase.getInstance().getReference("users");

        projectsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot projectSnapshot) {
                companiesRef.orderByChild("role").equalTo("company")
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot companySnapshot) {

                                // Create a map of company details for easy lookup
                                Map<String, CompanyInfo> companyMap = new HashMap<>();
                                for (DataSnapshot companySnap : companySnapshot.getChildren()) {
                                    String companyId = companySnap.getKey();
                                    CompanyInfo company = new CompanyInfo();
                                    company.id = companyId;
                                    company.name = companySnap.child("name").getValue(String.class);
                                    company.industry = companySnap.child("industry").getValue(String.class);
                                    company.description = companySnap.child("description").getValue(String.class);
                                    company.location = companySnap.child("location").getValue(String.class);
                                    companyMap.put(companyId, company);
                                }

                                // Search through projects
                                for (DataSnapshot projectSnap : projectSnapshot.getChildren()) {
                                    Project project = projectSnap.getValue(Project.class);
                                    if (project != null && "approved".equals(project.getStatus())) {

                                        SearchResult result = new SearchResult();
                                        result.type = "PROJECT";
                                        result.projectId = projectSnap.getKey();
                                        result.project = project;
                                        result.company = companyMap.get(project.getCompanyId());

                                        boolean matchFound = false;
                                        List<String> matchReasons = new ArrayList<>();

                                        // Check title match
                                        if (project.getTitle() != null &&
                                                project.getTitle().toLowerCase().contains(query.toLowerCase())) {
                                            matchFound = true;
                                            matchReasons.add("Title: " + project.getTitle());
                                        }

                                        // Check description match
                                        if (project.getDescription() != null &&
                                                project.getDescription().toLowerCase().contains(query.toLowerCase())) {
                                            matchFound = true;
                                            matchReasons.add("Description");
                                        }

                                        // Check category match
                                        if (project.getCategory() != null &&
                                                project.getCategory().toLowerCase().contains(query.toLowerCase())) {
                                            matchFound = true;
                                            matchReasons.add("Category: " + project.getCategory());
                                        }

                                        // Check skills match
                                        if (project.getSkills() != null) {
                                            for (String skill : project.getSkills()) {
                                                if (skill.toLowerCase().contains(query.toLowerCase())) {
                                                    matchFound = true;
                                                    matchReasons.add("Skill: " + skill);
                                                    break;
                                                }
                                            }
                                        }

                                        // Check company name match
                                        if (result.company != null && result.company.name != null &&
                                                result.company.name.toLowerCase().contains(query.toLowerCase())) {
                                            matchFound = true;
                                            matchReasons.add("Company: " + result.company.name);
                                        }

                                        // Check company industry match
                                        if (result.company != null && result.company.industry != null &&
                                                result.company.industry.toLowerCase().contains(query.toLowerCase())) {
                                            matchFound = true;
                                            matchReasons.add("Industry: " + result.company.industry);
                                        }

                                        // Check education level match
                                        if (project.getEducationLevel() != null &&
                                                project.getEducationLevel().toLowerCase().contains(query.toLowerCase())) {
                                            matchFound = true;
                                            matchReasons.add("Education: " + project.getEducationLevel());
                                        }

                                        // Check duration match
                                        if (project.getDuration() != null &&
                                                project.getDuration().toLowerCase().contains(query.toLowerCase())) {
                                            matchFound = true;
                                            matchReasons.add("Duration: " + project.getDuration());
                                        }

                                        // Check compensation type match
                                        if (project.getCompensationType() != null &&
                                                project.getCompensationType().toLowerCase().contains(query.toLowerCase())) {
                                            matchFound = true;
                                            matchReasons.add("Type: " + project.getCompensationType());
                                        }

                                        if (matchFound) {
                                            result.matchReasons = matchReasons;
                                            searchResults.add(result);
                                        }
                                    }
                                }

                                // Also add company-only results for companies that match but don't have matching projects
                                for (CompanyInfo company : companyMap.values()) {
                                    boolean companyMatch = false;
                                    List<String> companyMatchReasons = new ArrayList<>();

                                    if (company.name != null && company.name.toLowerCase().contains(query.toLowerCase())) {
                                        companyMatch = true;
                                        companyMatchReasons.add("Company Name: " + company.name);
                                    }

                                    if (company.industry != null && company.industry.toLowerCase().contains(query.toLowerCase())) {
                                        companyMatch = true;
                                        companyMatchReasons.add("Industry: " + company.industry);
                                    }

                                    if (company.description != null && company.description.toLowerCase().contains(query.toLowerCase())) {
                                        companyMatch = true;
                                        companyMatchReasons.add("Description");
                                    }

                                    if (companyMatch) {
                                        // Check if we already have projects from this company in results
                                        boolean alreadyHasProjects = false;
                                        for (SearchResult existing : searchResults) {
                                            if (existing.company != null && company.id.equals(existing.company.id)) {
                                                alreadyHasProjects = true;
                                                break;
                                            }
                                        }

                                        if (!alreadyHasProjects) {
                                            SearchResult companyResult = new SearchResult();
                                            companyResult.type = "COMPANY";
                                            companyResult.company = company;
                                            companyResult.matchReasons = companyMatchReasons;
                                            searchResults.add(companyResult);
                                        }
                                    }
                                }

                                // Sort results by relevance (projects first, then companies)
                                Collections.sort(searchResults, (a, b) -> {
                                    if (a.type.equals("PROJECT") && b.type.equals("COMPANY")) return -1;
                                    if (a.type.equals("COMPANY") && b.type.equals("PROJECT")) return 1;
                                    return 0;
                                });

                                showEnhancedSearchResults(searchResults, query);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Toast.makeText(StudentHomeActivity.this, "Search failed", Toast.LENGTH_SHORT).show();
                            }
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(StudentHomeActivity.this, "Search failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showEnhancedSearchResults(List<SearchResult> results, String query) {
        View popupView = LayoutInflater.from(this).inflate(R.layout.popup_enhanced_search_results, null);

        TextView searchQueryText = popupView.findViewById(R.id.tv_search_query);
        TextView resultsCountText = popupView.findViewById(R.id.tv_results_count);
        RecyclerView recyclerView = popupView.findViewById(R.id.rv_search_results);
        ImageView closeButton = popupView.findViewById(R.id.btn_close_search);
        LinearLayout emptyStateLayout = popupView.findViewById(R.id.layout_empty_state);

        searchQueryText.setText("Search results for: \"" + query + "\"");
        resultsCountText.setText(results.size() + " result(s) found");

        if (results.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);

            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            EnhancedSearchAdapter adapter = new EnhancedSearchAdapter(results);
            recyclerView.setAdapter(adapter);
        }

        PopupWindow popupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                true
        );

        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#F5F5F5")));
        popupWindow.showAtLocation(findViewById(android.R.id.content), Gravity.CENTER, 0, 0);

        closeButton.setOnClickListener(v -> popupWindow.dismiss());
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
            }  else if (id == R.id.nav_messages) {
                Intent intent = new Intent(this, MessagesActivity.class);
                intent.putExtra("STUDENT_ID", studentId); // Replace with actual variable name
                startActivity(intent);
                return true;
            }
            else if (id == R.id.navigation_map) {
                intent = new Intent(this, MapActivity.class);
                startActivity(intent);
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

                            if (hasStarted || isFull || alreadyApplied) continue;

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


    private void setupClickListeners1() {
        findViewById(R.id.view_all_applications_btn).setOnClickListener(v -> {
            showAllApplications();
        });



        notificationBell.setOnClickListener(v -> {
            // Navigate to StudentAnnounce.java
            Intent intent = new Intent(StudentHomeActivity.this, StudentAnnounce.class);
            startActivity(intent);
        });
    }



    private void setupClickListeners() {
        findViewById(R.id.btn_view_all_projects).setOnClickListener(v -> {
            showAllProjectsPopup();
        });
    }

    private void showAllProjectsPopup() {
        // Inflate the popup layout
        View popupView = LayoutInflater.from(this).inflate(R.layout.popup_all_projects, null);

        // Setup RecyclerView
        RecyclerView rvAllProjects = popupView.findViewById(R.id.rv_all_projects);

        // Use LinearLayoutManager with vertical orientation
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvAllProjects.setLayoutManager(layoutManager);

        // Add item decoration for spacing between items
        int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.project_item_spacing);
        rvAllProjects.addItemDecoration(new SpacesItemDecoration(spacingInPixels));

        List<Project> fetchedProjects = new ArrayList<>();
        DatabaseReference projectRef = FirebaseDatabase.getInstance().getReference("projects");

        // Show loading indicator
        ProgressBar progressBar = popupView.findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.VISIBLE);

        projectRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                fetchedProjects.clear();
                for (DataSnapshot projectSnap : snapshot.getChildren()) {
                    Project project = projectSnap.getValue(Project.class);
                    if (project != null && "approved".equals(project.getStatus())) {
                        project.setProjectId(projectSnap.getKey());
                        fetchedProjects.add(project);
                    }
                }



                ProjectAdapterHome adapter = new ProjectAdapterHome(fetchedProjects, project -> {
                    // Handle project click - open project details
                    Intent intent = new Intent(StudentHomeActivity.this, ProjectDetailsActivity.class);
                    intent.putExtra("PROJECT_ID", project.getProjectId());
                    startActivity(intent);
                }, false);

                rvAllProjects.setAdapter(adapter);
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getApplicationContext(), "Failed to load projects", Toast.LENGTH_SHORT).show();
            }
        });

        // Create and show the popup
        PopupWindow popupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                true
        );

        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setAnimationStyle(R.style.PopupAnimation);

        // Set elevation for better visual appearance
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            popupWindow.setElevation(20f);
        }

        // Set focusable to enable touch outside dismissal
        popupWindow.setFocusable(true);

        // Add close button functionality
        ImageView closeButton = popupView.findViewById(R.id.btn_close_popup);
        closeButton.setOnClickListener(v -> popupWindow.dismiss());

        popupWindow.showAtLocation(
                findViewById(android.R.id.content),
                Gravity.CENTER,
                10,
                10
        );
    }

    // Item decoration class for adding space between items
    public class SpacesItemDecoration extends RecyclerView.ItemDecoration {
        private final int space;

        public SpacesItemDecoration(int space) {
            this.space = space;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view,
                                   RecyclerView parent, RecyclerView.State state) {
            outRect.bottom = space;

            // Add top margin only for the first item to avoid double space between items
            if (parent.getChildAdapterPosition(view) == 0) {
                outRect.top = space;
            }
        }
    }


    private void showAllApplications() {
        View popupView = LayoutInflater.from(this).inflate(R.layout.popup_all_applications, null);

        RecyclerView rvApplications = popupView.findViewById(R.id.rv_applications);
        FloatingActionButton addButton = popupView.findViewById(R.id.btn_add_application);
        ImageView closeButton = popupView.findViewById(R.id.btn_close_popup);

        // Get filter chip references
        filterChips = popupView.findViewById(R.id.filter_chips);
        Chip chipAll = popupView.findViewById(R.id.chip_all);
        Chip chipAccepted = popupView.findViewById(R.id.chip_accepted);
        Chip chipInterview = popupView.findViewById(R.id.chip_interview);
        Chip chipPending = popupView.findViewById(R.id.chip_pending);
        Chip chipRejected = popupView.findViewById(R.id.chip_rejected);

        rvApplications.setLayoutManager(new LinearLayoutManager(this));

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("applications");

        ref.orderByChild("userId").equalTo(studentId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        allApplications.clear();
                        for (DataSnapshot appSnap : snapshot.getChildren()) {
                            Application app = appSnap.getValue(Application.class);
                            if (app != null) {
                                app.setApplicationId(appSnap.getKey());
                                allApplications.add(app);
                            }
                        }

                        // Initialize with all applications
                        filteredApplications = new ArrayList<>(allApplications);
                        applicationAdapter = new ApplicationAdapter(filteredApplications, studentId);
                        rvApplications.setAdapter(applicationAdapter);

                        // Set up filter chip listeners
                        setupFilterChips();

                        // Set up swipe functionality
                        setupSwipeFunctionality(rvApplications);
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
    private void setupFilterChips() {
        if (filterChips == null) return;

        filterChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                // If no chip is selected, default to "All"
                Chip chipAll = findViewById(R.id.chip_all);
                if (chipAll != null) {
                    chipAll.setChecked(true);
                }
                return;
            }

            int checkedId = checkedIds.get(0);
            String filterStatus = "";

            if (checkedId == R.id.chip_all) {
                filterStatus = "All";
            } else if (checkedId == R.id.chip_accepted) {
                filterStatus = "Accepted";
            } else if (checkedId == R.id.chip_interview) {
                filterStatus = "Shortlisted"; // Interview chip filters for Shortlisted status
            } else if (checkedId == R.id.chip_pending) {
                filterStatus = "Pending";
            } else if (checkedId == R.id.chip_rejected) {
                filterStatus = "Rejected";
            }

            filterApplications(filterStatus);
        });
    }

    // Add this new method to filter applications
    private void filterApplications(String status) {
        filteredApplications.clear();

        if ("All".equals(status)) {
            filteredApplications.addAll(allApplications);
        } else {
            for (Application app : allApplications) {
                String appStatus = app.getStatus();
                if (appStatus != null && status.equalsIgnoreCase(appStatus)) {
                    filteredApplications.add(app);
                }
            }
        }

        if (applicationAdapter != null) {
            applicationAdapter.notifyDataSetChanged();
        }

        // Optional: Show count of filtered results
        String chipText = getChipTextFromStatus(status);
        Toast.makeText(this, "Showing " + filteredApplications.size() + " " + chipText + " applications", Toast.LENGTH_SHORT).show();
    }

    // Helper method to get chip text from status
    private String getChipTextFromStatus(String status) {
        switch (status) {
            case "All": return "All";
            case "Accepted": return "Accepted";
            case "Shortlisted": return "Interview";
            case "Pending": return "Pending";
            case "Rejected": return "Rejected";
            default: return "All";
        }
    }

    // Extract the swipe functionality into a separate method for cleaner code
    private void setupSwipeFunctionality(RecyclerView rvApplications) {
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
                applicationAdapter.notifyItemChanged(viewHolder.getAdapterPosition());
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

                    if (Math.abs(dX) > itemView.getWidth() * SWIPE_THRESHOLD && isCurrentlyActive) {
                        int position = viewHolder.getAdapterPosition();
                        if (position < 0 || position >= filteredApplications.size()) return;

                        Application app = filteredApplications.get(position);

                        // ❌ Prevent deletion of Accepted/Rejected applications
                        if ("Accepted".equalsIgnoreCase(app.getStatus()) || "Rejected".equalsIgnoreCase(app.getStatus())) {
                            Toast.makeText(StudentHomeActivity.this, "You cannot delete an accepted or rejected application.", Toast.LENGTH_SHORT).show();
                            applicationAdapter.notifyItemChanged(position);
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
                                                // Remove from both lists
                                                allApplications.remove(app);
                                                filteredApplications.remove(position);
                                                applicationAdapter.notifyItemRemoved(position);
                                                Toast.makeText(StudentHomeActivity.this, "Application deleted", Toast.LENGTH_SHORT).show();
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(StudentHomeActivity.this, "Failed to delete", Toast.LENGTH_SHORT).show();
                                                applicationAdapter.notifyItemChanged(position);
                                            });
                                })
                                .setNegativeButton("Cancel", (dialog, which) -> {
                                    applicationAdapter.notifyItemChanged(position);
                                    dialog.dismiss();
                                })
                                .setCancelable(false)
                                .show();
                    }

                } else if (dX > 0) { // Swipe Right → VIEW
                    ColorDrawable background = new ColorDrawable(Color.parseColor("#2196F3"));
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
                        if (position < 0 || position >= filteredApplications.size()) return;

                        Application app = filteredApplications.get(position);
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

    // Optional: Add method to programmatically select a filter (useful for deep linking or testing)
    public void selectFilter(String status) {
        if (filterChips == null) return;

        int chipId = R.id.chip_all; // default

        switch (status) {
            case "Accepted":
                chipId = R.id.chip_accepted;
                break;
            case "Shortlisted":
                chipId = R.id.chip_interview;
                break;
            case "Pending":
                chipId = R.id.chip_pending;
                break;
            case "Rejected":
                chipId = R.id.chip_rejected;
                break;
            default:
                chipId = R.id.chip_all;
                break;
        }

        Chip chipToSelect = findViewById(chipId);
        if (chipToSelect != null) {
            chipToSelect.setChecked(true);
        }
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
            showAllApplications();
        } else if (id == R.id.nav_projects) {
            showAllProjectsPopup();
        } else if (id == R.id.nav_messages) {
            intent = new Intent(this, MessagesActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_notifications) {
            Intent intent = new Intent(StudentHomeActivity.this, StudentAnnounce.class);
            startActivity(intent);
        } else if (id == R.id.nav_feedback) {
            Toast.makeText(this, "Feedbacks", Toast.LENGTH_SHORT).show();
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
            super.onBackPressed();
        }
    }
}