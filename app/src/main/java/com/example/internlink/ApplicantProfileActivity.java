package com.example.internlink;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

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

    // Data variables
    private String applicantId;
    private String cvUrl;
    private String applicantEmail;
    private String applicantPhone;
    private String applicantName;
    private String currentCompanyName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // === PROFESSIONAL DEBUGGING ===
        Log.d(TAG, "=== ApplicantProfileActivity Started ===");
        Log.d(TAG, "Activity: " + this.getClass().getSimpleName());

        // Check intent extras with detailed logging
        Intent receivedIntent = getIntent();
        if (receivedIntent != null) {
            applicantId = receivedIntent.getStringExtra("APPLICANT_ID");
            applicantName = receivedIntent.getStringExtra("APPLICANT_NAME");
            String debugSource = receivedIntent.getStringExtra("DEBUG_SOURCE");

            Log.d(TAG, "Intent Data:");
            Log.d(TAG, "  → Applicant ID: " + applicantId);
            Log.d(TAG, "  → Applicant Name: " + applicantName);
            Log.d(TAG, "  → Source: " + debugSource);

            // Professional confirmation toast
            if (applicantName != null) {
                Toast.makeText(this, "Loading " + applicantName + "'s profile...",
                        Toast.LENGTH_SHORT).show();
            }
        }

        setContentView(R.layout.activity_applicant_profile);

        initializeViews();
        setupToolbar();
        validateIntentData();
        loadCurrentCompanyInfo();
        loadApplicantProfile();
    }

    private void initializeViews() {
        Log.d(TAG, "Initializing views...");

        // Toolbar
        toolbar = findViewById(R.id.toolbar);

        // Progress bar
        progressBar = findViewById(R.id.progress_bar);

        // Profile image
        profileImage = findViewById(R.id.profile_image);

        // TextViews
        tvName = findViewById(R.id.tv_name);
        tvEmail = findViewById(R.id.tv_email);
        tvDegree = findViewById(R.id.tv_degree);
        tvGPA = findViewById(R.id.tv_gpa);
        tvGradYear = findViewById(R.id.tv_gradyear);
        tvBio = findViewById(R.id.tv_bio);
        tvSkills = findViewById(R.id.tv_skills);
        tvPhone = findViewById(R.id.tv_phone);
        tvUniversity = findViewById(R.id.tv_university);

        // Buttons
        btnViewCV = findViewById(R.id.btn_view_cv);
        btnContactEmail = findViewById(R.id.btn_contact_email);
        btnContactPhone = findViewById(R.id.btn_contact_phone);
        btnMessage = findViewById(R.id.btn_contact); // Changed from Contact to Message

        // Cards
        personalInfoCard = findViewById(R.id.personal_info_card);
        academicInfoCard = findViewById(R.id.academic_info_card);
        contactInfoCard = findViewById(R.id.contact_info_card);

        // Verify critical views
        if (toolbar == null) Log.e(TAG, "❌ Toolbar not found!");
        if (progressBar == null) Log.e(TAG, "❌ Progress bar not found!");
        if (profileImage == null) Log.e(TAG, "❌ Profile image not found!");

        Log.d(TAG, "✅ Views initialized successfully");
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

    private void validateIntentData() {
        if (applicantId == null || applicantId.isEmpty()) {
            Log.e(TAG, "❌ No applicant ID provided");
            showErrorAndFinish("Invalid applicant data");
            return;
        }
        Log.d(TAG, "✅ Intent data validated");
    }

    private void loadCurrentCompanyInfo() {
        String companyId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference companyRef = FirebaseDatabase.getInstance()
                .getReference("users").child(companyId);

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

        // Try users node first (most common)
        DatabaseReference usersRef = FirebaseDatabase.getInstance()
                .getReference("users").child(applicantId);

        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.hasChildren()) {
                    Log.d(TAG, "✅ Profile found in 'users' node");
                    displayApplicantData(snapshot);
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
        DatabaseReference studentsRef = FirebaseDatabase.getInstance()
                .getReference("students").child(applicantId);

        studentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                showLoading(false);

                if (snapshot.exists() && snapshot.hasChildren()) {
                    Log.d(TAG, "✅ Profile found in 'students' node");
                    displayApplicantData(snapshot);
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

        // Log loaded data
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
        // Try multiple possible keys for profile image
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

        // ✅ PROFESSIONAL EMAIL BUTTON - Opens email app with pre-filled content
        if (btnContactEmail != null) {
            btnContactEmail.setOnClickListener(v -> handleProfessionalEmail());
        }

        // ✅ PROFESSIONAL CALL BUTTON - Opens phone dialer
        if (btnContactPhone != null) {
            btnContactPhone.setOnClickListener(v -> handleProfessionalCall());
        }

        // ✅ MESSAGE BUTTON - Opens chat between company and student
        if (btnMessage != null) {
            btnMessage.setText("Message"); // Change button text
            btnMessage.setOnClickListener(v -> handleOpenChat());
        }

        Log.d(TAG, "✅ Button listeners configured");
    }

    private void handleViewCV() {
        Log.d(TAG, "CV view requested");

        if (cvUrl != null && !cvUrl.isEmpty()) {
            try {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(cvUrl));
                startActivity(browserIntent);
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

    // ✅ PROFESSIONAL EMAIL HANDLER
    private void handleProfessionalEmail() {
        Log.d(TAG, "Professional email contact initiated");

        if (applicantEmail == null || applicantEmail.equals("Email not provided")) {
            Toast.makeText(this, "Email address not available", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:" + applicantEmail));

            // Professional email template
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

    // ✅ PROFESSIONAL CALL HANDLER
    private void handleProfessionalCall() {
        Log.d(TAG, "Professional call initiated");

        if (applicantPhone == null || applicantPhone.equals("Phone not provided")) {
            Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show confirmation dialog for professional calling
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

    // ✅ CHAT MESSAGE HANDLER - Opens chat between company and student
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