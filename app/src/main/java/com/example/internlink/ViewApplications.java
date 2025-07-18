package com.example.internlink;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.annotation.Nullable;

public class ViewApplications extends AppCompatActivity {

    private ImageView ivCompanyLogo;
    private TextView tvProjectTitle, tvCompanyName, tvProjectDescription,
            tvCategory, tvSkillsRequired, tvLocation, tvDuration,
            tvPaymentInfo, tvPostedDate, tvContact,
            tvStatus, tvSubmissionDate, tvQuizScore, tvCompanyMessage;
    private Button btnWithdraw, btnView, btnReapply;
    private ImageButton btnBack; // Added btnBack
    private ImageView btnChat;
    private ImageButton btnCompanyProfile;

    private String applicationId;
    private TextView tvReapplicationLabel;
    private String projectId, companyId;
    private int quizGrade = -1;
    private boolean hasQuiz = false;
    private String resumeUrl = null;
    private Project currentProject;
    private boolean hasResume = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_applications);

        applicationId = getIntent().getStringExtra("APPLICATION_ID");

        if (applicationId == null || applicationId.isEmpty()) {
            Toast.makeText(this, "Application ID not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        ivCompanyLogo = findViewById(R.id.iv_company_logo);
        tvProjectTitle = findViewById(R.id.tv_project_title);
        tvCompanyName = findViewById(R.id.tv_company_name);
        tvProjectDescription = findViewById(R.id.tv_project_description);
        tvCategory = findViewById(R.id.tv_category);
        tvSkillsRequired = findViewById(R.id.tv_skills_required);
        tvLocation = findViewById(R.id.tv_location);
        tvDuration = findViewById(R.id.tv_duration);
        tvPaymentInfo = findViewById(R.id.tv_payment_info);
        tvPostedDate = findViewById(R.id.tv_posted_date);
        tvContact = findViewById(R.id.tv_contact);
        btnCompanyProfile = findViewById(R.id.btn_company_profile);

        tvStatus = findViewById(R.id.tv_status);
        tvSubmissionDate = findViewById(R.id.tv_submission_date);
        tvQuizScore = findViewById(R.id.tv_quiz_score);
        tvReapplicationLabel = findViewById(R.id.tv_reapplication_label);

        btnWithdraw = findViewById(R.id.btn_withdraw);
        btnView = findViewById(R.id.btn_view);
        btnReapply = findViewById(R.id.btn_reapply);
        btnBack = findViewById(R.id.btn_back); // Initialize back button

        // Set up back button click listener
        btnBack.setOnClickListener(v -> finish());
        btnCompanyProfile.setOnClickListener(v -> {
            if (currentProject != null && currentProject.getCompanyId() != null && !currentProject.getCompanyId().isEmpty()) {
                openCompanyProfile();
            } else {
                Toast.makeText(this, "Company information not available", Toast.LENGTH_SHORT).show();
            }
        });

        loadApplicationData();

        btnWithdraw.setOnClickListener(v -> {
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_withdraw_confirm, null);

            AlertDialog dialog = new AlertDialog.Builder(ViewApplications.this, R.style.CustomAlertDialog)
                    .setView(dialogView)
                    .setCancelable(true)
                    .create();

            // Get views from the custom dialog
            TextView dialogTitle = dialogView.findViewById(R.id.dialog_title);
            TextView dialogMessage = dialogView.findViewById(R.id.dialog_message);
            Button negativeButton = dialogView.findViewById(R.id.negative_button);
            Button positiveButton = dialogView.findViewById(R.id.positive_button);

            // Set content
            dialogTitle.setText("Withdraw Application");
            dialogMessage.setText("Are you sure you want to withdraw this application?");

            // Handle buttons
            negativeButton.setOnClickListener(cancel -> dialog.dismiss());

            positiveButton.setOnClickListener(confirm -> {
                FirebaseDatabase.getInstance().getReference("applications")
                        .child(applicationId)
                        .removeValue()
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(ViewApplications.this, "Application withdrawn successfully", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            finish(); // Exit the screen
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(ViewApplications.this, "Failed to withdraw application", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        });
            });

            // Show the dialog
            dialog.show();
        });

        btnReapply.setOnClickListener(v -> {
            if (hasResume && resumeUrl == null) {
                uploadResume();
            } else if (hasQuiz) {
                startQuiz();
            } else {
                submitReapplication(null); // no resume, no quiz
            }
        });
    }
    private void openCompanyProfile() {
        Intent intent = new Intent(this, CompanyProfileViewActivity.class);
        intent.putExtra("COMPANY_ID", currentProject.getCompanyId());
        startActivity(intent);
    }

    private void uploadResume() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        startActivityForResult(intent, 1002);
    }

    private void showQuizDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_quiz_confirmation, null);
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomAlertDialog)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        TextView title = dialogView.findViewById(R.id.dialog_title);
        TextView message = dialogView.findViewById(R.id.dialog_message);
        Button positiveBtn = dialogView.findViewById(R.id.positive_button);
        Button negativeBtn = dialogView.findViewById(R.id.negative_button);

        title.setText("Quiz Required");
        message.setText("To reapply, you need to complete the assessment.\n\n" +
                "• " + currentProject.getQuiz().getQuestions().size() + " questions\n" +
                "• " + currentProject.getQuiz().getTimeLimit() + " min limit\n" +
                "• Passing: " + currentProject.getQuiz().getPassingScore() + "%");

        positiveBtn.setOnClickListener(v -> {
            dialog.dismiss();
            launchQuizForReapplication();
        });

        negativeBtn.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void launchQuizForReapplication() {
        Intent quizIntent = new Intent(this, QuizActivity.class);
        quizIntent.putExtra("QUIZ_DATA", currentProject.getQuiz());
        quizIntent.putExtra("PROJECT_ID", projectId);
        startActivityForResult(quizIntent, 1011); // custom request code
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1002 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            resumeUrl = data.getData().toString(); // You should upload to Firebase Storage ideally
            Toast.makeText(this, "Resume uploaded. Proceeding...", Toast.LENGTH_SHORT).show();

            if (hasQuiz) {
                startQuiz();
            } else {
                submitReapplication(null);
            }
        } else if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            quizGrade = data.getIntExtra("QUIZ_GRADE", 0);
            submitReapplication(quizGrade);
        }
    }

    private void startQuiz() {
        Intent intent = new Intent(this, QuizActivity.class);
        intent.putExtra("QUIZ_DATA", currentProject.getQuiz());
        intent.putExtra("PROJECT_ID", projectId);
        startActivityForResult(intent, 1001);
    }

    private void submitReapplication(@Nullable Integer quizGrade) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        // Create the new application
        Application application = new Application(
                projectId,
                user.getUid(),
                companyId,
                "Pending",
                System.currentTimeMillis(),
                resumeUrl,
                null,
                quizGrade
        );
        application.setReapplication(true);
        application.setParentApplicationId(applicationId); // current is parent

        DatabaseReference appRef = FirebaseDatabase.getInstance().getReference("applications");
        String newId = appRef.push().getKey();
        if (newId == null) return;

        // First delete the original rejected application
        appRef.child(applicationId).removeValue().addOnCompleteListener(deleteTask -> {
            if (deleteTask.isSuccessful()) {
                // Now save the reapplication
                appRef.child(newId).setValue(application).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Reapplied successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, "Failed to reapply", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(this, "Failed to reapply", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadApplicationData() {
        DatabaseReference appRef = FirebaseDatabase.getInstance().getReference("applications").child(applicationId);
        appRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(ViewApplications.this, "Application not found", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                btnChat = findViewById(R.id.btn_chat);

                String interviewType = snapshot.child("interviewType").getValue(String.class);
                String interviewDate = snapshot.child("interviewDate").getValue(String.class);
                String interviewTime = snapshot.child("interviewTime").getValue(String.class);

                if ("Online".equalsIgnoreCase(interviewType)) {
                    // Combine date + time and parse
                    try {
                        String dateTimeString = interviewDate + " " + interviewTime;
                        SimpleDateFormat format = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
                        Date interviewDateTime = format.parse(dateTimeString);

                        // Compare with current time
                        if (interviewDateTime != null && interviewDateTime.after(new Date())) {
                            btnChat.setEnabled(false);
                            btnChat.setAlpha(0.5f); // make it visually disabled
                        } else {
                            btnChat.setEnabled(true);
                            btnChat.setAlpha(1.0f);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        btnChat.setEnabled(false); // fail safe
                    }
                } else {
                    // In-person interviews: always allow chat
                    btnChat.setEnabled(true);
                    btnChat.setAlpha(1.0f);
                }

                Boolean isReapplication = snapshot.child("reapplication").getValue(Boolean.class);
                String parentId = snapshot.child("parentApplicationId").getValue(String.class);
                String status = snapshot.child("status").getValue(String.class); // ✅ Use current app's own status

                if (Boolean.TRUE.equals(isReapplication)) {
                    tvReapplicationLabel.setVisibility(View.VISIBLE);
                    tvReapplicationLabel.setPaintFlags(tvReapplicationLabel.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
                } else {
                    tvReapplicationLabel.setVisibility(View.GONE);
                }

                projectId = snapshot.child("projectId").getValue(String.class);
                companyId = snapshot.child("companyId").getValue(String.class);
                Long appliedDate = snapshot.child("appliedDate").getValue(Long.class);
                Integer quizGrade = snapshot.child("quizGrade").getValue(Integer.class) != null ?
                        snapshot.child("quizGrade").getValue(Integer.class) : -1;

                tvSubmissionDate.setText("Submitted on: " + formatDate(appliedDate));
                tvQuizScore.setText("Quiz Score: " + (quizGrade != null ? quizGrade + "/100" : "N/A"));

                setStatusAndButtons(status); // always use current status
                loadProjectAndCompany(projectId, companyId);

                if ("Pending".equalsIgnoreCase(status) || "Rejected".equalsIgnoreCase(status)) {
                    btnChat.setEnabled(false);
                    btnChat.setAlpha(0.5f);
                } else {
                    btnChat.setOnClickListener(v -> {
                        Intent intent = new Intent(ViewApplications.this, StudentChatActivity.class);
                        intent.putExtra("COMPANY_ID", companyId);
                        intent.putExtra("COMPANY_NAME", tvCompanyName.getText().toString());
                        intent.putExtra("PROJECT_ID", projectId);
                        intent.putExtra("PROJECT_TITLE", tvProjectTitle.getText().toString());
                        intent.putExtra("APPLICATION_ID", applicationId);
                        startActivity(intent);
                    });
                }

                btnView.setOnClickListener(v -> showInterviewPopup());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ViewApplications.this, "Error loading application", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showInterviewPopup() {
        DatabaseReference appRef = FirebaseDatabase.getInstance().getReference("applications").child(applicationId);
        appRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("MissingInflatedId")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                String type = snapshot.child("interviewType").getValue(String.class);
                String date = snapshot.child("interviewDate").getValue(String.class);
                String time = snapshot.child("interviewTime").getValue(String.class);
                String method = snapshot.child("interviewMethod").getValue(String.class);
                String location = snapshot.child("interviewLocation").getValue(String.class);
                String zoom = snapshot.child("zoomLink").getValue(String.class);

                View view = getLayoutInflater().inflate(R.layout.dialog_interview_details, null);
                AlertDialog dialog = new AlertDialog.Builder(ViewApplications.this)
                        .setView(view)
                        .setCancelable(true)
                        .create();

                TextView tvType = view.findViewById(R.id.tv_interview_type);
                TextView tvDate = view.findViewById(R.id.tv_interview_date);
                TextView tvTime = view.findViewById(R.id.tv_interview_time);
                TextView tvMethod = view.findViewById(R.id.tv_interview_method);
                TextView tvLocation = view.findViewById(R.id.tv_interview_location);
                TextView tvZoom = view.findViewById(R.id.tv_zoom_link);

                tvType.setText("Type: " + (type != null ? type : "N/A"));
                tvDate.setText("Date: " + (date != null ? date : "N/A"));
                tvTime.setText("Time: " + (time != null ? time : "N/A"));
                tvMethod.setText("Method: " + (method != null ? method : "N/A"));

                if ("Zoom".equalsIgnoreCase(method) || "Chat".equalsIgnoreCase(method)) {
                    tvLocation.setVisibility(View.GONE);
                } else {
                    tvLocation.setText("Location: " + (location != null ? location : "N/A"));
                    tvLocation.setVisibility(View.VISIBLE);
                }

                // Hide Zoom for "In-person" and "Chat"
                if ("In-person".equalsIgnoreCase(type) || "Chat".equalsIgnoreCase(method)) {
                    tvZoom.setVisibility(View.GONE);
                } else {
                    tvZoom.setText("Zoom: " + (zoom != null ? zoom : "N/A"));
                    tvZoom.setVisibility(View.VISIBLE);
                }

                if ("In-person".equalsIgnoreCase(type)) {
                    tvMethod.setVisibility(View.GONE);
                } else {
                    tvMethod.setText("Method: " + (method != null ? method : "N/A"));
                    tvMethod.setVisibility(View.VISIBLE);
                }
                Button btnAction = view.findViewById(R.id.btn_action);
                Button btnClose = view.findViewById(R.id.btn_close);

                // Set action button text based on method
                if ("Zoom".equalsIgnoreCase(method)) {
                    btnAction.setText("Join");
                    btnAction.setOnClickListener(v -> {
                        if (zoom != null && !zoom.isEmpty()) {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setData(android.net.Uri.parse(zoom));
                            startActivity(intent);
                        } else {
                            Toast.makeText(ViewApplications.this, "No Zoom link available", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else if ("Chat".equalsIgnoreCase(method)) {
                    btnAction.setText("Message");
                    btnAction.setOnClickListener(v -> {
                        Intent intent = new Intent(ViewApplications.this, StudentChatActivity.class);
                        intent.putExtra("COMPANY_ID", companyId);
                        intent.putExtra("COMPANY_NAME", tvCompanyName.getText().toString());
                        intent.putExtra("PROJECT_ID", projectId);
                        intent.putExtra("PROJECT_TITLE", tvProjectTitle.getText().toString());
                        intent.putExtra("APPLICATION_ID", applicationId);
                        startActivity(intent);
                    });
                } else if ("In-person".equalsIgnoreCase(type)) {
                    btnAction.setText("Find Location");
                    btnAction.setOnClickListener(v -> {
                        if (location != null && !location.isEmpty()) {
                            Uri mapIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(location));
                            Intent mapIntent = new Intent(Intent.ACTION_VIEW, mapIntentUri);
                            mapIntent.setPackage("com.google.android.apps.maps");
                            startActivity(mapIntent);
                        } else {
                            Toast.makeText(ViewApplications.this, "Location not available", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                // Handle close
                btnClose.setOnClickListener(close -> dialog.dismiss());
                dialog.show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ViewApplications.this, "Failed to load interview details", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadProjectAndCompany(String projId, String compId) {
        this.projectId = projId;
        this.companyId = compId;

        DatabaseReference projectRef = FirebaseDatabase.getInstance().getReference("projects").child(projId);
        DatabaseReference companyRef = FirebaseDatabase.getInstance().getReference("users").child(compId);

        projectRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                currentProject = snapshot.getValue(Project.class);
                if (currentProject == null) return;

                tvProjectTitle.setText(currentProject.getTitle());
                tvProjectDescription.setText(currentProject.getDescription());
                tvCategory.setText(currentProject.getCategory());
                tvSkillsRequired.setText(currentProject.getSkills() != null ? String.join(", ", currentProject.getSkills()) : "N/A");
                tvLocation.setText(currentProject.getLocation());
                tvDuration.setText(currentProject.getDuration());
                tvPaymentInfo.setText(currentProject.getCompensationType());
                tvPostedDate.setText(formatDate(currentProject.getCreatedAt()));

                hasResume = currentProject.isResumeRequired();
                hasQuiz = currentProject.getQuiz() != null;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        companyRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                tvCompanyName.setText(snapshot.child("name").getValue(String.class));
                tvContact.setText(snapshot.child("website").getValue(String.class));
                String logoUrl = snapshot.child("logoUrl").getValue(String.class);
                if (logoUrl != null && !logoUrl.isEmpty()) {
                    Glide.with(ViewApplications.this).load(logoUrl).into(ivCompanyLogo);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private String joinSkills(DataSnapshot skillsSnap) {
        StringBuilder skills = new StringBuilder();
        for (DataSnapshot skill : skillsSnap.getChildren()) {
            skills.append(skill.getValue(String.class)).append(", ");
        }
        return skills.length() > 0 ? skills.substring(0, skills.length() - 2) : "N/A";
    }

    private String getStatusTag(String status) {
        switch (status) {
            case "Accepted": return "🟢 Accepted";
            case "Rejected": return "🔴 Rejected";
            default: return "🟡 Pending";
        }
    }

    private String formatDate(Long timestamp) {
        if (timestamp == null) return "N/A";
        return new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date(timestamp));
    }

    private void setStatusAndButtons(String status) {
        if (status == null) status = "Pending";

        tvStatus.setText(getStatusTag(status));

        // Interview details button - show only for Shortlisted/Accepted
        btnView.setVisibility(("Shortlisted".equalsIgnoreCase(status) ||
                "Accepted".equalsIgnoreCase(status)) ? View.VISIBLE : View.GONE);

        // Modify reapply button visibility logic
        DatabaseReference applicationsRef = FirebaseDatabase.getInstance().getReference("applications");
        String finalStatus = status;

        // Query to check for any reapplications with this application as parent
        // AND check if current application is a reapplication
        DatabaseReference currentAppRef = applicationsRef.child(applicationId);
        currentAppRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot currentSnapshot) {
                // First check if current application is a reapplication
                boolean isCurrentReapplication = currentSnapshot.child("reapplication").exists() &&
                        Boolean.TRUE.equals(currentSnapshot.child("reapplication").getValue(Boolean.class));

                if (isCurrentReapplication) {
                    // If current application is a reapplication, hide the reapply button
                    btnReapply.setVisibility(View.GONE);
                } else {
                    // If not a reapplication, check if it has any child reapplications
                    applicationsRef.orderByChild("parentApplicationId")
                            .equalTo(applicationId)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    boolean hasReapplications = snapshot.exists();

                                    // Only show reapply button if:
                                    // 1. Status is Rejected AND
                                    // 2. No reapplications exist AND
                                    // 3. This is not a reapplication (already checked above)
                                    boolean showReapply = "Rejected".equalsIgnoreCase(finalStatus) &&
                                            !hasReapplications;
                                    btnReapply.setVisibility(showReapply ? View.VISIBLE : View.GONE);
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    btnReapply.setVisibility(View.GONE);
                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                btnReapply.setVisibility(View.GONE);
            }
        });

        // Withdraw button - show only for Shortlisted/Accepted
        btnWithdraw.setVisibility(("Shortlisted".equalsIgnoreCase(status) ||
                "Accepted".equalsIgnoreCase(status)) ? View.VISIBLE : View.GONE);
    }
}