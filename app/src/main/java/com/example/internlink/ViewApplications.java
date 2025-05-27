package com.example.internlink;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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

        tvStatus = findViewById(R.id.tv_status);
        tvSubmissionDate = findViewById(R.id.tv_submission_date);
        tvQuizScore = findViewById(R.id.tv_quiz_score);
        tvReapplicationLabel = findViewById(R.id.tv_reapplication_label);


        btnWithdraw = findViewById(R.id.btn_withdraw);
        btnView = findViewById(R.id.btn_view);
        btnReapply = findViewById(R.id.btn_reapply);

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

        appRef.child(newId).setValue(application).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Reapplied successfully!", Toast.LENGTH_SHORT).show();
                finish();
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

                setStatusAndButtons(status); // ✅ always use current status

                loadProjectAndCompany(projectId, companyId);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ViewApplications.this, "Error loading application", Toast.LENGTH_SHORT).show();
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
        btnView.setVisibility(("Shortlisted".equalsIgnoreCase(status) || "Accepted".equalsIgnoreCase(status)) ? View.VISIBLE : View.GONE);
        btnReapply.setVisibility("Rejected".equalsIgnoreCase(status) ? View.VISIBLE : View.GONE);
        btnWithdraw.setVisibility(!"Rejected".equalsIgnoreCase(status) ? View.VISIBLE : View.GONE);
    }

}
