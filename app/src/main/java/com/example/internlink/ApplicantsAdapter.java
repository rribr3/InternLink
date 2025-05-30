package com.example.internlink;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ApplicantsAdapter extends RecyclerView.Adapter<ApplicantsAdapter.ApplicantViewHolder> {

    private List<Applicant> applicants;

    public ApplicantsAdapter(List<Applicant> applicants) {
        this.applicants = applicants != null ? applicants : new ArrayList<>();
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
        if (position < applicants.size()) {
            Applicant applicant = applicants.get(position);
            holder.name.setText(applicant.getName());
            holder.position.setText(applicant.getPosition());
            holder.status.setText(applicant.getStatus());
            holder.profileImage.setImageResource(applicant.getProfileImageResId());
        }
    }

    @Override
    public int getItemCount() {
        return applicants != null ? applicants.size() : 0;
    }

    // ✅ NEW: Update applicants list and refresh the RecyclerView
    public void updateApplicants(List<Applicant> newApplicants) {
        if (newApplicants != null) {
            this.applicants.clear();
            this.applicants.addAll(newApplicants);
            notifyDataSetChanged();
        }
    }

    // ✅ NEW: Clear all data from the adapter
    public void clearData() {
        if (applicants != null) {
            applicants.clear();
            notifyDataSetChanged();
        }
    }

    // ✅ NEW: Add a single applicant to the list
    public void addApplicant(Applicant applicant) {
        if (applicant != null && applicants != null) {
            applicants.add(applicant);
            notifyItemInserted(applicants.size() - 1);
        }
    }

    // ✅ NEW: Remove an applicant at specific position
    public void removeApplicant(int position) {
        if (applicants != null && position >= 0 && position < applicants.size()) {
            applicants.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, applicants.size());
        }
    }

    // ✅ NEW: Update applicant at specific position
    public void updateApplicant(int position, Applicant updatedApplicant) {
        if (applicants != null && position >= 0 && position < applicants.size() && updatedApplicant != null) {
            applicants.set(position, updatedApplicant);
            notifyItemChanged(position);
        }
    }

    // ✅ NEW: Update applicant status by user ID
    public void updateApplicantStatus(String userId, String newStatus) {
        if (applicants != null && userId != null && newStatus != null) {
            for (int i = 0; i < applicants.size(); i++) {
                Applicant applicant = applicants.get(i);
                if (applicant.getUserId() != null && applicant.getUserId().equals(userId)) {
                    applicant.setStatus(newStatus);
                    notifyItemChanged(i);
                    break;
                }
            }
        }
    }

    // ✅ NEW: Find and update applicant by user ID
    public void updateApplicantById(String userId, Applicant updatedApplicant) {
        if (applicants != null && userId != null && updatedApplicant != null) {
            for (int i = 0; i < applicants.size(); i++) {
                Applicant applicant = applicants.get(i);
                if (applicant.getUserId() != null && applicant.getUserId().equals(userId)) {
                    applicants.set(i, updatedApplicant);
                    notifyItemChanged(i);
                    break;
                }
            }
        }
    }

    // ✅ NEW: Remove applicant by user ID
    public void removeApplicantById(String userId) {
        if (applicants != null && userId != null) {
            for (int i = 0; i < applicants.size(); i++) {
                Applicant applicant = applicants.get(i);
                if (applicant.getUserId() != null && applicant.getUserId().equals(userId)) {
                    applicants.remove(i);
                    notifyItemRemoved(i);
                    notifyItemRangeChanged(i, applicants.size());
                    break;
                }
            }
        }
    }

    // ✅ NEW: Get current applicants count
    public int getApplicantsCount() {
        return applicants != null ? applicants.size() : 0;
    }

    // ✅ NEW: Get applicant at specific position
    public Applicant getApplicant(int position) {
        if (applicants != null && position >= 0 && position < applicants.size()) {
            return applicants.get(position);
        }
        return null;
    }

    // ✅ NEW: Get all applicants
    public List<Applicant> getAllApplicants() {
        return new ArrayList<>(applicants);
    }

    // ✅ NEW: Check if adapter is empty
    public boolean isEmpty() {
        return applicants == null || applicants.isEmpty();
    }

    // ✅ NEW: Get applicant position by user ID
    public int getApplicantPosition(String userId) {
        if (applicants != null && userId != null) {
            for (int i = 0; i < applicants.size(); i++) {
                Applicant applicant = applicants.get(i);
                if (applicant.getUserId() != null && applicant.getUserId().equals(userId)) {
                    return i;
                }
            }
        }
        return -1; // Not found
    }

    // ✅ NEW: Filter applicants by status
    public List<Applicant> getApplicantsByStatus(String status) {
        List<Applicant> filteredApplicants = new ArrayList<>();
        if (applicants != null && status != null) {
            for (Applicant applicant : applicants) {
                if (status.equals(applicant.getStatus())) {
                    filteredApplicants.add(applicant);
                }
            }
        }
        return filteredApplicants;
    }

    // ✅ NEW: Get applicant by user ID
    public Applicant getApplicantById(String userId) {
        if (applicants != null && userId != null) {
            for (Applicant applicant : applicants) {
                if (applicant.getUserId() != null && applicant.getUserId().equals(userId)) {
                    return applicant;
                }
            }
        }
        return null;
    }

    // ✅ NEW: Insert applicant at specific position
    public void insertApplicant(int position, Applicant applicant) {
        if (applicants != null && applicant != null && position >= 0 && position <= applicants.size()) {
            applicants.add(position, applicant);
            notifyItemInserted(position);
        }
    }

    // ✅ NEW: Move applicant from one position to another
    public void moveApplicant(int fromPosition, int toPosition) {
        if (applicants != null && fromPosition >= 0 && fromPosition < applicants.size()
                && toPosition >= 0 && toPosition < applicants.size()) {
            Applicant applicant = applicants.remove(fromPosition);
            applicants.add(toPosition, applicant);
            notifyItemMoved(fromPosition, toPosition);
        }
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