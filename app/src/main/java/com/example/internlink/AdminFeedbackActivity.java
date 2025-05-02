package com.example.internlink;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

public class AdminFeedbackActivity extends AppCompatActivity {

    // Progress + Percentage TextViews
    TextView text1Star, text2Star;
    // Feedback summary
    TextView totalFeedbackText, newFeedbackText, companyFeedbackCount, studentFeedbackCount;
    // Buttons
    MaterialButton btnAll, btnCompanyFeedback, btnStudentFeedback, btnComplaints, btnSuggestions, btnBestFeedback;
    // Chips
    Chip chipRatingAll, chipRating5, chipRating4, chipRating3;
    // RecyclerView
    RecyclerView feedbackRecyclerView;
    FeedbackAdapter feedbackAdapter;
    List<FeedbackItem> feedbackList = new ArrayList<>();
    private AppCompatButton backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_feedback);

        backButton = findViewById(R.id.backButton);  // Initialize back button
        backButton.setOnClickListener(v -> finish());

        // Initialize views
        text1Star = findViewById(R.id.text1Star);
        text2Star = findViewById(R.id.text2Star);
        totalFeedbackText = findViewById(R.id.totalFeedbackText);
        newFeedbackText = findViewById(R.id.newFeedbackText);
        companyFeedbackCount = findViewById(R.id.companyFeedbackCount);
        studentFeedbackCount = findViewById(R.id.studentFeedbackCount);

        btnAll = findViewById(R.id.btnAll);
        btnCompanyFeedback = findViewById(R.id.btnCompanyFeedback);
        btnStudentFeedback = findViewById(R.id.btnStudentFeedback);
        btnComplaints = findViewById(R.id.btnComplaints);
        btnSuggestions = findViewById(R.id.btnSuggestions);
        btnBestFeedback = findViewById(R.id.btnBestFeedback);

        chipRatingAll = findViewById(R.id.chipRatingAll);
        chipRating5 = findViewById(R.id.chipRating5);
        chipRating4 = findViewById(R.id.chipRating4);
        chipRating3 = findViewById(R.id.chipRating3);

        feedbackRecyclerView = findViewById(R.id.feedbackRecyclerView);
        feedbackRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Populate dummy feedback data
        loadDummyFeedback();

        // Set adapter
        feedbackAdapter = new FeedbackAdapter(feedbackList);
        feedbackRecyclerView.setAdapter(feedbackAdapter);

        // Button click listeners
        btnAll.setOnClickListener(v -> filterFeedback("all"));
        btnCompanyFeedback.setOnClickListener(v -> filterFeedback("company"));
        btnStudentFeedback.setOnClickListener(v -> filterFeedback("student"));
        btnComplaints.setOnClickListener(v -> filterFeedback("complaint"));
        btnSuggestions.setOnClickListener(v -> filterFeedback("suggestion"));
        btnBestFeedback.setOnClickListener(v -> filterFeedback("top"));

        chipRatingAll.setOnClickListener(v -> filterByRating(0));
        chipRating5.setOnClickListener(v -> filterByRating(5));
        chipRating4.setOnClickListener(v -> filterByRating(4));
        chipRating3.setOnClickListener(v -> filterByRating(3));
    }

    private void loadDummyFeedback() {
        feedbackList.add(new FeedbackItem("Great platform!", "Student", 5));
        feedbackList.add(new FeedbackItem("Needs improvement in UI", "Company", 2));
        feedbackList.add(new FeedbackItem("Loved the experience!", "Student", 4));
        feedbackList.add(new FeedbackItem("Could be faster", "Company", 3));
        feedbackList.add(new FeedbackItem("Excellent!", "Student", 5));
    }

    private void filterFeedback(String type) {
        Toast.makeText(this, "Filtered by: " + type, Toast.LENGTH_SHORT).show();
        // Add your filtering logic based on type here
    }

    private void filterByRating(int rating) {
        Toast.makeText(this, rating == 0 ? "All ratings" : "Filtered by: " + rating + " stars", Toast.LENGTH_SHORT).show();
        // Add your filtering logic by rating here
    }
}
