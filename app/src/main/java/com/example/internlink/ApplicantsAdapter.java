package com.example.internlink;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ApplicantsAdapter extends RecyclerView.Adapter<ApplicantsAdapter.ApplicantViewHolder> {

    private List<Applicant> applicants;

    public ApplicantsAdapter(List<Applicant> applicants) {
        this.applicants = applicants;
    }

    @NonNull
    @Override
    public ApplicantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_applicant, parent, false);
        return new ApplicantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ApplicantViewHolder holder, int position) {
        Applicant applicant = applicants.get(position);
        holder.name.setText(applicant.getName());
        holder.position.setText(applicant.getPosition());
        holder.status.setText(applicant.getStatus());
        holder.profileImage.setImageResource(applicant.getProfileImageResId());
    }

    @Override
    public int getItemCount() {
        return applicants.size();
    }

    public static class ApplicantViewHolder extends RecyclerView.ViewHolder {
        TextView name, position, status;
        ImageView profileImage;

        public ApplicantViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.applicant_name);
            position = itemView.findViewById(R.id.applicant_position);
            status = itemView.findViewById(R.id.applicant_status);
            profileImage = itemView.findViewById(R.id.applicant_profile_image);
        }
    }
}