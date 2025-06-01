package com.example.internlink;

import android.content.Context;
import android.graphics.Color;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class FeedbackExAdapter extends RecyclerView.Adapter<FeedbackExAdapter.FeedbackViewHolder> {

    private List<CompanyFeedback> feedbackList;
    private Context context;
    private boolean isHorizontalLayout;

    // Constructor for vertical layout (default)
    public FeedbackExAdapter(List<CompanyFeedback> feedbackList, Context context) {
        this.feedbackList = feedbackList;
        this.context = context;
        this.isHorizontalLayout = false;
    }

    // Constructor with layout option
    public FeedbackExAdapter(List<CompanyFeedback> feedbackList, Context context, boolean isHorizontalLayout) {
        this.feedbackList = feedbackList;
        this.context = context;
        this.isHorizontalLayout = isHorizontalLayout;
    }

    @NonNull
    @Override
    public FeedbackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (isHorizontalLayout) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_feedback, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_feedback_vertical, parent, false);
        }
        return new FeedbackViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FeedbackViewHolder holder, int position) {
        CompanyFeedback feedback = feedbackList.get(position);

        // Set student name
        if (feedback.getStudentName() != null && !feedback.getStudentName().isEmpty()) {
            holder.tvStudentName.setText(feedback.getStudentName());
        } else {
            holder.tvStudentName.setText("Anonymous Student");
        }

        // Set project title
        if (feedback.getProjectTitle() != null && !feedback.getProjectTitle().isEmpty()) {
            holder.tvProjectTitle.setText(feedback.getProjectTitle());
        } else {
            holder.tvProjectTitle.setText("Project Review");
        }

        // Set rating
        holder.ratingBar.setRating(feedback.getRating());
        holder.tvRatingValue.setText(String.format("%.1f", feedback.getRating()));

        // Set rating value color based on rating
        int ratingColor = getRatingColor(feedback.getRating());
        holder.tvRatingValue.setTextColor(ratingColor);

        // Set comment
        if (feedback.getComment() != null && !feedback.getComment().trim().isEmpty()) {
            holder.tvComment.setText(feedback.getComment());
            holder.tvComment.setVisibility(View.VISIBLE);
        } else {
            holder.tvComment.setText("No additional comments provided.");
            holder.tvComment.setTextColor(Color.parseColor("#999999"));
            holder.tvComment.setVisibility(View.VISIBLE);
        }

        // Set time ago
        String timeAgo = getTimeAgo(feedback.getTimestamp());
        holder.tvTimeAgo.setText(timeAgo);

        // Load student profile image
        if (feedback.getStudentProfileUrl() != null && !feedback.getStudentProfileUrl().isEmpty()) {
            Glide.with(context)
                    .load(feedback.getStudentProfileUrl())
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .into(holder.ivStudentProfile);
        } else {
            holder.ivStudentProfile.setImageResource(R.drawable.ic_person);
        }

        // Show performance highlights for high ratings
        if (holder.performanceHighlights != null) {
            if (feedback.getRating() >= 4.5f) {
                holder.performanceHighlights.setVisibility(View.VISIBLE);
                setupPerformanceHighlights(holder.performanceHighlights, feedback.getRating());
            } else {
                holder.performanceHighlights.setVisibility(View.GONE);
            }
        }

        // Setup helpful action (optional)
        if (holder.tvHelpful != null) {
            holder.tvHelpful.setOnClickListener(v -> {
                // Handle helpful click - you can implement this feature later
                holder.tvHelpful.setText("👍 Marked as Helpful");
                holder.tvHelpful.setTextColor(Color.parseColor("#4CAF50"));
            });
        }
    }

    @Override
    public int getItemCount() {
        return feedbackList.size();
    }

    private int getRatingColor(float rating) {
        if (rating >= 4.5f) {
            return Color.parseColor("#4CAF50"); // Green for excellent
        } else if (rating >= 4.0f) {
            return Color.parseColor("#2196F3"); // Blue for very good
        } else if (rating >= 3.0f) {
            return Color.parseColor("#FF9800"); // Orange for good
        } else if (rating >= 2.0f) {
            return Color.parseColor("#FF5722"); // Red-orange for fair
        } else {
            return Color.parseColor("#F44336"); // Red for poor
        }
    }

    private void setupPerformanceHighlights(LinearLayout performanceHighlights, float rating) {
        performanceHighlights.removeAllViews();

        if (rating >= 4.8f) {
            addHighlight(performanceHighlights, "🌟 Outstanding", "#4CAF50");
            addHighlight(performanceHighlights, "🏆 Top Rated", "#2196F3");
        } else if (rating >= 4.5f) {
            addHighlight(performanceHighlights, "⭐ Excellent", "#4CAF50");
            addHighlight(performanceHighlights, "💼 Professional", "#2196F3");
        }
    }

    private void addHighlight(LinearLayout container, String text, String colorHex) {
        TextView highlight = new TextView(context);
        highlight.setText(text);
        highlight.setTextSize(10f);
        highlight.setTextColor(Color.parseColor(colorHex));
        highlight.setBackgroundResource(R.drawable.rounded_background);
        highlight.setPadding(12, 6, 12, 6);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMarginEnd(8);
        highlight.setLayoutParams(params);

        container.addView(highlight);
    }

    private String getTimeAgo(long timestamp) {
        if (timestamp == 0) {
            return "Recently";
        }

        long now = System.currentTimeMillis();
        long timeDiff = now - timestamp;

        if (timeDiff < DateUtils.MINUTE_IN_MILLIS) {
            return "Just now";
        } else if (timeDiff < DateUtils.HOUR_IN_MILLIS) {
            int minutes = (int) (timeDiff / DateUtils.MINUTE_IN_MILLIS);
            return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
        } else if (timeDiff < DateUtils.DAY_IN_MILLIS) {
            int hours = (int) (timeDiff / DateUtils.HOUR_IN_MILLIS);
            return hours + (hours == 1 ? " hour ago" : " hours ago");
        } else if (timeDiff < DateUtils.WEEK_IN_MILLIS) {
            int days = (int) (timeDiff / DateUtils.DAY_IN_MILLIS);
            return days + (days == 1 ? " day ago" : " days ago");
        } else if (timeDiff < DateUtils.WEEK_IN_MILLIS * 4) {
            int weeks = (int) (timeDiff / DateUtils.WEEK_IN_MILLIS);
            return weeks + (weeks == 1 ? " week ago" : " weeks ago");
        } else {
            int months = (int) (timeDiff / (DateUtils.WEEK_IN_MILLIS * 4));
            return months + (months == 1 ? " month ago" : " months ago");
        }
    }

    public void updateFeedbackList(List<CompanyFeedback> newFeedbackList) {
        this.feedbackList = newFeedbackList;
        notifyDataSetChanged();
    }

    // Method to clear data for refresh functionality
    public void clearData() {
        this.feedbackList.clear();
        notifyDataSetChanged();
    }

    static class FeedbackViewHolder extends RecyclerView.ViewHolder {
        CircleImageView ivStudentProfile;
        TextView tvStudentName;
        TextView tvProjectTitle;
        TextView tvTimeAgo;
        RatingBar ratingBar;
        TextView tvRatingValue;
        TextView tvComment;
        TextView tvHelpful;
        LinearLayout performanceHighlights;

        public FeedbackViewHolder(@NonNull View itemView) {
            super(itemView);
            ivStudentProfile = itemView.findViewById(R.id.iv_student_profile);
            tvStudentName = itemView.findViewById(R.id.tv_student_name);
            tvProjectTitle = itemView.findViewById(R.id.tv_project_title);
            tvTimeAgo = itemView.findViewById(R.id.tv_time_ago);
            ratingBar = itemView.findViewById(R.id.rating_bar);
            tvRatingValue = itemView.findViewById(R.id.tv_rating_value);
            tvComment = itemView.findViewById(R.id.tv_comment);
            tvHelpful = itemView.findViewById(R.id.tv_helpful);
            performanceHighlights = itemView.findViewById(R.id.performance_highlights);
        }
    }
}