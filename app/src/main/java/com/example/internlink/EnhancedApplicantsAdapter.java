package com.example.internlink;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class EnhancedApplicantsAdapter extends RecyclerView.Adapter<EnhancedApplicantsAdapter.ApplicantViewHolder> {

    private List<Applicant> applicants;
    private OnApplicantActionListener listener;

    public interface OnApplicantActionListener {
        void onViewProfile(Applicant applicant);
        void onScheduleInterview(Applicant applicant);
        void onChat(Applicant applicant);
        void onMoreOptions(Applicant applicant, View anchorView);  // Add View parameter
    }

    public EnhancedApplicantsAdapter(List<Applicant> applicants, OnApplicantActionListener listener) {
        this.applicants = applicants;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ApplicantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_applicant_enhanced, parent, false);
        return new ApplicantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ApplicantViewHolder holder, int position) {
        Applicant applicant = applicants.get(position);

        holder.applicantName.setText(applicant.getName());
        holder.applicantPosition.setText(applicant.getPosition());
        holder.applicantStatus.setText(applicant.getStatus());
        holder.profileImage.setImageResource(applicant.getProfileImageResId());

        if (applicant.hasQuizGrade()) {
            holder.quizGradeLayout.setVisibility(View.VISIBLE);
            int grade = applicant.getQuizGrade();
            holder.quizGradeText.setText("Quiz: " + grade + "%");

            // Color code the grade
            if (grade >= 80) {
                holder.quizGradeText.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.green));
            } else if (grade >= 50) {
                holder.quizGradeText.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.orange));
            } else {
                holder.quizGradeText.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.red));
            }
        } else {
            holder.quizGradeLayout.setVisibility(View.GONE);
        }

        // Set click listeners for action buttons
        holder.btnViewProfile.setOnClickListener(v -> {
            if (listener != null) listener.onViewProfile(applicant);
        });

        holder.btnScheduleInterview.setOnClickListener(v -> {
            if (listener != null) listener.onScheduleInterview(applicant);
        });

        holder.btnChat.setOnClickListener(v -> {
            if (listener != null) listener.onChat(applicant);
        });

        holder.btnMoreOptions.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMoreOptions(applicant, holder.btnMoreOptions); // Use the anchorView for PopupMenu
            }
        });

    }

    @Override
    public int getItemCount() {
        return applicants.size();
    }

    public static class ApplicantViewHolder extends RecyclerView.ViewHolder {
        TextView applicantName, applicantPosition, applicantStatus, quizGradeText;
        ImageView profileImage;
        ImageView btnViewProfile, btnScheduleInterview, btnChat, btnMoreOptions;
        LinearLayout quizGradeLayout;

        public ApplicantViewHolder(@NonNull View itemView) {
            super(itemView);
            applicantName = itemView.findViewById(R.id.applicant_name);
            applicantPosition = itemView.findViewById(R.id.applicant_position);
            applicantStatus = itemView.findViewById(R.id.applicant_status);
            profileImage = itemView.findViewById(R.id.applicant_profile_image);
            btnViewProfile = itemView.findViewById(R.id.btn_view_profile);
            btnScheduleInterview = itemView.findViewById(R.id.btn_schedule_interview);
            btnChat = itemView.findViewById(R.id.btn_chat);
            btnMoreOptions = itemView.findViewById(R.id.btn_more_options);
            quizGradeText = itemView.findViewById(R.id.quiz_grade_text);
            quizGradeLayout = itemView.findViewById(R.id.quiz_grade_layout);
        }
    }
}