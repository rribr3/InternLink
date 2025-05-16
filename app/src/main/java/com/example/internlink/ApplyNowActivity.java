package com.example.internlink;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ApplyNowActivity extends AppCompatActivity {

    private TextView titleText, descText, categoryText, educationText, compensationText, amountText;
    private TextView deadlineText, startDateText, durationText, applicantsText, studentsRequiredText, skillsText;
    private LinearLayout amountSection, quizSection;
    private TextView quizTitle, quizInstructions, quizTime, quizScore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apply_now);

        // Get project ID from intent
        String projectId = getIntent().getStringExtra("PROJECT_ID");
        if (projectId == null || projectId.isEmpty()) {
            Toast.makeText(this, "Project ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        loadProjectData(projectId);
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
    }

    private void loadProjectData(String projectId) {
        DatabaseReference projectRef = FirebaseDatabase.getInstance().getReference("projects").child(projectId);

        projectRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Project project = snapshot.getValue(Project.class);
                if (project == null) {
                    Toast.makeText(ApplyNowActivity.this, "Project not found", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                titleText.setText(project.getTitle());
                descText.setText(project.getDescription());
                categoryText.setText(project.getCategory());
                educationText.setText(project.getEducationLevel());
                compensationText.setText(project.getCompensationType());
                deadlineText.setText(formatDate(project.getDeadline()));
                startDateText.setText(formatDate(project.getStartDate()));
                durationText.setText(project.getDuration());
                applicantsText.setText(String.valueOf(project.getApplicants()));
                studentsRequiredText.setText(String.valueOf(project.getStudentsRequired()));

                if (project.getSkills() != null && !project.getSkills().isEmpty()) {
                    skillsText.setText(String.join(", ", project.getSkills()));
                } else {
                    skillsText.setText("No specific skills listed");
                }


                // Show amount if Paid
                if ("Paid".equalsIgnoreCase(project.getCompensationType())) {
                    amountSection.setVisibility(View.VISIBLE);
                    amountText.setText(String.valueOf(project.getAmount()));
                }

                // Show quiz section if exists
                if (project.getQuiz() != null) {
                    quizSection.setVisibility(View.VISIBLE);
                    quizTitle.setText(project.getQuiz().getTitle());
                    quizInstructions.setText(project.getQuiz().getInstructions());
                    quizTime.setText(project.getQuiz().getTimeLimit() + " mins");
                    quizScore.setText(project.getQuiz().getPassingScore() + "%");
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

