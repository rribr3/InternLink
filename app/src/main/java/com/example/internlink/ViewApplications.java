package com.example.internlink;

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

public class ViewApplications extends AppCompatActivity {

    private ImageView ivCompanyLogo;
    private TextView tvProjectTitle, tvCompanyName, tvProjectDescription,
            tvCategory, tvSkillsRequired, tvLocation, tvDuration,
            tvPaymentInfo, tvPostedDate, tvContact,
            tvStatus, tvSubmissionDate, tvQuizScore, tvCompanyMessage;
    private Button btnWithdraw, btnReapply;

    private String applicationId;

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

        btnWithdraw = findViewById(R.id.btn_withdraw);
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

                String projectId = snapshot.child("projectId").getValue(String.class);
                String companyId = snapshot.child("companyId").getValue(String.class);
                String status = snapshot.child("status").getValue(String.class);
                Long appliedDate = snapshot.child("appliedDate").getValue(Long.class);
                Integer quizGrade = snapshot.child("quizGrade").getValue(Integer.class);

                tvStatus.setText(getStatusTag(status));
                tvSubmissionDate.setText("Submitted on: " + formatDate(appliedDate));
                tvQuizScore.setText("Quiz Score: " + (quizGrade != null ? quizGrade + "/100" : "N/A"));

                loadProjectAndCompany(projectId, companyId);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ViewApplications.this, "Error loading application", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadProjectAndCompany(String projectId, String companyId) {
        DatabaseReference projectRef = FirebaseDatabase.getInstance().getReference("projects").child(projectId);
        DatabaseReference companyRef = FirebaseDatabase.getInstance().getReference("users").child(companyId);

        projectRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot projectSnap) {
                if (!projectSnap.exists()) return;

                tvProjectTitle.setText(projectSnap.child("title").getValue(String.class));
                tvProjectDescription.setText(projectSnap.child("description").getValue(String.class));
                tvCategory.setText(projectSnap.child("category").getValue(String.class));
                tvSkillsRequired.setText(joinSkills(projectSnap.child("skills")));
                tvLocation.setText(projectSnap.child("location").getValue(String.class));
                tvDuration.setText(projectSnap.child("duration").getValue(String.class));
                tvPaymentInfo.setText(projectSnap.child("compensationType").getValue(String.class));
                tvPostedDate.setText(formatDate(projectSnap.child("createdAt").getValue(Long.class)));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });

        companyRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot companySnap) {
                if (!companySnap.exists()) return;

                tvCompanyName.setText(companySnap.child("name").getValue(String.class));
                tvContact.setText(companySnap.child("website").getValue(String.class));
                String logoUrl = companySnap.child("logoUrl").getValue(String.class);
                if (logoUrl != null && !logoUrl.isEmpty()) {
                    Glide.with(ViewApplications.this).load(logoUrl).into(ivCompanyLogo);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
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
}
