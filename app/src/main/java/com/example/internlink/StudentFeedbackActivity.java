package com.example.internlink;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StudentFeedbackActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private TextView tvOverallRating, tvTotalReviews, tvNoFeedback;
    private RatingBar rbOverallRating;
    private RecyclerView rvFeedback;
    private ProgressBar progressBar;
    private View layoutOverallStats, layoutNoFeedback;

    // Rating breakdown views
    private ProgressBar pb5Star, pb4Star, pb3Star, pb2Star, pb1Star;
    private TextView tv5StarCount, tv4StarCount, tv3StarCount, tv2StarCount, tv1StarCount;
    private TextView tv5StarPercent, tv4StarPercent, tv3StarPercent, tv2StarPercent, tv1StarPercent;

    private StudentFeedbackAdapter feedbackAdapter;
    private List<StudentFeedback> feedbackList;
    private String studentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_feedback);

        // Initialize views
        initializeViews();

        // Setup toolbar
        setupToolbar();

        // Get student ID
        studentId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        if (studentId == null || studentId.isEmpty()) {
            Toast.makeText(this, "Error: User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize RecyclerView
        setupRecyclerView();

        // Load feedback data
        loadStudentFeedback();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        tvOverallRating = findViewById(R.id.tv_overall_rating);
        tvTotalReviews = findViewById(R.id.tv_total_reviews);
        tvNoFeedback = findViewById(R.id.tv_no_feedback);
        rbOverallRating = findViewById(R.id.rb_overall_rating);
        rvFeedback = findViewById(R.id.rv_feedback);
        progressBar = findViewById(R.id.progress_bar);
        layoutOverallStats = findViewById(R.id.layout_overall_stats);
        layoutNoFeedback = findViewById(R.id.layout_no_feedback);

        // Rating breakdown views
        pb5Star = findViewById(R.id.pb_5_star);
        pb4Star = findViewById(R.id.pb_4_star);
        pb3Star = findViewById(R.id.pb_3_star);
        pb2Star = findViewById(R.id.pb_2_star);
        pb1Star = findViewById(R.id.pb_1_star);

        tv5StarCount = findViewById(R.id.tv_5_star_count);
        tv4StarCount = findViewById(R.id.tv_4_star_count);
        tv3StarCount = findViewById(R.id.tv_3_star_count);
        tv2StarCount = findViewById(R.id.tv_2_star_count);
        tv1StarCount = findViewById(R.id.tv_1_star_count);

        tv5StarPercent = findViewById(R.id.tv_5_star_percent);
        tv4StarPercent = findViewById(R.id.tv_4_star_percent);
        tv3StarPercent = findViewById(R.id.tv_3_star_percent);
        tv2StarPercent = findViewById(R.id.tv_2_star_percent);
        tv1StarPercent = findViewById(R.id.tv_1_star_percent);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Reviews");
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        feedbackList = new ArrayList<>();
        feedbackAdapter = new StudentFeedbackAdapter(feedbackList, this);
        rvFeedback.setLayoutManager(new LinearLayoutManager(this));
        rvFeedback.setAdapter(feedbackAdapter);
    }

    private void loadStudentFeedback() {
        progressBar.setVisibility(View.VISIBLE);
        layoutOverallStats.setVisibility(View.GONE);
        layoutNoFeedback.setVisibility(View.GONE);
        rvFeedback.setVisibility(View.GONE);

        DatabaseReference feedbackRef = FirebaseDatabase.getInstance()
                .getReference("student_feedback");

        feedbackRef.orderByChild("studentId").equalTo(studentId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        feedbackList.clear();

                        for (DataSnapshot feedbackSnapshot : snapshot.getChildren()) {
                            StudentFeedback feedback = feedbackSnapshot.getValue(StudentFeedback.class);
                            if (feedback != null) {
                                feedback.setFeedbackId(feedbackSnapshot.getKey());
                                feedbackList.add(feedback);
                            }
                        }

                        progressBar.setVisibility(View.GONE);

                        if (feedbackList.isEmpty()) {
                            showNoFeedbackState();
                        } else {
                            // Sort feedback by timestamp (newest first)
                            Collections.sort(feedbackList, (f1, f2) ->
                                    Long.compare(f2.getTimestamp(), f1.getTimestamp()));

                            showFeedbackData();
                            calculateAndDisplayStats();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(StudentFeedbackActivity.this,
                                "Failed to load feedback: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        showNoFeedbackState();
                    }
                });
    }

    private void showNoFeedbackState() {
        layoutNoFeedback.setVisibility(View.VISIBLE);
        layoutOverallStats.setVisibility(View.GONE);
        rvFeedback.setVisibility(View.GONE);
    }

    private void showFeedbackData() {
        layoutNoFeedback.setVisibility(View.GONE);
        layoutOverallStats.setVisibility(View.VISIBLE);
        rvFeedback.setVisibility(View.VISIBLE);

        feedbackAdapter.updateFeedbackList(feedbackList);
    }

    private void calculateAndDisplayStats() {
        if (feedbackList.isEmpty()) return;

        int totalReviews = feedbackList.size();
        float totalRating = 0;

        // Count ratings by star
        int[] starCounts = new int[6]; // Index 0 unused, 1-5 for stars

        for (StudentFeedback feedback : feedbackList) {
            totalRating += feedback.getRating();
            int starRating = Math.round(feedback.getRating());
            if (starRating >= 1 && starRating <= 5) {
                starCounts[starRating]++;
            }
        }

        float averageRating = totalRating / totalReviews;

        // Update overall stats
        tvOverallRating.setText(String.format("%.1f", averageRating));
        tvTotalReviews.setText(totalReviews + (totalReviews == 1 ? " review" : " reviews"));
        rbOverallRating.setRating(averageRating);

        // Update rating breakdown
        updateRatingBreakdown(starCounts, totalReviews);
    }

    private void updateRatingBreakdown(int[] starCounts, int totalReviews) {
        updateStarBreakdown(5, starCounts[5], totalReviews, pb5Star, tv5StarCount, tv5StarPercent);
        updateStarBreakdown(4, starCounts[4], totalReviews, pb4Star, tv4StarCount, tv4StarPercent);
        updateStarBreakdown(3, starCounts[3], totalReviews, pb3Star, tv3StarCount, tv3StarPercent);
        updateStarBreakdown(2, starCounts[2], totalReviews, pb2Star, tv2StarCount, tv2StarPercent);
        updateStarBreakdown(1, starCounts[1], totalReviews, pb1Star, tv1StarCount, tv1StarPercent);
    }

    private void updateStarBreakdown(int starNumber, int count, int totalReviews,
                                     ProgressBar progressBar, TextView countTextView, TextView percentTextView) {
        int percentage = totalReviews > 0 ? (count * 100) / totalReviews : 0;

        progressBar.setMax(totalReviews);
        progressBar.setProgress(count);

        countTextView.setText(String.valueOf(count));
        percentTextView.setText(percentage + "%");
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}