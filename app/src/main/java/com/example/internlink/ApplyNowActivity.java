package com.example.internlink;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ApplyNowActivity extends AppCompatActivity {

    private static final int QUIZ_REQUEST_CODE = 1001;
    private static final int UPLOAD_RESUME_REQUEST_CODE = 1002;

    private TextView titleText, descText, categoryText, educationText, compensationText, amountText;
    private TextView deadlineText, startDateText, durationText, applicantsText, studentsRequiredText, skillsText;
    private LinearLayout amountSection;
    private CardView quizSection;
    private TextView quizTitle, quizInstructions, quizTime, quizScore;
    private Button applyButton;
    private ImageButton btnCompanyProfile; // Add company profile button
    private ImageButton btnBack; // Add back button

    private String projectId;
    private Project currentProject;
    private boolean hasQuiz = false;
    private boolean hasResume = false; // Set based on project requirements
    private String resumeUrl = null;
    private boolean isReapplying = false;
    private ImageButton btnSaveProject;
    private boolean isProjectSaved = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apply_now);

        // Get project ID from intent
        projectId = getIntent().getStringExtra("PROJECT_ID");
        if (projectId == null || projectId.isEmpty()) {
            Toast.makeText(this, "Project ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        loadProjectData(projectId);
        setupClickListeners();
        checkIfProjectIsSaved();
    }

    private void initViews() {
        titleText = findViewById(R.id.project_title);
        descText = findViewById(R.id.project_description);
        categoryText = findViewById(R.id.project_category);
        educationText = findViewById(R.id.project_education);
        compensationText = findViewById(R.id.project_compensation);
        amountText = findViewById(R.id.project_amount);
        amountSection = findViewById(R.id.amount_section);
        deadlineText = findViewById(R.id.project_deadline);
        startDateText = findViewById(R.id.project_start_date);
        durationText = findViewById(R.id.project_duration);
        applicantsText = findViewById(R.id.project_applicants);
        studentsRequiredText = findViewById(R.id.project_students_required);
        skillsText = findViewById(R.id.project_skills);
        btnSaveProject = findViewById(R.id.btn_save_project);

        quizSection = findViewById(R.id.quiz_section);
        quizTitle = findViewById(R.id.quiz_title);
        quizInstructions = findViewById(R.id.quiz_instructions);
        quizTime = findViewById(R.id.quiz_time_limit);
        quizScore = findViewById(R.id.quiz_passing_score);

        applyButton = findViewById(R.id.btn_submit_application);
        btnCompanyProfile = findViewById(R.id.btn_company_profile); // Initialize company profile button
        btnBack = findViewById(R.id.btn_back); // Initialize back button
    }
    private boolean hasProjectEnded() {
        if (currentProject == null) return false;

        long currentTime = System.currentTimeMillis();
        long endDate = currentProject.getDeadline();

        // If endDate is 0 or not set, project hasn't ended
        if (endDate <= 0) return false;

        return currentTime > endDate;
    }
    private void checkIfProjectIsSaved() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        DatabaseReference savedRef = FirebaseDatabase.getInstance()
                .getReference("saved_projects")
                .child(user.getUid())
                .child(projectId);

        savedRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                isProjectSaved = snapshot.exists();
                updateSaveButtonIcon();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ApplyNowActivity", "Error checking saved status", error.toException());
            }
        });
    }

    private void updateSaveButtonIcon() {
        btnSaveProject.setImageResource(isProjectSaved ?
                R.drawable.ic_bookmark_filled :
                R.drawable.ic_bookmark_border);
    }

    private void setupClickListeners() {
        btnSaveProject.setOnClickListener(v -> toggleSaveProject());
        // Back button click listener
        btnBack.setOnClickListener(v -> {
            // Navigate back to StudentHomeActivity
            Intent intent = new Intent(ApplyNowActivity.this, StudentHomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        // Apply button click listener
        applyButton.setOnClickListener(v -> {
            // Check if user is logged in
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                showLoginRequiredDialog();
                return;
            }

            // Check if project is still open
            if (currentProject.getDeadline() < System.currentTimeMillis()) {
                Toast.makeText(this, "This project's deadline has passed", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check if student has already applied
            checkExistingApplication(user.getUid(), true);
        });

        // Company profile button click listener
        btnCompanyProfile.setOnClickListener(v -> {
            if (currentProject != null && currentProject.getCompanyId() != null && !currentProject.getCompanyId().isEmpty()) {
                openCompanyProfile();
            } else {
                Toast.makeText(this, "Company information not available", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void toggleSaveProject() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            showLoginRequiredDialog();
            return;
        }

        DatabaseReference savedRef = FirebaseDatabase.getInstance()
                .getReference("saved_projects")
                .child(user.getUid())
                .child(projectId);

        if (isProjectSaved) {
            // Remove from saved projects
            savedRef.removeValue()
                    .addOnSuccessListener(aVoid -> {
                        isProjectSaved = false;
                        updateSaveButtonIcon();
                        Toast.makeText(ApplyNowActivity.this,
                                "Project removed from saved",
                                Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(ApplyNowActivity.this,
                                    "Failed to remove project",
                                    Toast.LENGTH_SHORT).show());
        } else {
            // Add to saved projects
            Map<String, Object> savedProject = new HashMap<>();
            savedProject.put("projectId", projectId);
            savedProject.put("savedAt", System.currentTimeMillis());
            savedProject.put("title", currentProject.getTitle());
            savedProject.put("companyId", currentProject.getCompanyId());

            savedRef.setValue(savedProject)
                    .addOnSuccessListener(aVoid -> {
                        isProjectSaved = true;
                        updateSaveButtonIcon();
                        Toast.makeText(ApplyNowActivity.this,
                                "Project saved successfully",
                                Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(ApplyNowActivity.this,
                                    "Failed to save project",
                                    Toast.LENGTH_SHORT).show());
        }
    }

    // Override the back button behavior
    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        // Navigate back to StudentHomeActivity instead of just finishing
        Intent intent = new Intent(ApplyNowActivity.this, StudentHomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void openCompanyProfile() {
        Intent intent = new Intent(this, CompanyProfileViewActivity.class);
        intent.putExtra("COMPANY_ID", currentProject.getCompanyId());
        startActivity(intent);
    }

    private void checkExistingApplication(String userId, boolean isFromButtonClick) {
        DatabaseReference appsRef = FirebaseDatabase.getInstance().getReference("applications");

        appsRef.orderByChild("userId").equalTo(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long latestTime = -1;
                String latestStatus = null;

                for (DataSnapshot appSnapshot : snapshot.getChildren()) {
                    Application app = appSnapshot.getValue(Application.class);
                    if (app != null && app.getProjectId().equals(projectId)) {
                        if (app.getAppliedDate() > latestTime) {
                            latestTime = app.getAppliedDate();
                            latestStatus = app.getStatus();
                        }
                    }
                }

                isReapplying = "Rejected".equalsIgnoreCase(latestStatus);
                if (hasProjectEnded()) {
                    applyButton.setText("Deadline Passed");
                    applyButton.setEnabled(false);
                    applyButton.setAlpha(0.5f);
                    if (isFromButtonClick) {
                        Toast.makeText(ApplyNowActivity.this, "This project's deadline has passed", Toast.LENGTH_SHORT).show();
                    }
                    return;
                }

                if (latestStatus != null && !"Rejected".equalsIgnoreCase(latestStatus)) {
                    applyButton.setText("Already Applied");
                    applyButton.setEnabled(false); // ❌ disable permanently
                    applyButton.setAlpha(0.5f);    // optional: fade the button visually
                    if (isFromButtonClick) {
                        Toast.makeText(ApplyNowActivity.this, "You've already applied to this project", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    if (isReapplying) {
                        applyButton.setText("Reapply");
                    } else {
                        applyButton.setText("Apply Now");
                    }

                    applyButton.setEnabled(true);  // ensure it's enabled
                    applyButton.setAlpha(1.0f);

                    if (isFromButtonClick) {
                        startApplicationProcess();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ApplyNowActivity.this, "Error checking applications", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startApplicationProcess() {
        // Step 1: Check if resume is required
        if (hasResume && resumeUrl == null) {
            showResumeUploadDialog();
            return;
        }

        // Step 2: Check if quiz is required
        if (hasQuiz) {
            showQuizConfirmationDialog();
            return;
        }

        // If no requirements, proceed to submit
        submitApplication();
    }

    private void showResumeUploadDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Resume Required")
                .setMessage("This project requires you to upload a resume before applying.")
                .setPositiveButton("Upload Resume", (dialog, which) -> {
                    // Launch resume upload activity
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("application/pdf");
                    startActivityForResult(intent, UPLOAD_RESUME_REQUEST_CODE);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showQuizConfirmationDialog() {
        // Inflate custom layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_quiz_confirmation, null);

        // Customize the dialog appearance
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomAlertDialog)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        // Get views from custom layout
        TextView title = dialogView.findViewById(R.id.dialog_title);
        TextView message = dialogView.findViewById(R.id.dialog_message);
        Button positiveBtn = dialogView.findViewById(R.id.positive_button);
        Button negativeBtn = dialogView.findViewById(R.id.negative_button);

        // Set content
        title.setText("Quiz Required");
        message.setText("To apply for this position, you need to complete a short assessment.\n\n" +
                "• " + currentProject.getQuiz().getQuestions().size() + " questions\n" +
                "• " + currentProject.getQuiz().getTimeLimit() + " minute time limit\n" +
                "• Minimum passing score: " + currentProject.getQuiz().getPassingScore() + "%");

        // Set button actions
        positiveBtn.setOnClickListener(v -> {
            dialog.dismiss();
            launchQuizActivity();
        });

        negativeBtn.setOnClickListener(v -> dialog.dismiss());

        // Show dialog with animation
        dialog.show();

        // Custom window animations
        dialog.getWindow().setWindowAnimations(R.style.DialogAnimation);
    }

    private void launchQuizActivity() {
        applyButton.setEnabled(false);
        Intent quizIntent = new Intent(this, QuizActivity.class);
        quizIntent.putExtra("QUIZ_DATA", currentProject.getQuiz());
        quizIntent.putExtra("PROJECT_ID", projectId);
        startActivityForResult(quizIntent, QUIZ_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == UPLOAD_RESUME_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                resumeUrl = data.getData().toString();
                Toast.makeText(this, "Resume uploaded successfully", Toast.LENGTH_SHORT).show();

                if (hasQuiz) {
                    showQuizConfirmationDialog();
                } else {
                    submitApplication();
                }
            }
        } else if (requestCode == QUIZ_REQUEST_CODE && resultCode == RESULT_OK) {
            applyButton.setEnabled(true);
            if (data != null) {
                int quizGrade = data.getIntExtra("QUIZ_GRADE", 0);
                Log.d("ApplyNowActivity", "Quiz grade received: " + quizGrade); // 🔧 Add debug log
                submitApplicationWithGrade(quizGrade);
            } else {
                Log.e("ApplyNowActivity", "Quiz result data is null!"); // 🔧 Add error log
            }
        }
    }

    private void notifyCompanyOfApplication(String companyId, String studentName, String projectTitle) {
        // Remove companyId from path structure
        DatabaseReference announcementsRef = FirebaseDatabase.getInstance()
                .getReference("announcements_by_role")
                .child("company");

        String announcementId = announcementsRef.push().getKey();
        if (announcementId == null) return;

        long timestamp = System.currentTimeMillis();
        String title = "New Application Received";
        String message = studentName + " has just applied to your project \"" + projectTitle + "\".\n\n[View Applicants]";

        Map<String, Object> announcementData = new HashMap<>();
        announcementData.put("title", title);
        announcementData.put("message", message);
        announcementData.put("timestamp", timestamp);
        announcementData.put("companyId", companyId);  // Company ID stored as a field in the announcement
        announcementData.put("projectId", projectId);
        announcementData.put("type", "application");
        announcementData.put("read", false);

        announcementsRef.child(announcementId).setValue(announcementData);
    }

    private void submitApplicationWithGrade(int quizGrade) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            showLoginRequiredDialog();
            return;
        }

        user.getIdToken(true).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Toast.makeText(this, "Authentication verification failed", Toast.LENGTH_SHORT).show();
                return;
            }

            Application application = new Application(
                    projectId,
                    user.getUid(),
                    currentProject.getCompanyId(),
                    "Pending",
                    System.currentTimeMillis(),
                    resumeUrl,
                    null,
                    quizGrade // ✅ Pass quiz grade here
            );
            application.setReapplication(isReapplying);

            DatabaseReference applicationsRef = FirebaseDatabase.getInstance().getReference("applications");
            String applicationId = applicationsRef.push().getKey();

            if (applicationId == null) {
                Toast.makeText(this, "Error creating application", Toast.LENGTH_SHORT).show();
                return;
            }

            applicationsRef.child(applicationId).setValue(application)
                    .addOnCompleteListener(saveTask -> {
                        if (!saveTask.isSuccessful()) {
                            Toast.makeText(this, "Failed to submit application", Toast.LENGTH_LONG).show();
                            return;
                        }

                        updateApplicantsCount();
                        showApplicationSuccessDialog();
                        // Fetch student's name to use in the announcement
                        FirebaseDatabase.getInstance().getReference("users").child(FirebaseAuth.getInstance().getUid())
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        String studentName = snapshot.child("name").getValue(String.class);
                                        if (studentName != null) {
                                            notifyCompanyOfApplication(currentProject.getCompanyId(), studentName, currentProject.getTitle());
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Log.e("NotifyError", "Failed to fetch student name for notification.");
                                    }
                                });
                    });
        });
    }

    private void submitApplication() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            showLoginRequiredDialog();
            return;
        }

        // Verify user is authenticated
        user.getIdToken(true).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Toast.makeText(ApplyNowActivity.this, "Authentication verification failed", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create application object
            Application application = new Application(
                    projectId,
                    user.getUid(),
                    currentProject.getCompanyId(),
                    "Pending",
                    System.currentTimeMillis(),
                    resumeUrl,
                    null,
                    null
            );
            application.setReapplication(isReapplying);

            // Save to Firebase
            DatabaseReference applicationsRef = FirebaseDatabase.getInstance().getReference("applications");
            String applicationId = applicationsRef.push().getKey();

            if (applicationId == null) {
                Toast.makeText(this, "Error creating application", Toast.LENGTH_SHORT).show();
                return;
            }

            applicationsRef.child(applicationId).setValue(application)
                    .addOnCompleteListener(saveTask -> {
                        if (!saveTask.isSuccessful()) {
                            Exception e = saveTask.getException();
                            String errorMsg = "Failed to submit application";
                            if (e != null) {
                                errorMsg += ": " + e.getMessage();
                                Log.e("FirebaseError", "Submit failed", e);
                            }
                            Toast.makeText(ApplyNowActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                            return;
                        }

                        updateApplicantsCount();
                        showApplicationSuccessDialog();
                        // Fetch student's name to use in the announcement
                        FirebaseDatabase.getInstance().getReference("users").child(FirebaseAuth.getInstance().getUid())
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        String studentName = snapshot.child("name").getValue(String.class);
                                        if (studentName != null) {
                                            notifyCompanyOfApplication(currentProject.getCompanyId(), studentName, currentProject.getTitle());
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Log.e("NotifyError", "Failed to fetch student name for notification.");
                                    }
                                });
                    });
        });
    }

    private void updateApplicantsCount() {
        DatabaseReference projectRef = FirebaseDatabase.getInstance()
                .getReference("projects")
                .child(projectId)
                .child("applicants");

        projectRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                Integer currentApplicants = mutableData.getValue(Integer.class);
                if (currentApplicants == null) {
                    mutableData.setValue(1);
                } else {
                    mutableData.setValue(currentApplicants + 1);
                }
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                if (error != null) {
                    Toast.makeText(ApplyNowActivity.this, "Error updating applicants count", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showApplicationSuccessDialog() {
        // Inflate custom layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_application_success, null);

        // Customize the dialog appearance
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.SuccessAlertDialog)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        // Get views from custom layout
        ImageView icon = dialogView.findViewById(R.id.success_icon);
        TextView title = dialogView.findViewById(R.id.dialog_title);
        TextView message = dialogView.findViewById(R.id.dialog_message);
        Button secondaryBtn = dialogView.findViewById(R.id.secondary_button);

        // Set content
        title.setText("Application Submitted!");
        message.setText("Congratulations!\nYour application has been successfully submitted.\n\nYou can track its status in the 'My Applications' section.");


        secondaryBtn.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(this, StudentHomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        // Show dialog with animation
        dialog.show();

        // Custom window animations
        dialog.getWindow().setWindowAnimations(R.style.DialogAnimation);
    }

    private void showLoginRequiredDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Login Required")
                .setMessage("You need to be logged in to apply for projects.")
                .setPositiveButton("Login", (dialog, which) -> {
                    // Launch login activity
                    // Intent loginIntent = new Intent(this, LoginActivity.class);
                    // startActivity(loginIntent);
                    Toast.makeText(this, "Redirect to login screen", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadProjectData(String projectId) {
        DatabaseReference projectRef = FirebaseDatabase.getInstance().getReference("projects").child(projectId);

        projectRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentProject = snapshot.getValue(Project.class);
                if (currentProject == null) {
                    Toast.makeText(ApplyNowActivity.this, "Project not found", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                // Populate UI with project info
                titleText.setText(currentProject.getTitle());
                descText.setText(currentProject.getDescription());
                categoryText.setText(currentProject.getCategory());
                educationText.setText(currentProject.getEducationLevel());
                compensationText.setText(currentProject.getCompensationType());
                deadlineText.setText(formatDate(currentProject.getDeadline()));
                startDateText.setText(formatDate(currentProject.getStartDate()));
                durationText.setText(currentProject.getDuration());
                applicantsText.setText(String.valueOf(currentProject.getApplicants()));
                studentsRequiredText.setText(String.valueOf(currentProject.getStudentsRequired()));

                if (currentProject.getSkills() != null && !currentProject.getSkills().isEmpty()) {
                    skillsText.setText(String.join(", ", currentProject.getSkills()));
                } else {
                    skillsText.setText("No specific skills listed");
                }

                if ("Paid".equalsIgnoreCase(currentProject.getCompensationType())) {
                    amountSection.setVisibility(View.VISIBLE);
                    amountText.setText(String.valueOf(currentProject.getAmount()));
                }

                // ✅ FIXED: Custom quiz conversion
                DataSnapshot quizSnapshot = snapshot.child("quiz");
                if (quizSnapshot.exists()) {
                    try {
                        Map<String, Object> quizData = (Map<String, Object>) quizSnapshot.getValue();
                        if (quizData != null) {
                            Quiz quiz = Quiz.fromFirebaseData(quizData);
                            currentProject.setQuiz(quiz); // Make sure you have this setter in your Project class

                            quizSection.setVisibility(View.VISIBLE);
                            quizTitle.setText(quiz.getTitle());
                            quizInstructions.setText(quiz.getInstructions());
                            quizTime.setText(quiz.getTimeLimit() + " mins");
                            quizScore.setText(quiz.getPassingScore() + "%");
                            hasQuiz = true;

                            Log.d("ApplyNowActivity", "Quiz loaded successfully with " +
                                    quiz.getQuestions().size() + " questions");
                        }
                    } catch (Exception e) {
                        Log.e("ApplyNowActivity", "Error loading quiz data", e);
                        // If quiz loading fails, continue without quiz
                        quizSection.setVisibility(View.GONE);
                        hasQuiz = false;
                    }
                } else {
                    // No quiz for this project
                    quizSection.setVisibility(View.GONE);
                    hasQuiz = false;
                }

                // Enable/disable company profile button based on company ID availability
                if (currentProject.getCompanyId() != null && !currentProject.getCompanyId().isEmpty()) {
                    btnCompanyProfile.setEnabled(true);
                    btnCompanyProfile.setAlpha(1.0f);
                } else {
                    btnCompanyProfile.setEnabled(false);
                    btnCompanyProfile.setAlpha(0.5f);
                }

                // 🔍 Now check if user already applied and update button
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) {
                    checkExistingApplication(user.getUid(), false); // false = not from button click
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ApplyNowActivity.this, "Error loading project", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private String formatDate(long timestamp) {
        if (timestamp == 0) return "N/A";
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date(timestamp));
    }
}