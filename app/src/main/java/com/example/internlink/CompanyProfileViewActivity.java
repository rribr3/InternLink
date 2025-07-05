package com.example.internlink;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CompanyProfileViewActivity extends AppCompatActivity {

    // UI Components
    private Toolbar toolbar;
    private ProgressBar progressBar;
    private ImageView companyLogo;
    private TextView tvCompanyName, tvIndustry, tvRating;
    private TextView tvLocation, tvWorkType, tvCompanySize;
    private TextView tvDescription, tvMission, tvVision;
    private TextView tvEmail, tvPhone, tvWebsite;
    private LinearLayout websiteLayout;
    private Button btnEmail, btnCall, btnWebsite;
    private ImageView btnLinkedin, btnTwitter;
    private RecyclerView rvRecentProjects;
    private RatingBar ratingBar;

    // Feedback UI Components
    private Button btnAddFeedback;
    private LinearLayout feedbackSummary, noReviewsLayout;
    private TextView tvAverageRating, tvTotalReviews;
    private TextView tv5StarCount, tv4StarCount, tv3StarCount, tv2StarCount, tv1StarCount;
    private ProgressBar progress5Star, progress4Star, progress3Star, progress2Star, progress1Star;
    private RatingBar ratingBarAverage;
    private RecyclerView rvFeedback;

    // Data
    private DatabaseReference databaseReference;
    private String companyId;
    private String currentUserId;
    private ProjectAdapterHome projectAdapter;
    private List<Project> projectsList;
    private FeedbackExAdapter feedbackAdapter;
    private List<CompanyFeedback> feedbackList;
    private boolean canLeaveFeedback = false;
    private List<String> eligibleProjects = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_company_profile_view);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        // Get company ID from intent
        companyId = getIntent().getStringExtra("COMPANY_ID");
        if (companyId == null) {
            Toast.makeText(this, "Company not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupToolbar();
        setupRecyclerViews();
        loadCompanyData();
        setupClickListeners();
        checkFeedbackEligibility();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        progressBar = findViewById(R.id.progress_bar);
        companyLogo = findViewById(R.id.company_logo);
        tvCompanyName = findViewById(R.id.tv_company_name);
        tvIndustry = findViewById(R.id.tv_industry);
        tvRating = findViewById(R.id.tv_rating);
        ratingBar = findViewById(R.id.rating_bar);

        tvLocation = findViewById(R.id.tv_location);
        tvWorkType = findViewById(R.id.tv_work_type);
        tvCompanySize = findViewById(R.id.tv_company_size);

        tvDescription = findViewById(R.id.tv_description);
        tvMission = findViewById(R.id.tv_mission);
        tvVision = findViewById(R.id.tv_vision);

        tvEmail = findViewById(R.id.tv_email);
        tvPhone = findViewById(R.id.tv_phone);
        tvWebsite = findViewById(R.id.tv_website);
        websiteLayout = findViewById(R.id.website_layout);

        btnEmail = findViewById(R.id.btn_email);
        btnCall = findViewById(R.id.btn_call);
        btnWebsite = findViewById(R.id.btn_website);
        btnLinkedin = findViewById(R.id.btn_linkedin);
        btnTwitter = findViewById(R.id.btn_twitter);

        rvRecentProjects = findViewById(R.id.rv_recent_projects);

        // Feedback UI Components
        btnAddFeedback = findViewById(R.id.btn_add_feedback);
        feedbackSummary = findViewById(R.id.feedback_summary);
        noReviewsLayout = findViewById(R.id.no_reviews_layout);
        tvAverageRating = findViewById(R.id.tv_average_rating);
        tvTotalReviews = findViewById(R.id.tv_total_reviews);
        tv5StarCount = findViewById(R.id.tv_5_star_count);
        tv4StarCount = findViewById(R.id.tv_4_star_count);
        tv3StarCount = findViewById(R.id.tv_3_star_count);
        tv2StarCount = findViewById(R.id.tv_2_star_count);
        tv1StarCount = findViewById(R.id.tv_1_star_count);
        progress5Star = findViewById(R.id.progress_5_star);
        progress4Star = findViewById(R.id.progress_4_star);
        progress3Star = findViewById(R.id.progress_3_star);
        progress2Star = findViewById(R.id.progress_2_star);
        progress1Star = findViewById(R.id.progress_1_star);
        ratingBarAverage = findViewById(R.id.rating_bar_average);
        rvFeedback = findViewById(R.id.rv_feedback);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerViews() {
        // Projects RecyclerView
        projectsList = new ArrayList<>();
        projectAdapter = new ProjectAdapterHome(projectsList, project -> {
            Intent intent = new Intent(this, ApplyNowActivity.class);
            intent.putExtra("PROJECT_ID", project.getProjectId());
            startActivity(intent);
        }, false);

        LinearLayoutManager projectsLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rvRecentProjects.setLayoutManager(projectsLayoutManager);
        rvRecentProjects.setAdapter(projectAdapter);

        // Feedback RecyclerView (Horizontal)
        feedbackList = new ArrayList<>();
        feedbackAdapter = new FeedbackExAdapter(feedbackList, this);

        LinearLayoutManager feedbackLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rvFeedback.setLayoutManager(feedbackLayoutManager);
        rvFeedback.setAdapter(feedbackAdapter);
    }

    private void loadCompanyData() {
        showLoading(true);

        databaseReference.child("users").child(companyId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            populateCompanyData(snapshot);
                            loadCompanyProjects();
                            loadApprovedApplicantsCount();
                            loadCompanyFeedback();
                        } else {
                            showLoading(false);
                            Toast.makeText(CompanyProfileViewActivity.this,
                                    "Company data not found", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showLoading(false);
                        Toast.makeText(CompanyProfileViewActivity.this,
                                "Error loading company data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadCompanyFeedback() {
        databaseReference.child("company_feedback")
                .orderByChild("companyId")
                .equalTo(companyId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        feedbackList.clear();

                        for (DataSnapshot feedbackSnapshot : snapshot.getChildren()) {
                            CompanyFeedback feedback = feedbackSnapshot.getValue(CompanyFeedback.class);
                            if (feedback != null) {
                                feedback.setFeedbackId(feedbackSnapshot.getKey());
                                feedbackList.add(feedback);
                            }
                        }

                        // Sort by timestamp (newest first)
                        feedbackList.sort((f1, f2) -> Long.compare(f2.getTimestamp(), f1.getTimestamp()));

                        feedbackAdapter.updateFeedbackList(feedbackList);
                        updateFeedbackSummary();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(CompanyProfileViewActivity.this,
                                "Failed to load feedback: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateFeedbackSummary() {
        if (feedbackList.isEmpty()) {
            feedbackSummary.setVisibility(View.GONE);
            noReviewsLayout.setVisibility(View.VISIBLE);
            rvFeedback.setVisibility(View.GONE);

            // Update header rating to show no reviews
            ratingBar.setRating(0);
            tvRating.setText("No reviews yet");
            return;
        }

        // Show feedback components
        feedbackSummary.setVisibility(View.VISIBLE);
        noReviewsLayout.setVisibility(View.GONE);
        rvFeedback.setVisibility(View.VISIBLE);

        // Calculate summary statistics
        FeedbackSummary summary = calculateFeedbackSummary();

        // Update summary UI
        tvAverageRating.setText(String.format("%.1f", summary.getAverageRating()));
        tvTotalReviews.setText("Based on " + summary.getTotalReviews() + " reviews");
        ratingBarAverage.setRating(summary.getAverageRating());

        // Update star counts and progress bars
        tv5StarCount.setText(String.valueOf(summary.getFiveStarCount()));
        tv4StarCount.setText(String.valueOf(summary.getFourStarCount()));
        tv3StarCount.setText(String.valueOf(summary.getThreeStarCount()));
        tv2StarCount.setText(String.valueOf(summary.getTwoStarCount()));
        tv1StarCount.setText(String.valueOf(summary.getOneStarCount()));

        int total = summary.getTotalReviews();
        progress5Star.setProgress(total > 0 ? (summary.getFiveStarCount() * 100) / total : 0);
        progress4Star.setProgress(total > 0 ? (summary.getFourStarCount() * 100) / total : 0);
        progress3Star.setProgress(total > 0 ? (summary.getThreeStarCount() * 100) / total : 0);
        progress2Star.setProgress(total > 0 ? (summary.getTwoStarCount() * 100) / total : 0);
        progress1Star.setProgress(total > 0 ? (summary.getOneStarCount() * 100) / total : 0);

        // Update header rating
        ratingBar.setRating(summary.getAverageRating());
        tvRating.setText(String.format("%.1f (%d reviews)",
                summary.getAverageRating(), summary.getTotalReviews()));
    }

    private FeedbackSummary calculateFeedbackSummary() {
        if (feedbackList.isEmpty()) {
            return new FeedbackSummary(0, 0, 0, 0, 0, 0, 0);
        }

        float totalRating = 0;
        int fiveStar = 0, fourStar = 0, threeStar = 0, twoStar = 0, oneStar = 0;

        for (CompanyFeedback feedback : feedbackList) {
            totalRating += feedback.getRating();

            int rating = Math.round(feedback.getRating());
            switch (rating) {
                case 5: fiveStar++; break;
                case 4: fourStar++; break;
                case 3: threeStar++; break;
                case 2: twoStar++; break;
                case 1: oneStar++; break;
            }
        }

        float averageRating = totalRating / feedbackList.size();
        return new FeedbackSummary(averageRating, feedbackList.size(),
                fiveStar, fourStar, threeStar, twoStar, oneStar);
    }

    private void checkFeedbackEligibility() {
        if (currentUserId.isEmpty()) {
            btnAddFeedback.setVisibility(View.GONE);
            return;
        }

        // Check if user has completed projects with this company
        databaseReference.child("applications")
                .orderByChild("userId")
                .equalTo(currentUserId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        eligibleProjects.clear();
                        long currentTime = System.currentTimeMillis();

                        for (DataSnapshot appSnapshot : snapshot.getChildren()) {
                            String applicationCompanyId = appSnapshot.child("companyId").getValue(String.class);
                            String status = appSnapshot.child("status").getValue(String.class);
                            String projectId = appSnapshot.child("projectId").getValue(String.class);

                            if (companyId.equals(applicationCompanyId) && "Accepted".equals(status) && projectId != null) {
                                // Check if project is completed
                                checkProjectCompletion(projectId, currentTime);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        btnAddFeedback.setVisibility(View.GONE);
                    }
                });
    }

    private void checkProjectCompletion(String projectId, long currentTime) {
        databaseReference.child("projects").child(projectId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String status = snapshot.child("status").getValue(String.class);
                            Long startDate = snapshot.child("startDate").getValue(Long.class);
                            String duration = snapshot.child("duration").getValue(String.class);

                            if ("completed".equals(status) ||
                                    (startDate != null && isProjectCompleted(startDate, duration, currentTime))) {

                                eligibleProjects.add(projectId);

                                // Check if user already left feedback for this company
                                checkExistingFeedback();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Handle error
                    }
                });
    }

    private boolean isProjectCompleted(long startDate, String duration, long currentTime) {
        if (duration == null) return false;

        long durationMs = 0;
        if (duration.contains("1-2 weeks")) {
            durationMs = 14 * 24 * 60 * 60 * 1000L; // 2 weeks
        } else if (duration.contains("3-4 weeks")) {
            durationMs = 28 * 24 * 60 * 60 * 1000L; // 4 weeks
        } else if (duration.contains("1-2 months")) {
            durationMs = 60 * 24 * 60 * 60 * 1000L; // 2 months
        } else if (duration.contains("3-4 months")) {
            durationMs = 120 * 24 * 60 * 60 * 1000L; // 4 months
        } else if (duration.contains("5-6 months")) {
            durationMs = 180 * 24 * 60 * 60 * 1000L; // 6 months
        }

        return currentTime > (startDate + durationMs);
    }

    private void checkExistingFeedback() {
        databaseReference.child("company_feedback")
                .orderByChild("studentId")
                .equalTo(currentUserId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean hasExistingFeedback = false;

                        for (DataSnapshot feedbackSnapshot : snapshot.getChildren()) {
                            String feedbackCompanyId = feedbackSnapshot.child("companyId").getValue(String.class);
                            if (companyId.equals(feedbackCompanyId)) {
                                hasExistingFeedback = true;
                                break;
                            }
                        }

                        canLeaveFeedback = !eligibleProjects.isEmpty() && !hasExistingFeedback;
                        btnAddFeedback.setVisibility(canLeaveFeedback ? View.VISIBLE : View.GONE);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        btnAddFeedback.setVisibility(View.GONE);
                    }
                });
    }

    private void showAddFeedbackDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_add_feedback);
        dialog.getWindow().setLayout(
                (int) (getResources().getDisplayMetrics().widthPixels * 0.9),
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        // Initialize dialog views
        ImageView ivCompanyLogoDialog = dialog.findViewById(R.id.iv_company_logo_dialog);
        TextView tvCompanyNameDialog = dialog.findViewById(R.id.tv_company_name_dialog);
        TextView tvProjectNameDialog = dialog.findViewById(R.id.tv_project_name_dialog);
        RatingBar ratingBarDialog = dialog.findViewById(R.id.rating_bar_dialog);
        TextView tvRatingDescription = dialog.findViewById(R.id.tv_rating_description);
        TextInputEditText etComment = dialog.findViewById(R.id.et_comment);
        Button btnCancel = dialog.findViewById(R.id.btn_cancel);
        Button btnSubmit = dialog.findViewById(R.id.btn_submit);

        // Set company info
        tvCompanyNameDialog.setText(tvCompanyName.getText());
        if (!eligibleProjects.isEmpty()) {
            loadProjectTitle(eligibleProjects.get(0), tvProjectNameDialog);
        }

        // Load company logo
        Glide.with(this)
                .load(companyLogo.getDrawable())
                .into(ivCompanyLogoDialog);

        // Rating change listener
        ratingBarDialog.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            if (fromUser) {
                btnSubmit.setEnabled(rating > 0);
                String[] descriptions = {
                        "Tap stars to rate",
                        "Poor",
                        "Fair",
                        "Good",
                        "Very Good",
                        "Excellent"
                };
                tvRatingDescription.setText(descriptions[(int) rating]);
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSubmit.setOnClickListener(v -> {
            float rating = ratingBarDialog.getRating();
            String comment = etComment.getText().toString().trim();

            if (rating > 0) {
                submitFeedback(rating, comment, eligibleProjects.get(0));
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void loadProjectTitle(String projectId, TextView textView) {
        databaseReference.child("projects").child(projectId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String title = snapshot.child("title").getValue(String.class);
                        textView.setText(title != null ? title : "Project");
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        textView.setText("Project");
                    }
                });
    }

    private void submitFeedback(float rating, String comment, String projectId) {
        showLoading(true);

        // Get current user info
        databaseReference.child("users").child(currentUserId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                        String studentName = userSnapshot.child("name").getValue(String.class);
                        String studentProfileUrl = userSnapshot.child("logoUrl").getValue(String.class);

                        // Get project title
                        databaseReference.child("projects").child(projectId)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot projectSnapshot) {
                                        String projectTitle = projectSnapshot.child("title").getValue(String.class);

                                        // Create feedback object
                                        CompanyFeedback feedback = new CompanyFeedback(
                                                currentUserId,
                                                studentName != null ? studentName : "Anonymous",
                                                companyId,
                                                projectId,
                                                projectTitle != null ? projectTitle : "Project",
                                                rating,
                                                comment,
                                                System.currentTimeMillis(),
                                                studentProfileUrl
                                        );

                                        // Save to Firebase
                                        DatabaseReference feedbackRef = databaseReference
                                                .child("company_feedback").push();

                                        feedbackRef.setValue(feedback)
                                                .addOnSuccessListener(aVoid -> {
                                                    showLoading(false);
                                                    Toast.makeText(CompanyProfileViewActivity.this,
                                                            "Review submitted successfully!", Toast.LENGTH_SHORT).show();

                                                    // Refresh feedback
                                                    loadCompanyFeedback();
                                                    btnAddFeedback.setVisibility(View.GONE);
                                                })
                                                .addOnFailureListener(e -> {
                                                    showLoading(false);
                                                    Toast.makeText(CompanyProfileViewActivity.this,
                                                            "Failed to submit review: " + e.getMessage(),
                                                            Toast.LENGTH_SHORT).show();
                                                });
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        showLoading(false);
                                        Toast.makeText(CompanyProfileViewActivity.this,
                                                "Failed to load project info", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showLoading(false);
                        Toast.makeText(CompanyProfileViewActivity.this,
                                "Failed to load user info", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void populateCompanyData(DataSnapshot snapshot) {
        // Company Header
        String name = snapshot.child("name").getValue(String.class);
        String industry = snapshot.child("industry").getValue(String.class);
        String logoUrl = snapshot.child("logoUrl").getValue(String.class);

        tvCompanyName.setText(name != null ? name : "Company Name");
        tvIndustry.setText(industry != null ? industry : "Technology");

        // Load company logo
        if (logoUrl != null && !logoUrl.isEmpty()) {
            Glide.with(this)
                    .load(logoUrl)
                    .placeholder(R.drawable.ic_company)
                    .error(R.drawable.ic_company)
                    .into(companyLogo);
        }

        // Company Information
        String address = snapshot.child("address").getValue(String.class);
        String location = snapshot.child("location").getValue(String.class);

        String displayLocation = "";
        if (address != null && !address.isEmpty()) {
            displayLocation = address;
        } else if (location != null && !location.isEmpty()) {
            displayLocation = location;
        } else {
            displayLocation = "Location not specified";
        }
        tvLocation.setText(displayLocation);

        String workType = getWorkTypeFromDatabase(location);
        tvWorkType.setText(workType);

        tvCompanySize.setText("Loading...");

        // About Company
        String description = snapshot.child("description").getValue(String.class);
        String mission = snapshot.child("mission").getValue(String.class);
        String vision = snapshot.child("vision").getValue(String.class);

        tvDescription.setText(description != null && !description.isEmpty() ?
                description : "No description available");
        tvMission.setText(mission != null && !mission.isEmpty() ?
                mission : "To deliver innovative solutions");
        tvVision.setText(vision != null && !vision.isEmpty() ?
                vision : "To be a leading company in our industry");

        // Contact Information
        String email = snapshot.child("email").getValue(String.class);
        String phone = snapshot.child("phone").getValue(String.class);
        String website = snapshot.child("website").getValue(String.class);

        tvEmail.setText(email != null ? email : "");
        tvPhone.setText(phone != null ? phone : "");

        if (website != null && !website.isEmpty()) {
            websiteLayout.setVisibility(View.VISIBLE);
            tvWebsite.setText(website);
        } else {
            websiteLayout.setVisibility(View.GONE);
        }

        setupSocialMedia(snapshot);
    }

    private void loadApprovedApplicantsCount() {
        databaseReference.child("applications")
                .orderByChild("companyId")
                .equalTo(companyId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int approvedCount = 0;

                        for (DataSnapshot appSnapshot : snapshot.getChildren()) {
                            String status = appSnapshot.child("status").getValue(String.class);
                            if ("Accepted".equals(status)) {
                                approvedCount++;
                            }
                        }

                        String companySizeText = approvedCount + " Approved Trainees";
                        if (approvedCount == 0) {
                            companySizeText = "No Approved Trainees Yet";
                        } else if (approvedCount == 1) {
                            companySizeText = "1 Approved Trainee";
                        }

                        tvCompanySize.setText(companySizeText);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        tvCompanySize.setText("Trainees Count Unavailable");
                    }
                });
    }

    private String getWorkTypeFromDatabase(String location) {
        if (location == null || location.isEmpty()) {
            return "Work Type Not Specified";
        }

        String loc = location.toLowerCase().trim();

        switch (loc) {
            case "remote":
                return "Remote Work";
            case "in office":
                return "In Office";
            case "office (lebanon)":
                return "In Office • Lebanon";
            case "hybrid":
                return "Hybrid Work";
            case "flexible":
                return "Flexible Work";
            default:
                if (loc.contains("beirut") || loc.contains("lebanon")) {
                    return "In Office • " + capitalizeFirstLetter(location);
                } else if (loc.contains("office")) {
                    return "In Office";
                } else {
                    return capitalizeFirstLetter(location);
                }
        }
    }

    private String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private void loadCompanyProjects() {
        DatabaseReference projectsRef = databaseReference.child("projects");
        projectsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                projectsList.clear();

                for (DataSnapshot snap : snapshot.getChildren()) {
                    Project project = snap.getValue(Project.class);
                    String projectId = snap.getKey();

                    if (project == null || projectId == null) continue;

                    // Only filter by company ID - show ALL projects belonging to this company
                    if (companyId.equals(project.getCompanyId())) {
                        project.setProjectId(projectId);
                        projectsList.add(project);
                    }
                }

                // Sort projects by creation date (newest first) - optional
                projectsList.sort((p1, p2) -> Long.compare(p2.getCreatedAt(), p1.getCreatedAt()));

                projectAdapter.notifyDataSetChanged();
                showLoading(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showLoading(false);
                Toast.makeText(CompanyProfileViewActivity.this,
                        "Failed to load projects: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadProjectsWithoutFiltering() {
        DatabaseReference projectsRef = databaseReference.child("projects");
        projectsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                projectsList.clear();
                long currentTime = System.currentTimeMillis();

                for (DataSnapshot snap : snapshot.getChildren()) {
                    Project project = snap.getValue(Project.class);
                    String projectId = snap.getKey();

                    if (project == null || projectId == null) continue;

                    if (!companyId.equals(project.getCompanyId())) continue;

                    if (!"approved".equals(project.getStatus())) continue;

                    int applicants = project.getApplicants();
                    int needed = project.getStudentsRequired();
                    long startDate = project.getStartDate();

                    boolean hasStarted = startDate <= currentTime;
                    boolean isFull = applicants >= needed;

                    if (hasStarted || isFull) continue;

                    project.setProjectId(projectId);
                    projectsList.add(project);
                }

                if (projectsList.size() > 5) {
                    projectsList = projectsList.subList(0, 5);
                }

                projectAdapter.notifyDataSetChanged();
                showLoading(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showLoading(false);
                Toast.makeText(CompanyProfileViewActivity.this,
                        "Failed to load projects: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupSocialMedia(DataSnapshot snapshot) {
        String linkedin = snapshot.child("linkedin").getValue(String.class);
        String twitter = snapshot.child("twitter").getValue(String.class);

        if (linkedin != null && !linkedin.isEmpty()) {
            btnLinkedin.setOnClickListener(v -> openUrl(linkedin));
            btnLinkedin.setAlpha(1.0f);
            btnLinkedin.setEnabled(true);
        } else {
            btnLinkedin.setAlpha(0.5f);
            btnLinkedin.setEnabled(false);
        }

        if (twitter != null && !twitter.isEmpty()) {
            btnTwitter.setOnClickListener(v -> openUrl(twitter));
            btnTwitter.setAlpha(1.0f);
            btnTwitter.setEnabled(true);
        } else {
            btnTwitter.setAlpha(0.5f);
            btnTwitter.setEnabled(false);
        }
    }

    private void setupClickListeners() {
        // Contact buttons
        btnEmail.setOnClickListener(v -> {
            String email = tvEmail.getText().toString();
            if (!email.isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:" + email));
                intent.putExtra(Intent.EXTRA_SUBJECT, "Inquiry from InternLink");
                try {
                    startActivity(Intent.createChooser(intent, "Send Email"));
                } catch (Exception e) {
                    Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnCall.setOnClickListener(v -> {
            String phone = tvPhone.getText().toString();
            if (!phone.isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + phone));
                try {
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(this, "Cannot make call", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnWebsite.setOnClickListener(v -> {
            String website = tvWebsite.getText().toString();
            if (!website.isEmpty()) {
                openUrl(website);
            }
        });

        // Add feedback button
        btnAddFeedback.setOnClickListener(v -> {
            if (canLeaveFeedback) {
                showAddFeedbackDialog();
            }
        });
    }

    private void openUrl(String url) {
        String formattedUrl = url;
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            formattedUrl = "https://" + url;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl));
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Cannot open URL", Toast.LENGTH_SHORT).show();
        }
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
}