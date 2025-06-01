package com.example.internlink;

// Updated StudentFeedbackAdapter.java
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

public class StudentFeedbackAdapter extends RecyclerView.Adapter<StudentFeedbackAdapter.StudentFeedbackViewHolder> {

    private List<StudentFeedback> feedbackList;
    private Context context;
    private boolean isHorizontalLayout;

    // Constructor for vertical layout (default)
    public StudentFeedbackAdapter(List<StudentFeedback> feedbackList, Context context) {
        this.feedbackList = feedbackList;
        this.context = context;
        this.isHorizontalLayout = false;
    }

    // Constructor with layout option
    public StudentFeedbackAdapter(List<StudentFeedback> feedbackList, Context context, boolean isHorizontalLayout) {
        this.feedbackList = feedbackList;
        this.context = context;
        this.isHorizontalLayout = isHorizontalLayout;
    }

    @NonNull
    @Override
    public StudentFeedbackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (isHorizontalLayout) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_student_feedback, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_student_feedback_vertical, parent, false);
        }
        return new StudentFeedbackViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentFeedbackViewHolder holder, int position) {
        StudentFeedback feedback = feedbackList.get(position);

        // Set company name
        holder.tvCompanyName.setText(feedback.getCompanyName());

        // Set project title
        holder.tvProjectTitle.setText(feedback.getProjectTitle());

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

        // Load company logo
        if (feedback.getCompanyLogoUrl() != null && !feedback.getCompanyLogoUrl().isEmpty()) {
            Glide.with(context)
                    .load(feedback.getCompanyLogoUrl())
                    .placeholder(R.drawable.ic_company)
                    .error(R.drawable.ic_company)
                    .into(holder.ivCompanyLogo);
        } else {
            holder.ivCompanyLogo.setImageResource(R.drawable.ic_company);
        }

        // Show performance badges for high ratings
        if (holder.performanceBadges != null) {
            if (feedback.getRating() >= 4.5f) {
                holder.performanceBadges.setVisibility(View.VISIBLE);
                setupPerformanceBadges(holder.performanceBadges, feedback.getRating());
            } else {
                holder.performanceBadges.setVisibility(View.GONE);
            }
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

    private void setupPerformanceBadges(LinearLayout performanceBadges, float rating) {
        performanceBadges.removeAllViews();

        if (rating >= 4.8f) {
            addBadge(performanceBadges, "⭐ Outstanding", "#4CAF50");
            addBadge(performanceBadges, "🏆 Top Performer", "#2196F3");
        } else if (rating >= 4.5f) {
            addBadge(performanceBadges, "⭐ Excellent", "#4CAF50");
            addBadge(performanceBadges, "🚀 Great Work", "#2196F3");
        }
    }

    private void addBadge(LinearLayout container, String text, String colorHex) {
        TextView badge = new TextView(context);
        badge.setText(text);
        badge.setTextSize(10f);
        badge.setTextColor(Color.parseColor(colorHex));
        badge.setBackgroundResource(R.drawable.rounded_background);
        badge.setPadding(12, 6, 12, 6);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMarginEnd(8);
        badge.setLayoutParams(params);

        container.addView(badge);
    }

    private String getTimeAgo(long timestamp) {
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

    public void updateFeedbackList(List<StudentFeedback> newFeedbackList) {
        this.feedbackList = newFeedbackList;
        notifyDataSetChanged();
    }

    static class StudentFeedbackViewHolder extends RecyclerView.ViewHolder {
        CircleImageView ivCompanyLogo;
        TextView tvCompanyName;
        TextView tvProjectTitle;
        TextView tvTimeAgo;
        RatingBar ratingBar;
        TextView tvRatingValue;
        TextView tvComment;
        LinearLayout performanceBadges;

        public StudentFeedbackViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCompanyLogo = itemView.findViewById(R.id.iv_company_logo);
            tvCompanyName = itemView.findViewById(R.id.tv_company_name);
            tvProjectTitle = itemView.findViewById(R.id.tv_project_title);
            tvTimeAgo = itemView.findViewById(R.id.tv_time_ago);
            ratingBar = itemView.findViewById(R.id.rating_bar);
            tvRatingValue = itemView.findViewById(R.id.tv_rating_value);
            tvComment = itemView.findViewById(R.id.tv_comment);
            performanceBadges = itemView.findViewById(R.id.performance_badges);
        }
    }
}