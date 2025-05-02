package com.example.internlink;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.RatingBar;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FeedbackAdapter extends RecyclerView.Adapter<FeedbackAdapter.ViewHolder> {

    private final List<FeedbackItem> feedbackList;

    // Constructor to initialize the feedback list
    public FeedbackAdapter(List<FeedbackItem> feedbackList) {
        this.feedbackList = feedbackList;
    }

    @Override
    public FeedbackAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.feedback_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(FeedbackAdapter.ViewHolder holder, int position) {
        FeedbackItem item = feedbackList.get(position);

        // Set message, user type, and rating
        holder.messageText.setText(item.message);
        holder.userTypeText.setText(item.userType);

        // Set the rating using RatingBar
        holder.ratingBar.setRating(item.rating); // RatingBar does not need to be cast to TextView
    }

    @Override
    public int getItemCount() {
        return feedbackList.size();
    }

    // ViewHolder class to bind views
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView messageText, userTypeText; // Only TextViews for message and user type
        RatingBar ratingBar; // RatingBar for displaying the rating

        @SuppressLint("WrongViewCast")
        public ViewHolder(View itemView) {
            super(itemView);

            // Initialize the views
            messageText = itemView.findViewById(R.id.txtFeedbackContent); // TextView for message
            userTypeText = itemView.findViewById(R.id.chipUserType); // TextView for user type
            ratingBar = itemView.findViewById(R.id.ratingBar); // RatingBar for rating
        }
    }
}
