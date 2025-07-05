package com.example.internlink;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProjectDetailsActivity extends AppCompatActivity {

    private TextView projectTitle, projectStatus, projectDescription, projectCategory;
    private TextView projectCompensation, projectAmount, projectEducation, projectStudentsRequired, projectApplicants;
    private TextView projectStartDate, projectDuration, projectDeadline, projectSkills;
    private TextView quizTitle, quizInstructions, quizTimeLimit, quizPassingScore;
    private Button viewQuizButton;
    private LinearLayout amountPaidSection;
    private ImageButton backButton;  // <-- Back button added here

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_details);

        initializeViews();

        // Back button click listener: close activity and go back
        backButton.setOnClickListener(v -> finish());

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
        amountPaidSection = findViewById(R.id.amount_paid_section);
        projectAmount = findViewById(R.id.project_amount);
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
        backButton = findViewById(R.id.back_button); // <-- find the back button here

        viewQuizButton.setOnClickListener(v -> {
            DatabaseReference projectRef = FirebaseDatabase.getInstance()
                    .getReference("projects")
                    .child(getIntent().getStringExtra("PROJECT_ID"));

            projectRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (!snapshot.exists()) {
                        Toast.makeText(ProjectDetailsActivity.this, "Quiz data not found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    DataSnapshot quizSnapshot = snapshot.child("quiz");
                    if (!quizSnapshot.exists()) {
                        Toast.makeText(ProjectDetailsActivity.this, "No quiz available", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    showQuizDetails(quizSnapshot);
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    Toast.makeText(ProjectDetailsActivity.this, "Failed to load quiz", Toast.LENGTH_SHORT).show();
                }
            });
        });


    }
    private void showQuizDetails(DataSnapshot quizSnapshot) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_quiz_review);

        // Initialize views
        TextView titleView = dialog.findViewById(R.id.quiz_title);
        TextView timeLimitView = dialog.findViewById(R.id.quiz_time_limit);
        TextView passingScoreView = dialog.findViewById(R.id.quiz_passing_score);
        LinearLayout questionsContainer = dialog.findViewById(R.id.questions_container);
        ImageButton closeButton = dialog.findViewById(R.id.btn_close_quiz);

        // Set quiz details
        String title = quizSnapshot.child("title").getValue(String.class);
        Long timeLimit = quizSnapshot.child("timeLimit").getValue(Long.class);
        Long passingScore = quizSnapshot.child("passingScore").getValue(Long.class);

        titleView.setText(title != null ? title + " - Answer Key" : "Quiz Answer Key");
        timeLimitView.setText(timeLimit != null ? timeLimit + " minutes" : "No time limit");
        passingScoreView.setText(passingScore != null ? "Pass: " + passingScore + "%" : "No passing score set");

        // Add questions
        int questionNum = 1;
        for (DataSnapshot questionSnap : quizSnapshot.child("questions").getChildren()) {
            View questionView = createQuestionReviewView(questionNum++, questionSnap);
            questionsContainer.addView(questionView);
        }

        // Setup close button
        closeButton.setOnClickListener(v -> dialog.dismiss());

        // Show dialog
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
        dialog.show();
    }

    private View createQuestionReviewView(int number, DataSnapshot questionSnap) {
        View view = getLayoutInflater().inflate(R.layout.item_quiz_question_review, null);

        TextView numberView = view.findViewById(R.id.question_number);
        TextView textView = view.findViewById(R.id.question_text);
        TextView typeView = view.findViewById(R.id.question_type);
        LinearLayout optionsContainer = view.findViewById(R.id.options_container);

        String text = questionSnap.child("text").getValue(String.class);
        String type = questionSnap.child("type").getValue(String.class);

        numberView.setText("Question " + number);
        textView.setText(text != null ? text : "");
        typeView.setText(type != null ? type : "");

        if ("Multiple Choice".equals(type)) {
            for (DataSnapshot optionSnap : questionSnap.child("options").getChildren()) {
                View optionView = createOptionReviewView(optionSnap);
                optionsContainer.addView(optionView);
            }
            optionsContainer.setVisibility(View.VISIBLE);
        } else if ("True/False".equals(type)) {
            // Handle True/False question
            DataSnapshot optionsSnap = questionSnap.child("options");
            if (optionsSnap.exists()) {
                for (DataSnapshot optionSnap : optionsSnap.getChildren()) {
                    Boolean isCorrect = optionSnap.child("correct").getValue(Boolean.class);
                    String optionText = optionSnap.child("text").getValue(String.class);
                    if (Boolean.TRUE.equals(isCorrect)) {
                        View tfView = getLayoutInflater().inflate(R.layout.item_quiz_question_tf, null);
                        TextView answerText = tfView.findViewById(R.id.answer_text);
                        answerText.setText(optionText);
                        optionsContainer.addView(tfView);
                        break;
                    }
                }
            }
            optionsContainer.setVisibility(View.VISIBLE);
        } else {
            optionsContainer.setVisibility(View.GONE);
        }

        return view;
    }

    private View createOptionReviewView(DataSnapshot optionSnap) {
        View view = getLayoutInflater().inflate(R.layout.item_quiz_option_review, null);

        TextView textView = view.findViewById(R.id.option_text);
        TextView correctLabel = view.findViewById(R.id.correct_answer_label);

        String text = optionSnap.child("text").getValue(String.class);
        Boolean isCorrect = optionSnap.child("correct").getValue(Boolean.class);

        textView.setText(text != null ? text : "");
        correctLabel.setVisibility(Boolean.TRUE.equals(isCorrect) ? View.VISIBLE : View.GONE);

        return view;
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
                if ("Unpaid".equalsIgnoreCase(project.getCompensationType())) {
                    amountPaidSection.setVisibility(View.GONE);
                } else {
                    amountPaidSection.setVisibility(View.VISIBLE);
                    projectAmount.setText(String.valueOf(project.getAmount()));
                }
                projectEducation.setText(getSafeString(project.getEducationLevel()));
                projectStudentsRequired.setText(String.valueOf(project.getStudentsRequired()));
                projectApplicants.setText(String.valueOf(project.getApplicants()));
                projectStartDate.setText(formatDate(project.getStartDate()));
                projectDuration.setText(getSafeString(project.getDuration()));
                projectDeadline.setText(formatDate(project.getDeadline()));
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
            color = Color.parseColor("#B0BEC5"); // Soft blue-grey
        } else {
            switch (status.toLowerCase()) {
                case "approved":
                    color = Color.parseColor("#4CAF50"); // Medium green
                    break;
                case "pending":
                    color = Color.parseColor("#FFB300"); // Amber
                    break;
                case "rejected":
                    color = Color.parseColor("#E53935"); // Soft red
                    break;
                case "completed":
                    color = Color.parseColor("#1E88E5"); // Blue
                    break;
                default:
                    color = Color.parseColor("#90A4AE"); // Blue Grey
            }
        }

        // Set the rounded background drawable and update its color
        projectStatus.setBackgroundResource(R.drawable.rounded_button);
        if (projectStatus.getBackground() instanceof android.graphics.drawable.GradientDrawable) {
            android.graphics.drawable.GradientDrawable bgDrawable =
                    (android.graphics.drawable.GradientDrawable) projectStatus.getBackground();
            bgDrawable.setColor(color);
        }
    }

    private String formatDate(long timestamp) {
        if (timestamp == 0) return "N/A";
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

}