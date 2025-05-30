package com.example.internlink;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

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

    // Data
    private DatabaseReference databaseReference;
    private String companyId;
    private String currentUserId;
    private ProjectAdapterHome projectAdapter;
    private List<Project> projectsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_company_profile_view);

        // Now this will work because we added android:id="@+id/main" to the root LinearLayout
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
        setupRecyclerView();
        loadCompanyData();
        setupClickListeners();
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
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        projectsList = new ArrayList<>();
        projectAdapter = new ProjectAdapterHome(projectsList, project -> {
            // Navigate to project details
            Intent intent = new Intent(this, ProjectDetailsActivity.class);
            intent.putExtra("PROJECT_ID", project.getProjectId());
            startActivity(intent);
        }, false); // false for company profile view

        rvRecentProjects.setLayoutManager(new LinearLayoutManager(this));
        rvRecentProjects.setAdapter(projectAdapter);
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
                            // Load approved applicants count
                            loadApprovedApplicantsCount();
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

        // Rating (you can calculate this based on reviews or set a default)
        ratingBar.setRating(4.5f);
        tvRating.setText("4.5 (128 reviews)");

        // Company Information - Get from database
        String address = snapshot.child("address").getValue(String.class);
        String location = snapshot.child("location").getValue(String.class);

        // Location - use address if available, otherwise use location
        String displayLocation = "";
        if (address != null && !address.isEmpty()) {
            displayLocation = address;
        } else if (location != null && !location.isEmpty()) {
            displayLocation = location;
        } else {
            displayLocation = "Location not specified";
        }
        tvLocation.setText(displayLocation);

        // Work Type - based on location field from database
        String workType = getWorkTypeFromDatabase(location);
        tvWorkType.setText(workType);

        // Company Size will be set in loadApprovedApplicantsCount() method
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

        // Website
        if (website != null && !website.isEmpty()) {
            websiteLayout.setVisibility(View.VISIBLE);
            tvWebsite.setText(website);
        } else {
            websiteLayout.setVisibility(View.GONE);
        }

        // Social Media setup
        setupSocialMedia(snapshot);
    }

    private void loadApprovedApplicantsCount() {
        // Query applications where companyId matches and status is "Accepted"
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

                        // Update the company size text with approved applicants count
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
                        // Set fallback text on error
                        tvCompanySize.setText("Trainees Count Unavailable");
                    }
                });
    }

    private String getWorkTypeFromDatabase(String location) {
        if (location == null || location.isEmpty()) {
            return "Work Type Not Specified";
        }

        // Convert to lowercase for consistent comparison
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
                // If it's a specific location like "beirut", "lebanon", etc.
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
        // Get current user's applied projects to filter them out
        if (currentUserId.isEmpty()) {
            loadProjectsWithoutFiltering();
            return;
        }

        DatabaseReference appliedRef = databaseReference.child("users")
                .child(currentUserId).child("appliedProjects");

        appliedRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot appliedSnapshot) {
                List<String> appliedProjectIds = new ArrayList<>();
                for (DataSnapshot snap : appliedSnapshot.getChildren()) {
                    appliedProjectIds.add(snap.getKey());
                }

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

                            // Only show projects from this company
                            if (!companyId.equals(project.getCompanyId())) continue;

                            // Only show approved projects
                            if (!"approved".equals(project.getStatus())) continue;

                            int applicants = project.getApplicants();
                            int needed = project.getStudentsRequired();
                            long startDate = project.getStartDate();

                            boolean hasStarted = startDate <= currentTime;
                            boolean isFull = applicants >= needed;
                            boolean alreadyApplied = appliedProjectIds.contains(projectId);

                            // For company profile, show all projects regardless of application status
                            // but still filter out started and full projects
                            if (hasStarted || isFull) continue;

                            project.setProjectId(projectId);
                            projectsList.add(project);
                        }

                        // Limit to max 5 projects for company profile
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

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showLoading(false);
                Toast.makeText(CompanyProfileViewActivity.this,
                        "Failed to load applied projects: " + error.getMessage(), Toast.LENGTH_SHORT).show();
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

                    // Only show projects from this company
                    if (!companyId.equals(project.getCompanyId())) continue;

                    // Only show approved projects
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

                // Limit to max 5 projects
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

        // LinkedIn
        if (linkedin != null && !linkedin.isEmpty()) {
            btnLinkedin.setOnClickListener(v -> openUrl(linkedin));
            btnLinkedin.setAlpha(1.0f);
            btnLinkedin.setEnabled(true);
        } else {
            btnLinkedin.setAlpha(0.5f);
            btnLinkedin.setEnabled(false);
        }

        // Twitter
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
    }

    private String getWorkType(String location) {
        if (location == null) return "Remote / In Office";

        switch (location.toLowerCase()) {
            case "remote":
                return "Remote";
            case "in office":
                return "In Office";
            case "office (lebanon)":
                return "In Office • Lebanon";
            default:
                return "Remote / In Office";
        }
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