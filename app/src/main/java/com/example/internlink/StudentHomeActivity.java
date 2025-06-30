package com.example.internlink;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
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
import com.google.android.material.card.MaterialCardView;
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

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
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

    // Search History
    private static final String SEARCH_HISTORY_PREFS = "search_history_prefs";
    private static final String SEARCH_HISTORY_KEY = "search_history";
    private static final int MAX_SEARCH_HISTORY = 10;
    private List<String> searchHistory = new ArrayList<>();
    private SharedPreferences searchHistoryPrefs;
    private SearchHistoryAdapter searchHistoryAdapter;
    private int id;

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

    // Tip model class
    public static class Tip {
        public int iconResId;
        public String title;
        public String description;
        public String actionId;
        public String colorHex;

        public Tip(int iconResId, String title, String description, String actionId, String colorHex) {
            this.iconResId = iconResId;
            this.title = title;
            this.description = description;
            this.actionId = actionId;
            this.colorHex = colorHex;
        }
    }
    // Add this inner class inside StudentHomeActivity
    private static class SimpleTip {
        String title;
        String description;
        String colorHex;

        SimpleTip(String title, String description, String colorHex) {
            this.title = title;
            this.description = description;
            this.colorHex = colorHex;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_home);

        // Check if user is logged in
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Get student ID and validate
        studentId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // ✅ ADDED: Validate studentId is not null or empty
        if (studentId == null || studentId.isEmpty()) {
            Toast.makeText(this, "Error: Invalid user ID", Toast.LENGTH_SHORT).show();
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Continue with the rest of your initialization...
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
        setupEnhancedSearchWithHistory();
        setupClickListeners1();
        // In your onCreate method, after setupBottomNavigation(), add:
        setupUnreadMessagesBadge();
        updateApplicationCounts();
        // Initialize dynamic tips
        initializeDynamicTips();

    }

    // DYNAMIC TIPS IMPLEMENTATION
    private void initializeDynamicTips() {
        setupDynamicTips();
    }

    private void setupDynamicTips() {
        LinearLayout tipsContainer = findViewById(R.id.tips_container);
        if (tipsContainer == null) return;

        // Show loading state
        showTipsLoading(tipsContainer);

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(studentId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Tip> personalizedTips = generatePersonalizedTipsFromDatabase(snapshot);
                displayTips(personalizedTips, tipsContainer);

                // Setup refresh button
                TextView refreshBtn = findViewById(R.id.tips_refresh_btn);
                if (refreshBtn != null) {
                    refreshBtn.setOnClickListener(v -> {
                        showTipsLoading(tipsContainer);
                        setupDynamicTips(); // Refresh tips
                    });
                }

                // Setup "See All Tips" button
                Button seeAllTipsBtn = findViewById(R.id.see_all_tips_btn);
                if (seeAllTipsBtn != null) {
                    seeAllTipsBtn.setOnClickListener(v -> showAllTipsDialog());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                displayDefaultTips(tipsContainer);
            }
        });
    }

    private void showTipsLoading(LinearLayout container) {
        container.removeAllViews();

        // Create loading view
        LinearLayout loadingLayout = new LinearLayout(this);
        loadingLayout.setOrientation(LinearLayout.HORIZONTAL);
        loadingLayout.setGravity(Gravity.CENTER);
        loadingLayout.setPadding(0, 40, 0, 40);

        ProgressBar progressBar = new ProgressBar(this);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(60, 60);
        progressParams.setMarginEnd(24);
        progressBar.setLayoutParams(progressParams);

        TextView loadingText = new TextView(this);
        loadingText.setText("Generating personalized tips...");
        loadingText.setTextColor(Color.parseColor("#666666"));
        loadingText.setTextSize(14f);

        loadingLayout.addView(progressBar);
        loadingLayout.addView(loadingText);
        container.addView(loadingLayout);
    }

    private List<Tip> generatePersonalizedTipsFromDatabase(DataSnapshot userSnapshot) {
        List<Tip> tips = new ArrayList<>();

        // Extract user data from Firebase
        String name = userSnapshot.child("name").getValue(String.class);
        String bio = userSnapshot.child("bio").getValue(String.class);
        String cvUrl = userSnapshot.child("cvUrl").getValue(String.class);
        String linkedin = userSnapshot.child("linkedin").getValue(String.class);
        String github = userSnapshot.child("github").getValue(String.class);
        String skills = userSnapshot.child("skills").getValue(String.class);
        String gpa = userSnapshot.child("gpa").getValue(String.class);
        String university = userSnapshot.child("university").getValue(String.class);
        String gradyear = userSnapshot.child("gradyear").getValue(String.class);
        String degree = userSnapshot.child("degree").getValue(String.class);
        String phone = userSnapshot.child("phone").getValue(String.class);
        String email = userSnapshot.child("email").getValue(String.class);

        // Priority 1: CV Upload (Critical)
        if (cvUrl == null || cvUrl.trim().isEmpty()) {
            tips.add(new Tip(
                    R.drawable.ic_download,
                    "Upload Your CV",
                    "A professional CV increases your chances by 70%. Upload yours now!",
                    "upload_cv",
                    "#E53E3E"
            ));
        }

        // Priority 2: Profile Completion
        int profileCompleteness = calculateProfileCompleteness(userSnapshot);
        if (profileCompleteness < 80) {
            List<String> missingFields = getMissingProfileFields(userSnapshot);
            String missingFieldsText = missingFields.size() > 2 ?
                    String.join(", ", missingFields.subList(0, 2)) + "..." :
                    String.join(", ", missingFields);

            tips.add(new Tip(
                    R.drawable.ic_profile,
                    "Complete Your Profile (" + profileCompleteness + "%)",
                    "Complete missing: " + missingFieldsText,
                    "complete_profile",
                    "#FF8C00"
            ));
        }

        // Priority 3: Skills Enhancement
        if (skills == null || skills.trim().isEmpty() || skills.split(",").length < 3) {
            tips.add(new Tip(
                    R.drawable.ic_skills,
                    "Add More Skills",
                    "Students with 5+ skills get 3x more interview calls. Add yours!",
                    "add_skills",
                    "#4299E1"
            ));
        }

        // Priority 4: LinkedIn Profile
        if (linkedin == null || linkedin.trim().isEmpty()) {
            tips.add(new Tip(
                    R.drawable.ic_linkedin,
                    "Connect on LinkedIn",
                    "85% of internships are filled through networking. Create your LinkedIn!",
                    "add_linkedin",
                    "#0077B5"
            ));
        }

        // Priority 5: GitHub Portfolio (for relevant students)
        if (github == null || github.trim().isEmpty()) {
            // Check if student has tech-related skills
            boolean isTechStudent = skills != null &&
                    (skills.toLowerCase().contains("java") ||
                            skills.toLowerCase().contains("python") ||
                            skills.toLowerCase().contains("javascript") ||
                            skills.toLowerCase().contains("html") ||
                            skills.toLowerCase().contains("css") ||
                            skills.toLowerCase().contains("react"));

            if (isTechStudent) {
                tips.add(new Tip(
                        R.drawable.ic_github,
                        "Showcase Your Code",
                        "Tech recruiters love seeing your projects. Add your GitHub profile!",
                        "add_github",
                        "#333333"
                ));
            }
        }

        // Priority 6: GPA-based tips
        if (gpa != null && !gpa.trim().isEmpty()) {
            try {
                double gpaValue = Double.parseDouble(gpa);
                if (gpaValue >= 3.5) {
                    tips.add(new Tip(
                            R.drawable.ic_star,
                            "Highlight Your Excellence",
                            "Your " + gpa + " GPA is impressive! Make sure it's visible on your CV",
                            "highlight_gpa",
                            "#38A169"
                    ));
                } else if (gpaValue < 3.0) {
                    tips.add(new Tip(
                            R.drawable.ic_skills,
                            "Boost Your Application",
                            "Compensate with strong projects and skills to impress employers",
                            "boost_application",
                            "#4299E1"
                    ));
                }
            } catch (NumberFormatException e) {
                // Invalid GPA format, skip
            }
        }

        // Priority 7: Graduation year-based advice
        if (gradyear != null && !gradyear.trim().isEmpty()) {
            try {
                int gradYear = Integer.parseInt(gradyear);
                int currentYear = Calendar.getInstance().get(Calendar.YEAR);

                if (gradYear == currentYear) {
                    tips.add(new Tip(
                            R.drawable.ic_clock,
                            "Final Year Advantage",
                            "You're graduating this year! Apply to full-time positions too",
                            "final_year_tip",
                            "#D69E2E"
                    ));
                } else if (gradYear == currentYear + 1) {
                    tips.add(new Tip(
                            R.drawable.ic_calendar,
                            "Perfect Timing",
                            "Junior year is ideal for summer internships. Start applying now!",
                            "junior_year_tip",
                            "#38A169"
                    ));
                }
            } catch (NumberFormatException e) {
                // Invalid year format, skip
            }
        }

        // Priority 8: Application encouragement
        tips.add(new Tip(
                R.drawable.ic_target,
                "This Week's Goal",
                "Apply to 3 internships and follow up on 2 previous applications",
                "weekly_goal",
                "#E53E3E"
        ));

        // Return top 3 most relevant tips
        return tips.subList(0, Math.min(3, tips.size()));
    }

    private int calculateProfileCompleteness(DataSnapshot userSnapshot) {
        String[] requiredFields = {"name", "bio", "email", "phone", "university", "degree", "gradyear", "gpa", "skills", "cvUrl", "linkedin"};
        int completedFields = 0;

        for (String field : requiredFields) {
            String value = userSnapshot.child(field).getValue(String.class);
            if (value != null && !value.trim().isEmpty()) {
                completedFields++;
            }
        }

        return (completedFields * 100) / requiredFields.length;
    }

    private List<String> getMissingProfileFields(DataSnapshot userSnapshot) {
        List<String> missingFields = new ArrayList<>();
        Map<String, String> fieldDisplayNames = new HashMap<>();
        fieldDisplayNames.put("bio", "Bio");
        fieldDisplayNames.put("phone", "Phone");
        fieldDisplayNames.put("skills", "Skills");
        fieldDisplayNames.put("cvUrl", "CV");
        fieldDisplayNames.put("linkedin", "LinkedIn");
        fieldDisplayNames.put("github", "GitHub");
        fieldDisplayNames.put("gpa", "GPA");
        fieldDisplayNames.put("university", "University");
        fieldDisplayNames.put("degree", "Degree");
        fieldDisplayNames.put("gradyear", "Graduation Year");

        for (Map.Entry<String, String> entry : fieldDisplayNames.entrySet()) {
            String value = userSnapshot.child(entry.getKey()).getValue(String.class);
            if (value == null || value.trim().isEmpty()) {
                missingFields.add(entry.getValue());
            }
        }

        return missingFields;
    }

    private void displayTips(List<Tip> tips, LinearLayout container) {
        container.removeAllViews();

        for (int i = 0; i < tips.size(); i++) {
            Tip tip = tips.get(i);
            View tipView = createTipView(tip);
            container.addView(tipView);

            // Add animation
            tipView.setAlpha(0f);
            tipView.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .setStartDelay(i * 100)
                    .start();
        }
    }

    private View createTipView(Tip tip) {
        // Create card view
        androidx.cardview.widget.CardView cardView = new androidx.cardview.widget.CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, 24);
        cardView.setLayoutParams(cardParams);
        cardView.setRadius(24);
        cardView.setCardElevation(6);
        cardView.setForeground(ContextCompat.getDrawable(this, android.R.drawable.list_selector_background));

        // Main container
        LinearLayout mainContainer = new LinearLayout(this);
        mainContainer.setOrientation(LinearLayout.HORIZONTAL);
        mainContainer.setPadding(32, 24, 32, 24);
        mainContainer.setGravity(Gravity.CENTER_VERTICAL);

        // Color indicator
        View colorIndicator = new View(this);
        LinearLayout.LayoutParams colorParams = new LinearLayout.LayoutParams(8, 80);
        colorParams.setMarginEnd(24);
        colorIndicator.setLayoutParams(colorParams);
        colorIndicator.setBackgroundColor(Color.parseColor(tip.colorHex));

        // Create rounded corners for color indicator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                colorIndicator.setBackground(ContextCompat.getDrawable(this, R.drawable.circle_dot_active));
                colorIndicator.getBackground().setTint(Color.parseColor(tip.colorHex));
            } catch (Exception e) {
                colorIndicator.setBackgroundColor(Color.parseColor(tip.colorHex));
            }
        } else {
            colorIndicator.setBackgroundColor(Color.parseColor(tip.colorHex));
        }

        // Icon container
        LinearLayout iconContainer = new LinearLayout(this);
        iconContainer.setOrientation(LinearLayout.VERTICAL);
        iconContainer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams iconContainerParams = new LinearLayout.LayoutParams(72, 72);
        iconContainerParams.setMarginEnd(24);
        iconContainer.setLayoutParams(iconContainerParams);
        try {
            iconContainer.setBackground(ContextCompat.getDrawable(this, R.drawable.circle_dot_faded));
        } catch (Exception e) {
            iconContainer.setBackgroundColor(Color.parseColor("#F0F0F0"));
        }

        // Icon
        ImageView icon = new ImageView(this);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(48, 48);
        icon.setLayoutParams(iconParams);
        icon.setImageResource(tip.iconResId);
        icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        iconContainer.addView(icon);

        // Text container
        LinearLayout textContainer = new LinearLayout(this);
        textContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        textContainer.setLayoutParams(textParams);

        // Title
        TextView title = new TextView(this);
        title.setText(tip.title);
        title.setTextColor(Color.BLACK);
        title.setTextSize(18f);
        title.setTypeface(null, Typeface.BOLD);
        title.setMaxLines(1);
        title.setEllipsize(TextUtils.TruncateAt.END);

        // Description
        TextView description = new TextView(this);
        description.setText(tip.description);
        description.setTextColor(Color.parseColor("#666666"));
        description.setTextSize(15f);
        description.setMaxLines(2);
        description.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        descParams.topMargin = 8;
        description.setLayoutParams(descParams);

        textContainer.addView(title);
        textContainer.addView(description);

        // Action arrow
        ImageView arrow = new ImageView(this);
        LinearLayout.LayoutParams arrowParams = new LinearLayout.LayoutParams(32, 32);
        arrowParams.setMarginStart(16);
        arrow.setLayoutParams(arrowParams);
        try {
            arrow.setImageResource(R.drawable.right_arrow);
        } catch (Exception e) {
            arrow.setImageResource(android.R.drawable.ic_menu_more);
        }
        arrow.setAlpha(0.6f);
        arrow.setColorFilter(Color.BLACK);

        // Add all views to main container
        mainContainer.addView(colorIndicator);
        mainContainer.addView(iconContainer);
        mainContainer.addView(textContainer);
        mainContainer.addView(arrow);

        cardView.addView(mainContainer);

        // Click listener
        cardView.setOnClickListener(v -> handleTipClick(tip));

        return cardView;
    }



    private void showWeeklyGoalDialog() {
        new AlertDialog.Builder(this)
                .setTitle("This Week's Action Plan")
                .setMessage("✅ Apply to 3 relevant internships\n" +
                        "✅ Follow up on 2 previous applications\n" +
                        "✅ Update one section of your profile\n" +
                        "✅ Connect with 5 professionals on LinkedIn\n" +
                        "✅ Practice 1 technical interview question")
                .setPositiveButton("Let's do this!", null)
                .setNegativeButton("Remind me later", null)
                .show();
    }

    private void trackTipInteraction(String actionId) {
        // Optional: Send analytics to Firebase
        Map<String, Object> interaction = new HashMap<>();
        interaction.put("userId", studentId);
        interaction.put("tipAction", actionId);
        interaction.put("timestamp", System.currentTimeMillis());

        // Uncomment if you want to track interactions
        // FirebaseDatabase.getInstance().getReference("tip_interactions").push().setValue(interaction);
    }

    private void displayDefaultTips(LinearLayout container) {
        List<Tip> defaultTips = Arrays.asList(
                new Tip(R.drawable.ic_profile, "Complete Your Profile", "A complete profile attracts more opportunities", "complete_profile", "#4299E1"),
                new Tip(R.drawable.ic_download, "Upload Your CV", "Make your CV stand out to employers", "upload_cv", "#E53E3E"),
                new Tip(R.drawable.ic_target, "Start Applying", "Your dream internship is waiting for you", "weekly_goal", "#38A169")
        );

        displayTips(defaultTips, container);
    }

    private void showAllTipsDialog() {
        // Inflate the custom dialog layout
        Dialog dialog = new Dialog(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_all_tips, null);
        dialog.setContentView(dialogView);

        // Setup close button
        ImageView btnClose = dialogView.findViewById(R.id.btn_close_tips_dialog);
        btnClose.setOnClickListener(v -> dialog.dismiss());

        // Get container references
        LinearLayout profileTipsContainer = dialogView.findViewById(R.id.profile_tips_container);
        LinearLayout strategyTipsContainer = dialogView.findViewById(R.id.strategy_tips_container);
        LinearLayout interviewTipsContainer = dialogView.findViewById(R.id.interview_tips_container);
        LinearLayout networkingTipsContainer = dialogView.findViewById(R.id.networking_tips_container);
        LinearLayout careerTipsContainer = dialogView.findViewById(R.id.career_tips_container);

        // Add tips to each section
        addTipsToSection(profileTipsContainer, Arrays.asList(
                new SimpleTip("Complete Your Profile", "A complete profile increases visibility by 80%", "#4CAF50"),
                new SimpleTip("Upload Your CV", "Keep your CV updated and professional", "#2196F3"),
                new SimpleTip("Add Key Skills", "List relevant skills to match with opportunities", "#FF9800")
        ));

        addTipsToSection(strategyTipsContainer, Arrays.asList(
                new SimpleTip("Quality Over Quantity", "Focus on opportunities that match your skills", "#E91E63"),
                new SimpleTip("Personalize Applications", "Tailor each application to the position", "#9C27B0"),
                new SimpleTip("Track Your Progress", "Monitor application statuses and follow up", "#3F51B5")
        ));

        addTipsToSection(interviewTipsContainer, Arrays.asList(
                new SimpleTip("Research Companies", "Learn about the company before interviews", "#F44336"),
                new SimpleTip("Practice Common Questions", "Prepare answers for typical questions", "#009688"),
                new SimpleTip("Be Punctual", "Join virtual interviews 5 minutes early", "#795548")
        ));

        addTipsToSection(networkingTipsContainer, Arrays.asList(
                new SimpleTip("Build LinkedIn Profile", "Connect with industry professionals", "#607D8B"),
                new SimpleTip("Engage with Companies", "Follow and interact with target companies", "#FF5722"),
                new SimpleTip("Attend Virtual Events", "Join career fairs and webinars", "#8BC34A")
        ));

        addTipsToSection(careerTipsContainer, Arrays.asList(
                new SimpleTip("Set Clear Goals", "Define your career objectives", "#00BCD4"),
                new SimpleTip("Keep Learning", "Stay updated with industry trends", "#CDDC39"),
                new SimpleTip("Build Portfolio", "Document your projects and achievements", "#FFC107")
        ));

        // Setup refresh button
        Button btnRefresh = dialogView.findViewById(R.id.btn_refresh_tips);
        btnRefresh.setOnClickListener(v -> {
            refreshTips(profileTipsContainer, strategyTipsContainer,
                    interviewTipsContainer, networkingTipsContainer,
                    careerTipsContainer);
        });

        // Setup got it button
        Button btnGotIt = dialogView.findViewById(R.id.btn_got_it);
        btnGotIt.setOnClickListener(v -> dialog.dismiss());

        // Show dialog with window animations
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        }
        dialog.show();
    }

    private void addTipsToSection(LinearLayout container, List<SimpleTip> tips) {
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (SimpleTip tip : tips) {
            View tipView = inflater.inflate(R.layout.item_tip_card, container, false);

            TextView titleView = tipView.findViewById(R.id.tip_title);
            TextView descriptionView = tipView.findViewById(R.id.tip_description);
            MaterialCardView cardView = tipView.findViewById(R.id.tip_card);

            titleView.setText(tip.title);
            descriptionView.setText(tip.description);

            // Set card stroke color
            try {
                cardView.setStrokeColor(Color.parseColor(tip.colorHex));
                cardView.setStrokeWidth(2);
            } catch (IllegalArgumentException e) {
                cardView.setStrokeColor(Color.GRAY);
            }

            // Add click animation
            cardView.setOnClickListener(v -> {
                v.animate()
                        .scaleX(0.95f)
                        .scaleY(0.95f)
                        .setDuration(100)
                        .withEndAction(() -> {
                            v.animate()
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .setDuration(100)
                                    .start();
                            handleTipClick(new Tip(R.drawable.ic_tip, tip.title, tip.description, "default", tip.colorHex));
                        })
                        .start();
            });

            container.addView(tipView);
        }
    }
    private void refreshTips(LinearLayout... containers) {
        // Add loading animation
        for (LinearLayout container : containers) {
            container.setAlpha(0.5f);
        }

        // Simulate refresh with delay
        new Handler().postDelayed(() -> {
            for (LinearLayout container : containers) {
                container.setAlpha(1.0f);
                // Re-add tips with new random order
                List<SimpleTip> currentTips = getCurrentTips(container);
                Collections.shuffle(currentTips);
                addTipsToSection(container, currentTips);
            }
        }, 500);
    }

    private List<SimpleTip> getCurrentTips(LinearLayout container) {
        List<SimpleTip> tips = new ArrayList<>();
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            TextView titleView = child.findViewById(R.id.tip_title);
            TextView descriptionView = child.findViewById(R.id.tip_description);
            MaterialCardView cardView = child.findViewById(R.id.tip_card);

            tips.add(new SimpleTip(
                    titleView.getText().toString(),
                    descriptionView.getText().toString(),
                    String.format("#%06X", (0xFFFFFF & cardView.getStrokeColor()))
            ));
        }
        return tips;
    }

    private void handleTipClick(Tip tip) {
        Intent intent = null;

        if (tip.title.contains("Profile") || tip.title.contains("CV") || tip.title.contains("Skills")) {
            intent = new Intent(this, StudentProfileActivity.class);
        } else if (tip.title.contains("Track") || tip.title.contains("Application")) {
            showAllApplications();
        } else if (tip.title.contains("Interview")) {
            intent = new Intent(this, StudentScheduleActivity.class);
        }

        if (intent != null) {
            startActivity(intent);
        }
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

    private void loadSearchHistory() {
        String historyJson = searchHistoryPrefs.getString(SEARCH_HISTORY_KEY, "[]");
        try {
            JSONArray jsonArray = new JSONArray(historyJson);
            searchHistory.clear();
            for (int i = 0; i < jsonArray.length(); i++) {
                searchHistory.add(jsonArray.getString(i));
            }
        } catch (JSONException e) {
            searchHistory = new ArrayList<>();
        }
    }

    private void saveSearchHistory() {
        try {
            JSONArray jsonArray = new JSONArray();
            for (String query : searchHistory) {
                jsonArray.put(query);
            }
            searchHistoryPrefs.edit().putString(SEARCH_HISTORY_KEY, jsonArray.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addToSearchHistory(String query) {
        // Remove if already exists to move to top
        searchHistory.remove(query);

        // Add to beginning
        searchHistory.add(0, query);

        // Limit history size
        if (searchHistory.size() > MAX_SEARCH_HISTORY) {
            searchHistory = searchHistory.subList(0, MAX_SEARCH_HISTORY);
        }

        saveSearchHistory();
    }

    private void setupEnhancedSearchWithHistory() {
        if (searchView != null && searchBar != null) {
            // Connect SearchBar with SearchView properly
            searchView.setupWithSearchBar(searchBar);

            // Set up the SearchBar click listener
            searchBar.setOnClickListener(v -> {
                searchView.show();
                updateSearchViewVisibility(true);
            });

            // Configure the SearchView's EditText
            EditText editText = searchView.getEditText();
            if (editText != null) {
                editText.setTextColor(Color.BLACK);
                editText.setHintTextColor(Color.GRAY);
                editText.setTextSize(16f);
                editText.setImeOptions(EditorInfo.IME_ACTION_SEARCH);

                // Handle search button click
                editText.setOnEditorActionListener((v, actionId, event) -> {
                    if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                        String query = v.getText().toString().trim();
                        if (!query.isEmpty() && query.length() >= 2) {
                            addToSearchHistory(query);
                            performEnhancedSearch(query);
                            searchView.hide();
                        } else if (query.length() < 2) {
                            Toast.makeText(StudentHomeActivity.this, "Please enter at least 2 characters", Toast.LENGTH_SHORT).show();
                        }
                        return true;
                    }
                    return false;
                });

                // Show/hide search content based on text input
                editText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        updateSearchViewVisibility(s.length() == 0);
                    }

                    @Override
                    public void afterTextChanged(Editable s) {}
                });

                // Handle focus to show history
                editText.setOnFocusChangeListener((v, hasFocus) -> {
                    if (hasFocus && editText.getText().toString().trim().isEmpty()) {
                        updateSearchViewVisibility(true);
                    }
                });
            }

            // Setup category chips click listeners
            setupCategoryChips();

            // Setup clear history button
            TextView clearHistoryBtn = findViewById(R.id.tv_clear_history);
            if (clearHistoryBtn != null) {
                clearHistoryBtn.setOnClickListener(v -> {
                    new AlertDialog.Builder(this)
                            .setTitle("Clear Search History")
                            .setMessage("Are you sure you want to clear all search history?")
                            .setPositiveButton("Clear", (dialog, which) -> {
                                searchHistory.clear();
                                saveSearchHistory();
                                updateSearchHistoryDisplay();
                                Toast.makeText(this, "Search history cleared", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                });
            }
        }
    }

    private void setupCategoryChips() {
        ChipGroup categoryChips = findViewById(R.id.search_categories_chips);
        if (categoryChips != null) {
            for (int i = 0; i < categoryChips.getChildCount(); i++) {
                View child = categoryChips.getChildAt(i);
                if (child instanceof Chip) {
                    Chip chip = (Chip) child;
                    chip.setOnClickListener(v -> {
                        String categoryText = chip.getText().toString();
                        searchView.getEditText().setText(categoryText);
                        addToSearchHistory(categoryText);
                        performEnhancedSearch(categoryText);
                        searchView.hide();
                    });
                }
            }
        }
    }

    private void updateSearchViewVisibility(boolean showHistory) {
        LinearLayout historyContainer = findViewById(R.id.search_history_container);
        LinearLayout categoriesContainer = findViewById(R.id.search_categories_container);
        LinearLayout tipsContainer = findViewById(R.id.search_tips_container);
        View divider = findViewById(R.id.search_divider);

        if (showHistory) {
            // Show history and categories
            if (historyContainer != null) historyContainer.setVisibility(View.VISIBLE);
            if (categoriesContainer != null) categoriesContainer.setVisibility(View.VISIBLE);
            if (tipsContainer != null) tipsContainer.setVisibility(View.VISIBLE);
            if (divider != null) divider.setVisibility(searchHistory.isEmpty() ? View.GONE : View.VISIBLE);

            updateSearchHistoryDisplay();
        } else {
            // Hide everything when typing
            if (historyContainer != null) historyContainer.setVisibility(View.GONE);
            if (categoriesContainer != null) categoriesContainer.setVisibility(View.GONE);
            if (tipsContainer != null) tipsContainer.setVisibility(View.GONE);
            if (divider != null) divider.setVisibility(View.GONE);
        }
    }

    private void updateSearchHistoryDisplay() {
        RecyclerView historyRecyclerView = findViewById(R.id.rv_search_history);
        LinearLayout emptyHistoryState = findViewById(R.id.empty_history_state);
        TextView clearHistoryBtn = findViewById(R.id.tv_clear_history);

        if (historyRecyclerView != null && emptyHistoryState != null) {
            if (searchHistory.isEmpty()) {
                historyRecyclerView.setVisibility(View.GONE);
                emptyHistoryState.setVisibility(View.VISIBLE);
                if (clearHistoryBtn != null) clearHistoryBtn.setVisibility(View.GONE);
            } else {
                historyRecyclerView.setVisibility(View.VISIBLE);
                emptyHistoryState.setVisibility(View.GONE);
                if (clearHistoryBtn != null) clearHistoryBtn.setVisibility(View.VISIBLE);

                // Setup RecyclerView
                if (historyRecyclerView.getAdapter() == null) {
                    historyRecyclerView.setLayoutManager(new LinearLayoutManager(this));
                    searchHistoryAdapter = new SearchHistoryAdapter(searchHistory, new SearchHistoryAdapter.OnHistoryItemClickListener() {
                        @Override
                        public void onHistoryClick(String query) {
                            searchView.getEditText().setText(query);
                            addToSearchHistory(query);
                            performEnhancedSearch(query);
                            searchView.hide();
                        }

                        @Override
                        public void onHistoryDelete(String query) {
                            searchHistory.remove(query);
                            saveSearchHistory();
                            updateSearchHistoryDisplay();

                            if (searchHistory.isEmpty()) {
                                Toast.makeText(StudentHomeActivity.this, "Search history cleared", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onClearAllHistory() {
                            new AlertDialog.Builder(StudentHomeActivity.this)
                                    .setTitle("Clear Search History")
                                    .setMessage("Are you sure you want to clear all search history?")
                                    .setPositiveButton("Clear", (dialog, which) -> {
                                        searchHistory.clear();
                                        saveSearchHistory();
                                        updateSearchHistoryDisplay();
                                        Toast.makeText(StudentHomeActivity.this, "Search history cleared", Toast.LENGTH_SHORT).show();
                                    })
                                    .setNegativeButton("Cancel", null)
                                    .show();
                        }
                    });
                    historyRecyclerView.setAdapter(searchHistoryAdapter);
                } else {
                    searchHistoryAdapter.notifyDataSetChanged();
                }
            }
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


    private void updateApplicationCounts() {
        TextView interviewCountView = findViewById(R.id.interview_count);
        TextView acceptedCountView = findViewById(R.id.accepted_count);
        TextView rejectedCountView = findViewById(R.id.rejected_count);

        // Initialize counters
        final int[] interviewCount = {0};
        final int[] acceptedCount = {0};
        final int[] rejectedCount = {0};

        DatabaseReference applicationsRef = FirebaseDatabase.getInstance().getReference("applications");
        applicationsRef.orderByChild("userId").equalTo(studentId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        // Reset counters
                        interviewCount[0] = 0;
                        acceptedCount[0] = 0;
                        rejectedCount[0] = 0;

                        // Count applications by status
                        for (DataSnapshot appSnapshot : snapshot.getChildren()) {
                            try {
                                // ✅ SAFER: Handle both string and other data types
                                Object statusObj = appSnapshot.child("status").getValue();
                                String status = null;

                                if (statusObj instanceof String) {
                                    status = (String) statusObj;
                                } else if (statusObj != null) {
                                    status = statusObj.toString();
                                }

                                if (status != null && !status.trim().isEmpty()) {
                                    switch (status.toLowerCase().trim()) {
                                        case "shortlisted":
                                            interviewCount[0]++;
                                            break;
                                        case "accepted":
                                            acceptedCount[0]++;
                                            break;
                                        case "rejected":
                                            rejectedCount[0]++;
                                            break;
                                        // "pending" and other statuses are not counted in these specific categories
                                    }
                                }
                            } catch (Exception e) {
                                // Log the error but continue processing other applications
                                android.util.Log.e("StudentHome", "Error processing application: " + e.getMessage());
                            }
                        }

                        // Update the UI on main thread
                        runOnUiThread(() -> {
                            if (interviewCountView != null) {
                                interviewCountView.setText(String.valueOf(interviewCount[0]));
                            }
                            if (acceptedCountView != null) {
                                acceptedCountView.setText(String.valueOf(acceptedCount[0]));
                            }
                            if (rejectedCountView != null) {
                                rejectedCountView.setText(String.valueOf(rejectedCount[0]));
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Handle error - set default values
                        runOnUiThread(() -> {
                            if (interviewCountView != null) {
                                interviewCountView.setText("0");
                            }
                            if (acceptedCountView != null) {
                                acceptedCountView.setText("0");
                            }
                            if (rejectedCountView != null) {
                                rejectedCountView.setText("0");
                            }
                        });

                        Toast.makeText(StudentHomeActivity.this, "Failed to load application counts", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Alternative method with better performance using ValueEventListener for real-time updates
    private void setupApplicationCountsListener() {
        TextView interviewCountView = findViewById(R.id.interview_count);
        TextView acceptedCountView = findViewById(R.id.accepted_count);
        TextView rejectedCountView = findViewById(R.id.rejected_count);

        DatabaseReference applicationsRef = FirebaseDatabase.getInstance().getReference("applications");

        // Create a listener that updates counts in real-time
        ValueEventListener countsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int interviewCount = 0;
                int acceptedCount = 0;
                int rejectedCount = 0;

                // Count applications by status for current student
                for (DataSnapshot appSnapshot : snapshot.getChildren()) {
                    String userId = appSnapshot.child("userId").getValue(String.class);
                    String status = appSnapshot.child("status").getValue(String.class);

                    // Only count applications for current student
                    if (studentId.equals(userId) && status != null) {
                        switch (status.toLowerCase()) {
                            case "shortlisted":
                                interviewCount++;
                                break;
                            case "accepted":
                                acceptedCount++;
                                break;
                            case "rejected":
                                rejectedCount++;
                                break;
                        }
                    }
                }

                // Update UI
                if (interviewCountView != null) {
                    interviewCountView.setText(String.valueOf(interviewCount));
                }
                if (acceptedCountView != null) {
                    acceptedCountView.setText(String.valueOf(acceptedCount));
                }
                if (rejectedCountView != null) {
                    rejectedCountView.setText(String.valueOf(rejectedCount));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Set default values on error
                if (interviewCountView != null) interviewCountView.setText("0");
                if (acceptedCountView != null) acceptedCountView.setText("0");
                if (rejectedCountView != null) rejectedCountView.setText("0");
            }
        };

        // Attach the listener
        applicationsRef.addValueEventListener(countsListener);
    }

    // Fix the navigation to messages in your setupBottomNavigation() method

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
            } else if (id == R.id.nav_messages) {
                // Clear the badge when opening messages
                bottomNavigationView.removeBadge(R.id.nav_messages);

                // ✅ FIXED: Ensure we have a valid student ID before navigating
                if (studentId != null && !studentId.isEmpty()) {
                    Intent intent = new Intent(this, MessagesActivity.class); // Changed from StudentChatActivity to MessagesActivity
                    intent.putExtra("USER_ID", studentId); // Use consistent key name
                    intent.putExtra("USER_ROLE", "student"); // Add role for clarity
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "User not properly logged in", Toast.LENGTH_SHORT).show();
                }
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
            // Refresh all data including tips
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

        // Refresh all the data components including tips
        setWelcomeMessage();
        setupNotificationBell();
        setupProjectsRecyclerView();
        setupDynamicTips(); // Refresh tips
        updateApplicationCounts(); // Add this line

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
                                    String recipientId = snap.child("recipientId").getValue(String.class);

                                    if (!readsSnapshot.hasChild(snap.getKey()) &&
                                            (recipientId == null || recipientId.isEmpty() || recipientId.equals(userId))) {
                                        unreadIds.add(snap.getKey());
                                    }
                                }

                                // Update the badge - show red dot if there are unread notifications
                                if (!unreadIds.isEmpty()) {
                                    notificationBadge.setVisibility(View.VISIBLE);
                                    // Remove text and just show as a red dot
                                    notificationBadge.setText("");
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
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.navigation_home);
        }
        setupNotificationBell();
        setupDynamicTips();
        setupUnreadMessagesBadge();
        updateApplicationCounts(); // Add this line
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
        return Collections.emptyList();
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

    // Replace the existing project categorization logic in showAllProjectsPopup() method

    // Replace the existing project categorization logic in showAllProjectsPopup() method

    // Replace the existing project categorization logic in showAllProjectsPopup() method

    private void showAllProjectsPopup() {
        // Inflate the popup layout
        View popupView = LayoutInflater.from(this).inflate(R.layout.popup_all_projects, null);

        // Setup RecyclerView
        RecyclerView rvAllProjects = popupView.findViewById(R.id.rv_all_projects);
        ChipGroup filterChips = popupView.findViewById(R.id.project_filter_chips);
        LinearLayout emptyStateLayout = popupView.findViewById(R.id.empty_state_layout);
        TextView emptyStateTitle = popupView.findViewById(R.id.tv_empty_state_title);
        TextView emptyStateMessage = popupView.findViewById(R.id.tv_empty_state_message);

        // Use LinearLayoutManager with vertical orientation and center items
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvAllProjects.setLayoutManager(layoutManager);

        // Add item decoration for spacing between items
        int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.project_item_spacing);
        rvAllProjects.addItemDecoration(new SpacesItemDecoration(spacingInPixels));

        // Lists to hold different types of projects
        List<Project> allProjects = new ArrayList<>();
        List<Project> ongoingProjects = new ArrayList<>();
        List<Project> acceptableProjects = new ArrayList<>();
        List<Project> completedProjects = new ArrayList<>();
        List<Project> filteredProjects = new ArrayList<>();

        DatabaseReference projectRef = FirebaseDatabase.getInstance().getReference("projects");
        DatabaseReference applicationsRef = FirebaseDatabase.getInstance().getReference("applications");

        // Show loading indicator
        ProgressBar progressBar = popupView.findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.VISIBLE);
        rvAllProjects.setVisibility(View.GONE);

        // First, get student's applications
        applicationsRef.orderByChild("userId").equalTo(studentId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot applicationsSnapshot) {
                        // Store student's applications for quick lookup
                        Map<String, String> studentApplications = new HashMap<>(); // projectId -> status

                        for (DataSnapshot appSnap : applicationsSnapshot.getChildren()) {
                            String projectId = appSnap.child("projectId").getValue(String.class);
                            String status = appSnap.child("status").getValue(String.class);
                            if (projectId != null && status != null) {
                                studentApplications.put(projectId, status);
                            }
                        }

                        // Now get all projects and categorize them
                        projectRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                allProjects.clear();
                                ongoingProjects.clear();
                                acceptableProjects.clear();
                                completedProjects.clear();

                                long currentTime = System.currentTimeMillis();

                                for (DataSnapshot projectSnap : snapshot.getChildren()) {
                                    Project project = projectSnap.getValue(Project.class);
                                    if (project != null && "approved".equals(project.getStatus())) {
                                        project.setProjectId(projectSnap.getKey());
                                        allProjects.add(project);

                                        String projectId = projectSnap.getKey();
                                        long startDate = project.getStartDate();
                                        long deadline = project.getDeadline();

                                        // Check if student has applied to this project and get status
                                        String applicationStatus = studentApplications.get(projectId);
                                        boolean hasApplied = applicationStatus != null;
                                        boolean isAccepted = "Accepted".equalsIgnoreCase(applicationStatus);

                                        // Categorization logic:
                                        // ONGOING: Projects that haven't started yet (can still apply)
                                        if (startDate > currentTime) {
                                            ongoingProjects.add(project);
                                        }
                                        // ACCEPTABLE/ACTIVE: Projects currently running AND student is accepted
                                        else if (startDate <= currentTime && deadline > currentTime && isAccepted) {
                                            acceptableProjects.add(project);
                                        }
                                        // COMPLETED: Projects student applied to AND deadline has passed
                                        else if (hasApplied && deadline <= currentTime) {
                                            completedProjects.add(project);
                                        }
                                        // Projects that are running but student not accepted, or projects ended
                                        // without student participation are not categorized (only appear in "All Projects")
                                    }
                                }

                                // Set up adapter with all projects initially
                                filteredProjects.addAll(allProjects);
                                ProjectAdapterHome adapter = new ProjectAdapterHome(filteredProjects, project -> {
                                    // Handle project click - open project details
                                    Intent intent = new Intent(StudentHomeActivity.this, ProjectDetailsActivity.class);
                                    intent.putExtra("PROJECT_ID", project.getProjectId());
                                    startActivity(intent);
                                }, false);

                                rvAllProjects.setAdapter(adapter);

                                // Set up filter chips with updated logic
                                setupProjectFilterChips(filterChips, allProjects, ongoingProjects, acceptableProjects, completedProjects,
                                        adapter, filteredProjects, emptyStateLayout, emptyStateTitle, emptyStateMessage);

                                progressBar.setVisibility(View.GONE);
                                rvAllProjects.setVisibility(View.VISIBLE);
                                updateEmptyState(filteredProjects, emptyStateLayout, emptyStateTitle, emptyStateMessage, "All Projects");
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                progressBar.setVisibility(View.GONE);
                                rvAllProjects.setVisibility(View.VISIBLE);
                                Toast.makeText(getApplicationContext(), "Failed to load projects", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        progressBar.setVisibility(View.GONE);
                        rvAllProjects.setVisibility(View.VISIBLE);
                        Toast.makeText(getApplicationContext(), "Failed to load applications", Toast.LENGTH_SHORT).show();
                    }
                });

        // Create and show the popup (rest of the method remains the same)
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

    // Update the updateEmptyState method to provide better messages
    private void updateEmptyState(List<Project> projects,
                                  LinearLayout emptyStateLayout,
                                  TextView emptyStateTitle,
                                  TextView emptyStateMessage,
                                  String filterType) {
        if (projects.isEmpty()) {
            emptyStateLayout.setVisibility(View.VISIBLE);

            switch (filterType.toLowerCase()) {
                case "ongoing":
                    emptyStateTitle.setText("No Ongoing Projects");
                    emptyStateMessage.setText("There are no projects available to apply for at the moment.\nProjects that haven't started yet will appear here!");
                    break;
                case "acceptable":
                    emptyStateTitle.setText("No Active Projects");
                    emptyStateMessage.setText("You don't have any accepted projects currently running.\nProjects you're accepted for (between start date and deadline) will appear here!");
                    break;
                case "completed":
                    emptyStateTitle.setText("No Completed Projects");
                    emptyStateMessage.setText("You haven't applied to any projects that have reached their deadlines yet.\nYour completed applications will appear here!");
                    break;
                default:
                    emptyStateTitle.setText("No Projects Available");
                    emptyStateMessage.setText("There are currently no approved projects.\nCheck back later for new opportunities!");
                    break;
            }
        } else {
            emptyStateLayout.setVisibility(View.GONE);
        }
    }
    private void checkApplicationStatusAndCategorize(Project project, List<Project> ongoingProjects, List<Project> completedProjects) {
        DatabaseReference applicationsRef = FirebaseDatabase.getInstance().getReference("applications");

        applicationsRef.orderByChild("userId").equalTo(studentId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot appSnap : snapshot.getChildren()) {
                            String projectId = appSnap.child("projectId").getValue(String.class);
                            String status = appSnap.child("status").getValue(String.class);

                            if (project.getProjectId().equals(projectId)) {
                                long currentTime = System.currentTimeMillis();
                                long deadline = project.getDeadline();

                                if ("Accepted".equalsIgnoreCase(status)) {
                                    // If accepted and deadline not passed, it's ongoing
                                    // If accepted and deadline passed, it's completed
                                    if (deadline <= currentTime) {
                                        if (!completedProjects.contains(project)) {
                                            completedProjects.add(project);
                                        }
                                    } else {
                                        if (!ongoingProjects.contains(project)) {
                                            ongoingProjects.add(project);
                                        }
                                    }
                                } else if ("Shortlisted".equalsIgnoreCase(status) || "Pending".equalsIgnoreCase(status)) {
                                    // Still in process, consider as ongoing if not past deadline
                                    if (deadline > currentTime && !ongoingProjects.contains(project)) {
                                        ongoingProjects.add(project);
                                    }
                                }
                                break;
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Handle error silently or log
                    }
                });
    }

    // Helper method to set up filter chips
    // Replace your existing setupProjectFilterChips method with this updated version
    // Replace your existing setupProjectFilterChips method with this updated version
    private void setupProjectFilterChips(ChipGroup filterChips,
                                         List<Project> allProjects,
                                         List<Project> ongoingProjects,
                                         List<Project> acceptableProjects,
                                         List<Project> completedProjects,
                                         ProjectAdapterHome adapter,
                                         List<Project> filteredProjects,
                                         LinearLayout emptyStateLayout,
                                         TextView emptyStateTitle,
                                         TextView emptyStateMessage) {

        Chip chipAll = filterChips.findViewById(R.id.chip_all_projects);
        Chip chipOngoing = filterChips.findViewById(R.id.chip_ongoing_projects);
        Chip chipAcceptable = filterChips.findViewById(R.id.chip_acceptable_projects); // Add this to your XML
        Chip chipCompleted = filterChips.findViewById(R.id.chip_completed_projects);

        // Update chip texts with counts
        chipAll.setText("All Projects (" + allProjects.size() + ")");
        chipOngoing.setText("Ongoing (" + ongoingProjects.size() + ")");
        chipAcceptable.setText("My Active (" + acceptableProjects.size() + ")");
        chipCompleted.setText("Completed (" + completedProjects.size() + ")");

        filterChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                // If no chip is selected, default to "All Projects"
                chipAll.setChecked(true);
                return;
            }

            int checkedId = checkedIds.get(0);
            String filterType = "";
            List<Project> projectsToShow = new ArrayList<>();

            if (checkedId == R.id.chip_all_projects) {
                filterType = "All Projects";
                projectsToShow.addAll(allProjects);
            } else if (checkedId == R.id.chip_ongoing_projects) {
                filterType = "Ongoing";
                projectsToShow.addAll(ongoingProjects);
            } else if (checkedId == R.id.chip_acceptable_projects) {
                filterType = "My Active";
                projectsToShow.addAll(acceptableProjects);
            } else if (checkedId == R.id.chip_completed_projects) {
                filterType = "Completed";
                projectsToShow.addAll(completedProjects);
            }

            // Update filtered projects and refresh adapter
            filteredProjects.clear();
            filteredProjects.addAll(projectsToShow);
            adapter.notifyDataSetChanged();

            // Update empty state
            updateEmptyState(filteredProjects, emptyStateLayout, emptyStateTitle, emptyStateMessage, filterType);

            // Show a toast with the filter result
            String message = "Showing " + filteredProjects.size() + " " + filterType.toLowerCase();
            if (filteredProjects.size() == 1) {
                message = message.replace("projects", "project");
            }
            Toast.makeText(StudentHomeActivity.this, message, Toast.LENGTH_SHORT).show();
        });
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

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_profile) {
            bottomNavigationView.setSelectedItemId(R.id.navigation_profile);
        }
        else if (id == R.id.nav_schedule) {
            // Navigate to Student Schedule Activity
            Intent intent = new Intent(this, StudentScheduleActivity.class);
            startActivity(intent);
        }
        else if (id == R.id.nav_applications) {
            showAllApplications();
        } else if (id == R.id.nav_projects) {
            showAllProjectsPopup();
        } else if (id == R.id.nav_messages) {
            // ✅ FIXED: Ensure we have a valid student ID before navigating
            if (studentId != null && !studentId.isEmpty()) {
                Intent intent = new Intent(this, MessagesActivity.class); // Use MessagesActivity instead of StudentChatActivity
                intent.putExtra("USER_ID", studentId);
                intent.putExtra("USER_ROLE", "student");
                startActivity(intent);
            } else {
                Toast.makeText(this, "User not properly logged in", Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.nav_notifications) {
            Intent intent = new Intent(StudentHomeActivity.this, StudentAnnounce.class);
            startActivity(intent);
        } else if (id == R.id.nav_feedback) {
            Intent intent = new Intent(this, StudentFeedbackActivity.class);
            startActivity(intent);
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

    private void setupUnreadMessagesBadge() {
        String userId = studentId;
        DatabaseReference notificationsRef = FirebaseDatabase.getInstance()
                .getReference("notifications")
                .child(userId);

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
                bottomNav.removeBadge(R.id.nav_messages);

                if (count > 0) {
                    // Create and show badge
                    com.google.android.material.badge.BadgeDrawable badge =
                            bottomNav.getOrCreateBadge(R.id.nav_messages);

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
        View messagesView = findViewById(R.id.nav_messages);

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
                badge.setBackgroundResource(R.drawable.bg_card_blue);

                // Position the badge
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                        size, size, Gravity.TOP | Gravity.END);
                params.setMargins(0, 5, 5, 0);
                badge.setLayoutParams(params);

                messagesContainer.addView(badge);
            }
        }


        // In your setupBottomNavigation method, update the messages case:
        else if (id == R.id.nav_messages) {
            // Clear the badge when opening messages
            bottomNavigationView.removeBadge(R.id.nav_messages);

            Intent intent = new Intent(this, MessagesActivity.class);
            intent.putExtra("STUDENT_ID", studentId);
            startActivity(intent);
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
