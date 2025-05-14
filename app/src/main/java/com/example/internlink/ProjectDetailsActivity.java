package com.example.internlink;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class ProjectDetailsActivity extends AppCompatActivity {

    private TextView projectTitle, projectStatus, projectDescription, projectCategory;
    private TextView projectCompensation, projectEducation, projectStudentsRequired, projectApplicants;
    private TextView projectStartDate, projectDuration, projectDeadline, projectSkills;
    private TextView quizTitle, quizInstructions, quizTimeLimit, quizPassingScore;
    private Button viewQuizButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_details);

        initializeViews();

        String projectId = getIntent().getStringExtra("PROJECT_ID");
        if (projectId == null || projectId.isEmpty()) {
            Toast.makeText(this, "Project not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadProjectDetails(projectId);
    }

    private void initializeViews() {
        projectTitle = findViewById(R.id.project_title);
        projectStatus = findViewById(R.id.project_status);
        projectDescription = findViewById(R.id.project_description);
        projectCategory = findViewById(R.id.project_category);
        projectCompensation = findViewById(R.id.project_compensation);
        projectEducation = findViewById(R.id.project_education);
        projectStudentsRequired = findViewById(R.id.project_students_required);
        projectApplicants = findViewById(R.id.project_applicants);
        projectStartDate = findViewById(R.id.project_start_date);
        projectDuration = findViewById(R.id.project_duration);
        projectDeadline = findViewById(R.id.project_deadline);
        projectSkills = findViewById(R.id.project_skills);
        quizTitle = findViewById(R.id.quiz_title);
        quizInstructions = findViewById(R.id.quiz_instructions);
        quizTimeLimit = findViewById(R.id.quiz_time_limit);
        quizPassingScore = findViewById(R.id.quiz_passing_score);
        viewQuizButton = findViewById(R.id.view_quiz_button);

        viewQuizButton.setOnClickListener(v ->
                Toast.makeText(this, "View quiz questions", Toast.LENGTH_SHORT).show()
        );
    }

    private void loadProjectDetails(String projectId) {
        DatabaseReference projectRef = FirebaseDatabase.getInstance().getReference("projects").child(projectId);

        projectRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(ProjectDetailsActivity.this, "Project not found", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                Project project = snapshot.getValue(Project.class);
                if (project == null) {
                    Toast.makeText(ProjectDetailsActivity.this, "Project data is corrupted", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                // Populate UI
                projectTitle.setText(getSafeString(project.getTitle()));
                projectDescription.setText(getSafeString(project.getDescription()));
                projectCategory.setText(getSafeString(project.getCategory()));
                projectCompensation.setText(getSafeString(project.getCompensationType()));
                projectEducation.setText(getSafeString(project.getEducationLevel()));
                projectStudentsRequired.setText(String.valueOf(project.getStudentsRequired()));
                projectApplicants.setText(String.valueOf(project.getAmount()));
                projectStartDate.setText(getSafeString(project.getStartDate()));
                projectDuration.setText(getSafeString(project.getDuration()));
                projectDeadline.setText(getSafeString(project.getDeadline()));
                projectStatus.setText(getSafeString(project.getStatus()));
                setStatusBackground(project.getStatus());

                List<String> skills = project.getSkills();
                if (skills != null && !skills.isEmpty()) {
                    projectSkills.setText(String.join(", ", skills));
                } else {
                    projectSkills.setText("No specific skills required");
                }

                if (project.getQuiz() != null) {
                    findViewById(R.id.section_quiz).setVisibility(View.VISIBLE);
                    quizTitle.setText(getSafeString(project.getQuiz().getTitle()));
                    quizInstructions.setText(getSafeString(project.getQuiz().getInstructions()));
                    quizTimeLimit.setText(project.getQuiz().getTimeLimit() + " minutes");
                    quizPassingScore.setText(project.getQuiz().getPassingScore() + "%");
                    viewQuizButton.setVisibility(View.VISIBLE);
                } else {
                    findViewById(R.id.section_quiz).setVisibility(View.GONE);
                    viewQuizButton.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(ProjectDetailsActivity.this, "Failed to load project details", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private String getSafeString(String value) {
        return value != null ? value : "N/A";
    }

    private void setStatusBackground(String status) {
        int color;
        if (status == null) {
            color = Color.GRAY;
        } else {
            switch (status.toLowerCase()) {
                case "approved":
                    color = Color.GREEN;
                    break;
                case "pending":
                    color = Color.YELLOW;
                    break;
                case "rejected":
                    color = Color.RED;
                    break;
                case "completed":
                    color = Color.BLUE;
                    break;
                default:
                    color = Color.GRAY;
            }
        }
        projectStatus.setBackgroundColor(color);
    }
}
