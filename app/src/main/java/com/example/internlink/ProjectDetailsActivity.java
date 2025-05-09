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

        // Initialize all views
        initializeViews();

        // Get project ID from intent
        String projectId = getIntent().getStringExtra("PROJECT_ID");
        if (projectId == null || projectId.isEmpty()) {
            Toast.makeText(this, "Project not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Load project details from Firebase
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

        viewQuizButton.setOnClickListener(v -> {
            // Handle view quiz button click
            Toast.makeText(this, "View quiz questions", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadProjectDetails(String projectId) {
        DatabaseReference projectRef = FirebaseDatabase.getInstance().getReference("projects").child(projectId);

        projectRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Set basic project info
                    projectTitle.setText(dataSnapshot.child("title").getValue(String.class));
                    projectDescription.setText(dataSnapshot.child("description").getValue(String.class));

                    // Set status with appropriate color
                    String status = dataSnapshot.child("status").getValue(String.class);
                    projectStatus.setText(status);
                    setStatusBackground(status);

                    // Set basic information
                    projectCategory.setText(dataSnapshot.child("category").getValue(String.class));
                    projectCompensation.setText(dataSnapshot.child("compensationType").getValue(String.class));
                    projectEducation.setText(dataSnapshot.child("educationLevel").getValue(String.class));
                    projectStudentsRequired.setText(String.valueOf(dataSnapshot.child("studentsRequired").getValue(Integer.class)));
                    projectApplicants.setText(String.valueOf(dataSnapshot.child("applicants").getValue(Integer.class)));

                    // Set timeline information
                    projectStartDate.setText(dataSnapshot.child("startDate").getValue(String.class));
                    projectDuration.setText(dataSnapshot.child("duration").getValue(String.class));
                    projectDeadline.setText(dataSnapshot.child("deadline").getValue(String.class));

                    // Set skills
                    List<String> skills = (List<String>) dataSnapshot.child("skills").getValue();
                    if (skills != null && !skills.isEmpty()) {
                        projectSkills.setText(String.join(", ", skills));
                    } else {
                        projectSkills.setText("No specific skills required");
                    }

                    // Set quiz details if exists
                    if (dataSnapshot.hasChild("quiz")) {
                        findViewById(R.id.section_quiz).setVisibility(View.VISIBLE);
                        quizTitle.setText(dataSnapshot.child("quiz").child("title").getValue(String.class));
                        quizInstructions.setText(dataSnapshot.child("quiz").child("instructions").getValue(String.class));
                        quizTimeLimit.setText(dataSnapshot.child("quiz").child("timeLimit").getValue(Integer.class) + " minutes");
                        quizPassingScore.setText(dataSnapshot.child("quiz").child("passingScore").getValue(Integer.class) + "%");
                        viewQuizButton.setVisibility(View.VISIBLE);
                    } else {
                        findViewById(R.id.section_quiz).setVisibility(View.GONE);
                        viewQuizButton.setVisibility(View.GONE);
                    }
                } else {
                    Toast.makeText(ProjectDetailsActivity.this, "Project not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(ProjectDetailsActivity.this, "Failed to load project details", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void setStatusBackground(String status) {
        int color;
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
        projectStatus.setBackgroundColor(color);
    }
}