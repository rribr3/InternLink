package com.example.internlink;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ApplicantProfileActivity extends AppCompatActivity {

    private static final String TAG = "ApplicantProfile";

    // UI Components
    private ImageView profileImage;
    private TextView tvName, tvEmail, tvDegree, tvGPA, tvGradYear,
            tvBio, tvSkills, tvPhone, tvUniversity;
    private Button btnViewCV, btnContactEmail, btnContactPhone, btnMessage;
    private ProgressBar progressBar;
    private Toolbar toolbar;
    private CardView personalInfoCard, academicInfoCard, contactInfoCard;

    // Feedback UI Components
    private Button btnAddFeedback;
    private LinearLayout feedbackSummary, noReviewsLayout;
    private TextView tvAverageRating, tvTotalReviews;
    private TextView tv5StarCount, tv4StarCount, tv3StarCount, tv2StarCount, tv1StarCount;
    private ProgressBar progress5Star, progress4Star, progress3Star, progress2Star, progress1Star;
    private RatingBar ratingBarAverage, ratingBarHeader;
    private TextView tvRatingHeader;
    private RecyclerView rvFeedback;

    // Data variables
    private String applicantId;
    private String cvUrl;
    private String applicantEmail;
    private String applicantPhone;
    private String applicantName;
    private String currentCompanyName;
    private String currentCompanyId;

    // Feedback data
    private DatabaseReference databaseReference;
    private StudentFeedbackAdapter feedbackAdapter;
    private List<StudentFeedback> feedbackList;
    private boolean canLeaveFeedback = false;
    private List<String> eligibleProjects = new ArrayList<>();
    private RecyclerView rvCompletedProjects;
    private CompletedProjectsAdapter completedProjectsAdapter;
    private List<Project> completedProjects = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "=== ApplicantProfileActivity Started ===");

        // Check intent extras
        Intent receivedIntent = getIntent();
        if (receivedIntent != null) {
            applicantId = receivedIntent.getStringExtra("APPLICANT_ID");
            applicantName = receivedIntent.getStringExtra("APPLICANT_NAME");
            String debugSource = receivedIntent.getStringExtra("DEBUG_SOURCE");

            Log.d(TAG, "Intent Data:");
            Log.d(TAG, "  → Applicant ID: " + applicantId);
            Log.d(TAG, "  → Applicant Name: " + applicantName);
            Log.d(TAG, "  → Source: " + debugSource);

            if (applicantName != null) {
                Toast.makeText(this, "Loading " + applicantName + "'s profile...",
                        Toast.LENGTH_SHORT).show();
            }
        }

        setContentView(R.layout.activity_applicant_profile);

        // Initialize Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference();
        currentCompanyId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        initializeViews();
        setupToolbar();
        validateIntentData();
        setupRecyclerView();
        loadCurrentCompanyInfo();
        loadApplicantProfile();
        loadCompletedProjects();
        checkFeedbackEligibility();
    }

    private void initializeViews() {
        Log.d(TAG, "Initializing views...");

        // Existing views
        toolbar = findViewById(R.id.toolbar);
        progressBar = findViewById(R.id.progress_bar);
        profileImage = findViewById(R.id.profile_image);
        tvName = findViewById(R.id.tv_name);
        tvEmail = findViewById(R.id.tv_email);
        tvDegree = findViewById(R.id.tv_degree);
        tvGPA = findViewById(R.id.tv_gpa);
        tvGradYear = findViewById(R.id.tv_gradyear);
        tvBio = findViewById(R.id.tv_bio);
        tvSkills = findViewById(R.id.tv_skills);
        tvPhone = findViewById(R.id.tv_phone);
        tvUniversity = findViewById(R.id.tv_university);
        btnViewCV = findViewById(R.id.btn_view_cv);
        btnContactEmail = findViewById(R.id.btn_contact_email);
        btnContactPhone = findViewById(R.id.btn_contact_phone);
        btnMessage = findViewById(R.id.btn_contact);
        personalInfoCard = findViewById(R.id.personal_info_card);
        academicInfoCard = findViewById(R.id.academic_info_card);
        contactInfoCard = findViewById(R.id.contact_info_card);

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
        ratingBarHeader = findViewById(R.id.rating_bar_header);
        tvRatingHeader = findViewById(R.id.tv_rating_header);
        rvFeedback = findViewById(R.id.rv_feedback);

        Log.d(TAG, "✅ Views initialized successfully");

        rvCompletedProjects = findViewById(R.id.rv_completed_projects);
        rvCompletedProjects.setLayoutManager(new LinearLayoutManager(this));
        completedProjectsAdapter = new CompletedProjectsAdapter(this, rvCompletedProjects, completedProjects,
                project -> viewCertificate(project.getProjectId(), applicantId));
        rvCompletedProjects.setAdapter(completedProjectsAdapter);
    }

    private void loadCompletedProjects() {
        DatabaseReference applicationsRef = FirebaseDatabase.getInstance()
                .getReference("applications");

        // Create a temporary list to hold all completed projects
        List<Project> loadedProjects = new ArrayList<>();

        applicationsRef.orderByChild("userId").equalTo(applicantId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        // Clear the temporary list
                        loadedProjects.clear();

                        // Count how many projects we need to load
                        final int[] totalProjects = {0};
                        final int[] loadedCount = {0};

                        for (DataSnapshot appSnap : snapshot.getChildren()) {
                            String status = appSnap.child("status").getValue(String.class);
                            if ("Accepted".equals(status)) {
                                totalProjects[0]++;
                            }
                        }

                        if (totalProjects[0] == 0) {
                            // No projects to load, update adapter with empty list
                            completedProjectsAdapter.updateProjects(loadedProjects);
                            return;
                        }

                        // Load each project's details
                        for (DataSnapshot appSnap : snapshot.getChildren()) {
                            String status = appSnap.child("status").getValue(String.class);
                            if ("Accepted".equals(status)) {
                                String projectId = appSnap.child("projectId").getValue(String.class);
                                DatabaseReference projectRef = FirebaseDatabase.getInstance()
                                        .getReference("projects").child(projectId);

                                projectRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        if (snapshot.exists() &&
                                                "completed".equals(snapshot.child("status").getValue(String.class))) {
                                            Project project = snapshot.getValue(Project.class);
                                            if (project != null) {
                                                project.setProjectId(snapshot.getKey());
                                                loadedProjects.add(project);
                                            }
                                        }

                                        loadedCount[0]++;

                                        // Check if all projects have been loaded
                                        if (loadedCount[0] == totalProjects[0]) {
                                            // Update adapter with all loaded projects at once
                                            completedProjectsAdapter.updateProjects(loadedProjects);
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        loadedCount[0]++;
                                        Toast.makeText(ApplicantProfileActivity.this,
                                                "Failed to load project details", Toast.LENGTH_SHORT).show();

                                        // Check if all projects have been attempted to load
                                        if (loadedCount[0] == totalProjects[0]) {
                                            completedProjectsAdapter.updateProjects(loadedProjects);
                                        }
                                    }
                                });
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(ApplicantProfileActivity.this,
                                "Failed to load completed projects", Toast.LENGTH_SHORT).show();
                        // Update adapter with empty list in case of error
                        completedProjectsAdapter.updateProjects(new ArrayList<>());
                    }
                });
    }

    private void loadProjectDetails(String projectId) {
        DatabaseReference projectRef = FirebaseDatabase.getInstance()
                .getReference("projects").child(projectId);

        projectRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && "completed".equals(snapshot.child("status").getValue(String.class))) {
                    Project project = snapshot.getValue(Project.class);
                    if (project != null) {
                        project.setProjectId(snapshot.getKey());
                        completedProjects.add(project);
                        completedProjectsAdapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ApplicantProfileActivity.this,
                        "Failed to load project details", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Add method to view certificate
    private void viewCertificate(String projectId, String studentId) {
        DatabaseReference certificateRef = FirebaseDatabase.getInstance()
                .getReference("certificates")
                .child(projectId)
                .child(studentId);

        certificateRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String certificateUrl = snapshot.child("certificateUrl").getValue(String.class);
                    if (certificateUrl != null && !certificateUrl.isEmpty()) {
                        // Use PdfViewerActivity to display the certificate
                        Intent intent = new Intent(ApplicantProfileActivity.this, PdfViewerActivity.class);
                        intent.putExtra("pdf_url", certificateUrl);
                        startActivity(intent);
                    } else {
                        Toast.makeText(ApplicantProfileActivity.this,
                                "Certificate not available", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(ApplicantProfileActivity.this,
                            "Certificate not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ApplicantProfileActivity.this,
                        "Failed to load certificate", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Applicant Profile");
            getSupportActionBar().setSubtitle("Loading...");
        }
    }

    private void setupRecyclerView() {
        feedbackList = new ArrayList<>();
        feedbackAdapter = new StudentFeedbackAdapter(feedbackList, this);

        LinearLayoutManager feedbackLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rvFeedback.setLayoutManager(feedbackLayoutManager);
        rvFeedback.setAdapter(feedbackAdapter);
    }

    private void validateIntentData() {
        if (applicantId == null || applicantId.isEmpty()) {
            Log.e(TAG, "❌ No applicant ID provided");
            showErrorAndFinish("Invalid applicant data");
            return;
        }
        Log.d(TAG, "✅ Intent data validated");
    }

    private void loadCurrentCompanyInfo() {
        if (currentCompanyId.isEmpty()) {
            currentCompanyName = "Our Company";
            return;
        }

        DatabaseReference companyRef = databaseReference.child("users").child(currentCompanyId);
        companyRef.child("name").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentCompanyName = snapshot.getValue(String.class);
                if (currentCompanyName == null) {
                    currentCompanyName = "Our Company";
                }
                Log.d(TAG, "Company name loaded: " + currentCompanyName);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                currentCompanyName = "Our Company";
                Log.w(TAG, "Failed to load company name: " + error.getMessage());
            }
        });
    }

    private void loadApplicantProfile() {
        Log.d(TAG, "Loading applicant profile for ID: " + applicantId);
        showLoading(true);

        DatabaseReference usersRef = databaseReference.child("users").child(applicantId);
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.hasChildren()) {
                    Log.d(TAG, "✅ Profile found in 'users' node");
                    displayApplicantData(snapshot);
                    loadStudentFeedback();
                } else {
                    Log.d(TAG, "Profile not found in 'users', trying 'students' node");
                    loadFromStudentsNode();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Users node query cancelled: " + error.getMessage());
                loadFromStudentsNode();
            }
        });
    }

    private void loadFromStudentsNode() {
        DatabaseReference studentsRef = databaseReference.child("students").child(applicantId);
        studentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                showLoading(false);

                if (snapshot.exists() && snapshot.hasChildren()) {
                    Log.d(TAG, "✅ Profile found in 'students' node");
                    displayApplicantData(snapshot);
                    loadStudentFeedback();
                } else {
                    Log.e(TAG, "❌ Profile not found in any node");
                    showErrorAndFinish("Applicant profile not found");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showLoading(false);
                Log.e(TAG, "❌ Students node query failed: " + error.getMessage());
                showErrorAndFinish("Failed to load profile: " + error.getMessage());
            }
        });
    }

    private void loadStudentFeedback() {
        databaseReference.child("student_feedback")
                .orderByChild("studentId")
                .equalTo(applicantId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        feedbackList.clear();

                        for (DataSnapshot feedbackSnapshot : snapshot.getChildren()) {
                            StudentFeedback feedback = feedbackSnapshot.getValue(StudentFeedback.class);
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
                        Log.e(TAG, "Failed to load feedback: " + error.getMessage());
                        Toast.makeText(ApplicantProfileActivity.this,
                                "Failed to load reviews: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateFeedbackSummary() {
        if (feedbackList.isEmpty()) {
            feedbackSummary.setVisibility(View.GONE);
            noReviewsLayout.setVisibility(View.VISIBLE);
            rvFeedback.setVisibility(View.GONE);

            // Update header rating to show no reviews
            ratingBarHeader.setRating(0);
            tvRatingHeader.setText("No reviews yet");
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
        ratingBarHeader.setRating(summary.getAverageRating());
        tvRatingHeader.setText(String.format("%.1f (%d reviews)",
                summary.getAverageRating(), summary.getTotalReviews()));
    }

    private FeedbackSummary calculateFeedbackSummary() {
        if (feedbackList.isEmpty()) {
            return new FeedbackSummary(0, 0, 0, 0, 0, 0, 0);
        }

        float totalRating = 0;
        int fiveStar = 0, fourStar = 0, threeStar = 0, twoStar = 0, oneStar = 0;

        for (StudentFeedback feedback : feedbackList) {
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
        if (currentCompanyId.isEmpty()) {
            btnAddFeedback.setVisibility(View.GONE);
            return;
        }

        // Check if company has accepted this student and project is completed
        databaseReference.child("applications")
                .orderByChild("companyId")
                .equalTo(currentCompanyId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        eligibleProjects.clear();
                        long currentTime = System.currentTimeMillis();

                        for (DataSnapshot appSnapshot : snapshot.getChildren()) {
                            String studentId = appSnapshot.child("userId").getValue(String.class);
                            String status = appSnapshot.child("status").getValue(String.class);
                            String projectId = appSnapshot.child("projectId").getValue(String.class);

                            if (applicantId.equals(studentId) && "Accepted".equals(status) && projectId != null) {
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

                                // Check if company already left feedback for this student
                                checkExistingFeedback();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Handle error silently
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
        databaseReference.child("student_feedback")
                .orderByChild("companyId")
                .equalTo(currentCompanyId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean hasExistingFeedback = false;

                        for (DataSnapshot feedbackSnapshot : snapshot.getChildren()) {
                            String feedbackStudentId = feedbackSnapshot.child("studentId").getValue(String.class);
                            if (applicantId.equals(feedbackStudentId)) {
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

    private void displayApplicantData(DataSnapshot snapshot) {
        showLoading(false);
        Log.d(TAG, "Displaying applicant data...");

        // Extract data with professional null handling
        applicantName = getStringValue(snapshot, "name", "Unknown Applicant");
        applicantEmail = getStringValue(snapshot, "email", null);
        String degree = getStringValue(snapshot, "degree", "Not specified");
        String gpa = getStringValue(snapshot, "gpa", "Not available");
        String gradYear = getStringValue(snapshot, "gradyear", "Not specified");
        String bio = getStringValue(snapshot, "bio", "No bio provided");
        String skills = getStringValue(snapshot, "skills", "No skills listed");
        applicantPhone = getStringValue(snapshot, "phone", null);
        String university = getStringValue(snapshot, "university", "Not specified");

        // URLs and media
        cvUrl = snapshot.child("cvUrl").getValue(String.class);
        String profileImageUrl = getProfileImageUrl(snapshot);

        Log.d(TAG, "Profile data loaded:");
        Log.d(TAG, "  → Name: " + applicantName);
        Log.d(TAG, "  → Email: " + (applicantEmail != null ? "Available" : "Not provided"));
        Log.d(TAG, "  → Phone: " + (applicantPhone != null ? "Available" : "Not provided"));
        Log.d(TAG, "  → CV: " + (cvUrl != null ? "Available" : "Not uploaded"));

        // Update UI with professional presentation
        updateUI(applicantName, applicantEmail, degree, gpa, gradYear,
                bio, skills, applicantPhone, university, profileImageUrl);

        setupProfessionalButtonListeners();
        showSuccessMessage();
    }

    private String getStringValue(DataSnapshot snapshot, String key, String defaultValue) {
        String value = snapshot.child(key).getValue(String.class);
        if (value != null && !value.trim().isEmpty()) {
            return value.trim();
        }
        return defaultValue;
    }

    private String getProfileImageUrl(DataSnapshot snapshot) {
        String[] imageKeys = {"profileImageUrl", "logoUrl", "imageUrl", "profilePicture"};

        for (String key : imageKeys) {
            String imageUrl = snapshot.child(key).getValue(String.class);
            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                Log.d(TAG, "Profile image found with key: " + key);
                return imageUrl.trim();
            }
        }

        Log.d(TAG, "No profile image found, using default");
        return null;
    }

    private void updateUI(String name, String email, String degree, String gpa,
                          String gradYear, String bio, String skills, String phone,
                          String university, String profileImageUrl) {

        // Update toolbar with professional title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(name);
            getSupportActionBar().setSubtitle("Applicant Profile");
        }

        // Set text values with proper formatting
        tvName.setText(name);
        tvEmail.setText(email != null ? email : "Email not provided");
        tvDegree.setText(degree);
        tvGPA.setText(gpa);
        tvGradYear.setText(gradYear);
        tvBio.setText(bio);
        tvSkills.setText(skills);
        tvPhone.setText(phone != null ? phone : "Phone not provided");
        tvUniversity.setText(university);

        // Load profile image with professional styling
        loadProfileImage(profileImageUrl);

        // Update button states based on available data
        updateButtonStates(email, phone);
    }

    private void loadProfileImage(String imageUrl) {
        RequestOptions options = new RequestOptions()
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .circleCrop()
                .centerCrop();

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .apply(options)
                    .into(profileImage);
            Log.d(TAG, "✅ Profile image loaded");
        } else {
            profileImage.setImageResource(R.drawable.ic_profile);
            Log.d(TAG, "Using default profile image");
        }
    }

    private void updateButtonStates(String email, String phone) {
        // Update email button
        if (btnContactEmail != null) {
            btnContactEmail.setEnabled(email != null);
            btnContactEmail.setAlpha(email != null ? 1.0f : 0.5f);
        }

        // Update phone button
        if (btnContactPhone != null) {
            btnContactPhone.setEnabled(phone != null);
            btnContactPhone.setAlpha(phone != null ? 1.0f : 0.5f);
        }

        // CV button
        if (btnViewCV != null) {
            btnViewCV.setEnabled(cvUrl != null);
            btnViewCV.setAlpha(cvUrl != null ? 1.0f : 0.5f);
        }

        Log.d(TAG, "Button states updated");
    }

    private void setupProfessionalButtonListeners() {
        Log.d(TAG, "Setting up button listeners...");

        // View CV Button
        if (btnViewCV != null) {
            btnViewCV.setOnClickListener(v -> handleViewCV());
        }

        // Professional Email Button
        if (btnContactEmail != null) {
            btnContactEmail.setOnClickListener(v -> handleProfessionalEmail());
        }

        // Professional Call Button
        if (btnContactPhone != null) {
            btnContactPhone.setOnClickListener(v -> handleProfessionalCall());
        }

        // Message Button
        if (btnMessage != null) {
            btnMessage.setText("Message");
            btnMessage.setOnClickListener(v -> handleOpenChat());
        }

        // Add Feedback Button
        if (btnAddFeedback != null) {
            btnAddFeedback.setOnClickListener(v -> {
                if (canLeaveFeedback) {
                    showAddFeedbackDialog();
                }
            });
        }

        Log.d(TAG, "✅ Button listeners configured");
    }

    private void showAddFeedbackDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_add_student_feedback);
        dialog.getWindow().setLayout(
                (int) (getResources().getDisplayMetrics().widthPixels * 0.9),
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        // Initialize dialog views
        ImageView ivStudentProfileDialog = dialog.findViewById(R.id.iv_student_profile_dialog);
        TextView tvStudentNameDialog = dialog.findViewById(R.id.tv_student_name_dialog);
        TextView tvProjectNameDialog = dialog.findViewById(R.id.tv_project_name_dialog);
        RatingBar ratingBarDialog = dialog.findViewById(R.id.rating_bar_dialog);
        TextView tvRatingDescription = dialog.findViewById(R.id.tv_rating_description);
        TextInputEditText etComment = dialog.findViewById(R.id.et_comment);
        Button btnCancel = dialog.findViewById(R.id.btn_cancel);
        Button btnSubmit = dialog.findViewById(R.id.btn_submit);

        // Set student info
        tvStudentNameDialog.setText(applicantName);
        if (!eligibleProjects.isEmpty()) {
            loadProjectTitle(eligibleProjects.get(0), tvProjectNameDialog);
        }

        // Load student profile image
        Glide.with(this)
                .load(profileImage.getDrawable())
                .into(ivStudentProfileDialog);

        // Rating change listener
        ratingBarDialog.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            if (fromUser) {
                btnSubmit.setEnabled(rating > 0);
                String[] descriptions = {
                        "Tap stars to rate",
                        "Poor Performance",
                        "Below Average",
                        "Average Performance",
                        "Good Performance",
                        "Excellent Performance"
                };
                tvRatingDescription.setText(descriptions[(int) rating]);
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSubmit.setOnClickListener(v -> {
            float rating = ratingBarDialog.getRating();
            String comment = etComment.getText().toString().trim();

            if (rating > 0) {
                submitStudentFeedback(rating, comment, eligibleProjects.get(0));
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

    private void submitStudentFeedback(float rating, String comment, String projectId) {
        showLoading(true);

        // Get current company info
        databaseReference.child("users").child(currentCompanyId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot companySnapshot) {
                        String companyName = companySnapshot.child("name").getValue(String.class);
                        String companyLogoUrl = companySnapshot.child("logoUrl").getValue(String.class);

                        // Get project title
                        databaseReference.child("projects").child(projectId)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot projectSnapshot) {
                                        String projectTitle = projectSnapshot.child("title").getValue(String.class);

                                        // Create feedback object
                                        StudentFeedback feedback = new StudentFeedback(
                                                currentCompanyId,
                                                companyName != null ? companyName : "Company",
                                                applicantId,
                                                projectId,
                                                projectTitle != null ? projectTitle : "Project",
                                                rating,
                                                comment,
                                                System.currentTimeMillis(),
                                                companyLogoUrl
                                        );

                                        // Save to Firebase
                                        DatabaseReference feedbackRef = databaseReference
                                                .child("student_feedback").push();

                                        feedbackRef.setValue(feedback)
                                                .addOnSuccessListener(aVoid -> {
                                                    showLoading(false);
                                                    Toast.makeText(ApplicantProfileActivity.this,
                                                            "Review submitted successfully!", Toast.LENGTH_SHORT).show();

                                                    // Refresh feedback
                                                    loadStudentFeedback();
                                                    btnAddFeedback.setVisibility(View.GONE);
                                                })
                                                .addOnFailureListener(e -> {
                                                    showLoading(false);
                                                    Toast.makeText(ApplicantProfileActivity.this,
                                                            "Failed to submit review: " + e.getMessage(),
                                                            Toast.LENGTH_SHORT).show();
                                                });
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        showLoading(false);
                                        Toast.makeText(ApplicantProfileActivity.this,
                                                "Failed to load project info", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showLoading(false);
                        Toast.makeText(ApplicantProfileActivity.this,
                                "Failed to load company info", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void handleViewCV() {
        Log.d(TAG, "CV view requested");

        if (cvUrl != null && !cvUrl.isEmpty()) {
            try {
                // Use PdfViewerActivity instead of browser intent
                Intent pdfIntent = new Intent(this, PdfViewerActivity.class);
                pdfIntent.putExtra("pdf_url", cvUrl);
                startActivity(pdfIntent);
                Toast.makeText(this, "Opening " + applicantName + "'s CV...", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "✅ CV opened successfully");
            } catch (Exception e) {
                Log.e(TAG, "❌ Failed to open CV", e);
                Toast.makeText(this, "Unable to open CV", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.w(TAG, "CV not available");
            showNoCVDialog();
        }
    }

    private void handleProfessionalEmail() {
        Log.d(TAG, "Professional email contact initiated");

        if (applicantEmail == null || applicantEmail.equals("Email not provided")) {
            Toast.makeText(this, "Email address not available", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:" + applicantEmail));

            String subject = "Internship Opportunity - " + (currentCompanyName != null ? currentCompanyName : "Our Company");
            String body = "Dear " + applicantName + ",\n\n"
                    + "Thank you for your interest in our internship program. "
                    + "We would like to discuss your application further.\n\n"
                    + "Please let us know your availability for a brief call or meeting.\n\n"
                    + "Best regards,\n"
                    + (currentCompanyName != null ? currentCompanyName : "Hiring Team");

            emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
            emailIntent.putExtra(Intent.EXTRA_TEXT, body);

            startActivity(Intent.createChooser(emailIntent, "Send Professional Email"));

            Toast.makeText(this, "Opening email to " + applicantName, Toast.LENGTH_SHORT).show();
            Log.d(TAG, "✅ Professional email intent created");

        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to open email app", e);
            Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleProfessionalCall() {
        Log.d(TAG, "Professional call initiated");

        if (applicantPhone == null || applicantPhone.equals("Phone not provided")) {
            Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Call " + applicantName + "?")
                .setMessage("You're about to call " + applicantName + " at:\n" + applicantPhone)
                .setPositiveButton("Call Now", (dialog, which) -> {
                    try {
                        Intent phoneIntent = new Intent(Intent.ACTION_DIAL);
                        phoneIntent.setData(Uri.parse("tel:" + applicantPhone));
                        startActivity(phoneIntent);

                        Toast.makeText(this, "Calling " + applicantName + "...", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "✅ Call initiated to: " + applicantPhone);

                    } catch (Exception e) {
                        Log.e(TAG, "❌ Failed to initiate call", e);
                        Toast.makeText(this, "Unable to make call", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .setIcon(R.drawable.ic_phone)
                .show();
    }

    private void handleOpenChat() {
        Log.d(TAG, "Opening chat with applicant: " + applicantName);

        if (applicantId == null) {
            Toast.makeText(this, "Unable to start chat", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Intent chatIntent = new Intent(this, ChatActivity.class);
            chatIntent.putExtra("CHAT_WITH_ID", applicantId);
            chatIntent.putExtra("CHAT_WITH_NAME", applicantName);
            chatIntent.putExtra("CHAT_TYPE", "COMPANY_TO_STUDENT");
            chatIntent.putExtra("SOURCE", "ApplicantProfile");

            startActivity(chatIntent);

            Toast.makeText(this, "Opening chat with " + applicantName, Toast.LENGTH_SHORT).show();
            Log.d(TAG, "✅ Chat opened with: " + applicantName);

        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to open chat", e);
            Toast.makeText(this, "Unable to open chat", Toast.LENGTH_SHORT).show();
        }
    }

    private void showNoCVDialog() {
        new AlertDialog.Builder(this)
                .setTitle("CV Not Available")
                .setMessage(applicantName + " hasn't uploaded a CV yet.")
                .setPositiveButton("OK", null)
                .setNeutralButton("Contact Applicant", (dialog, which) -> showContactOptionsDialog())
                .setIcon(R.drawable.ic_report)
                .show();
    }

    private void showContactOptionsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Contact " + applicantName);

        String message = "Choose how you'd like to contact " + applicantName + ":\n\n";

        boolean hasEmail = applicantEmail != null && !applicantEmail.equals("Email not provided");
        boolean hasPhone = applicantPhone != null && !applicantPhone.equals("Phone not provided");

        if (hasEmail) {
            message += "✉ Email: " + applicantEmail + "\n";
        }
        if (hasPhone) {
            message += "📞 Phone: " + applicantPhone + "\n";
        }

        message += "💬 In-app messaging available";

        builder.setMessage(message);

        if (hasEmail) {
            builder.setPositiveButton("Email", (dialog, which) -> handleProfessionalEmail());
        }

        if (hasPhone) {
            builder.setNegativeButton("Call", (dialog, which) -> handleProfessionalCall());
        }

        builder.setNeutralButton("Message", (dialog, which) -> handleOpenChat());
        builder.show();
    }

    private void showSuccessMessage() {
        Toast.makeText(this, "Profile loaded successfully", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "✅ Profile display completed successfully");
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            Log.d(TAG, "Loading state: " + (show ? "ON" : "OFF"));
        }
    }

    private void showErrorAndFinish(String message) {
        Log.e(TAG, "❌ Error: " + message);
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "Navigating back from profile");
        super.onBackPressed();
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }
}