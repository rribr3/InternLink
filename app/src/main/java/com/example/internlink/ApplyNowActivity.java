package com.example.internlink;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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

public class ApplyNowActivity extends AppCompatActivity {

    private static final int QUIZ_REQUEST_CODE = 1001;
    private static final int UPLOAD_RESUME_REQUEST_CODE = 1002;

    private TextView titleText, descText, categoryText, educationText, compensationText, amountText;
    private TextView deadlineText, startDateText, durationText, applicantsText, studentsRequiredText, skillsText;
    private LinearLayout amountSection;
    private CardView quizSection;
    private TextView quizTitle, quizInstructions, quizTime, quizScore;
    private Button applyButton;

    private String projectId;
    private Project currentProject;
    private boolean hasQuiz = false;
    private boolean hasResume = false; // Set based on project requirements
    private String resumeUrl = null;

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
        setupApplyButton();
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

        quizSection = findViewById(R.id.quiz_section);
        quizTitle = findViewById(R.id.quiz_title);
        quizInstructions = findViewById(R.id.quiz_instructions);
        quizTime = findViewById(R.id.quiz_time_limit);
        quizScore = findViewById(R.id.quiz_passing_score);

        applyButton = findViewById(R.id.btn_submit_application);
    }

    private void setupApplyButton() {
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
            checkExistingApplication(user.getUid());
        });
    }

    private void checkExistingApplication(String userId) {
        DatabaseReference appsRef = FirebaseDatabase.getInstance().getReference("applications");
        appsRef.orderByChild("userId").equalTo(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean hasApplied = false;
                for (DataSnapshot appSnapshot : snapshot.getChildren()) {
                    Application app = appSnapshot.getValue(Application.class);
                    if (app != null && app.getProjectId().equals(projectId)) {
                        hasApplied = true;
                        break;
                    }
                }

                if (hasApplied) {
                    Toast.makeText(ApplyNowActivity.this, "You've already applied to this project", Toast.LENGTH_SHORT).show();
                } else {
                    startApplicationProcess();
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
                // Handle the uploaded resume file
                // You would typically upload this to Firebase Storage and get a URL
                // For simplicity, we'll just store the URI
                resumeUrl = data.getData().toString();
                Toast.makeText(this, "Resume uploaded successfully", Toast.LENGTH_SHORT).show();

                // After resume upload, check if quiz is needed
                if (hasQuiz) {
                    showQuizConfirmationDialog();
                } else {
                    submitApplication();
                }
            }
        } else if (requestCode == QUIZ_REQUEST_CODE && resultCode == RESULT_OK) {
            applyButton.setEnabled(true);
            if (data != null) {
                int quizGrade = data.getIntExtra("QUIZ_GRADE", 0); // 🎯 Retrieve grade from QuizActivity
                submitApplicationWithGrade(quizGrade); // Proceed regardless of quiz outcome

            }

        }
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
        Button primaryBtn = dialogView.findViewById(R.id.primary_button);
        Button secondaryBtn = dialogView.findViewById(R.id.secondary_button);

        // Set content
        title.setText("Application Submitted!");
        message.setText("Congratulations!\nYour application has been successfully submitted.\n\nYou can track its status in the 'My Applications' section.");

        // Set button actions
        primaryBtn.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(this, StudentHomeActivity.class);
            startActivity(intent);
            finish();
        });

        secondaryBtn.setOnClickListener(v -> {
            dialog.dismiss();
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

                // Show amount if Paid
                if ("Paid".equalsIgnoreCase(currentProject.getCompensationType())) {
                    amountSection.setVisibility(View.VISIBLE);
                    amountText.setText(String.valueOf(currentProject.getAmount()));
                }

                if (currentProject.getQuiz() != null) {
                    quizSection.setVisibility(View.VISIBLE);
                    Quiz quiz = currentProject.getQuiz();
                    quizTitle.setText(quiz.getTitle());
                    quizInstructions.setText(quiz.getInstructions());
                    quizTime.setText(quiz.getTimeLimit() + " mins");
                    quizScore.setText(quiz.getPassingScore() + "%");

                    // Set this flag so quiz logic works
                    hasQuiz = true;
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