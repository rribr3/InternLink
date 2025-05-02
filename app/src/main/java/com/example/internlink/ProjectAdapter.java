package com.example.internlink;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder> {

    private List<ProjectManagementActivity.Project> projects;
    private List<ProjectManagementActivity.Project> projectsFull; // For search functionality

    public ProjectAdapter(List<ProjectManagementActivity.Project> projects) {
        this.projects = projects;
        this.projectsFull = new ArrayList<>(projects);
    }

    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_project, parent, false);
        return new ProjectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
        ProjectManagementActivity.Project project = projects.get(position);

        // Set project details
        holder.tvProjectTitle.setText(project.getTitle());
        holder.tvCompanyName.setText(project.getCompanyName());
        holder.tvProjectDescription.setText(project.getDescription());

        // Set status chip
        holder.chipStatus.setText(project.getStatus());
        setStatusChipStyle(holder.chipStatus, project.getStatus());

        // Set tags
        holder.chipGroupTags.removeAllViews();
        if (project.getTags() != null) {
            for (String tag : project.getTags()) {
                Chip chip = new Chip(holder.itemView.getContext());
                chip.setText(tag);
                chip.setChipBackgroundColorResource(R.color.chip_tag_background);
                holder.chipGroupTags.addView(chip);
            }
        }

        // Set click listeners
        holder.btnApprove.setOnClickListener(v -> {
            project.setStatus("approved");
            notifyItemChanged(position);
            Toast.makeText(holder.itemView.getContext(), "Project approved locally", Toast.LENGTH_SHORT).show();
        });

        holder.btnReject.setOnClickListener(v -> {
            project.setStatus("rejected");
            notifyItemChanged(position);
            Toast.makeText(holder.itemView.getContext(), "Project rejected locally", Toast.LENGTH_SHORT).show();
        });

        holder.btnTags.setOnClickListener(v -> editTags(project));
    }

    private void setStatusChipStyle(Chip chip, String status) {
        int bgColor = R.color.status_pending;
        int strokeColor = R.color.status_pending_stroke;

        switch (status.toLowerCase()) {
            case "approved":
                bgColor = R.color.status_approved;
                strokeColor = R.color.status_approved_stroke;
                break;
            case "rejected":
                bgColor = R.color.status_rejected;
                strokeColor = R.color.status_rejected_stroke;
                break;
            case "in progress":
                bgColor = R.color.status_in_progress;
                strokeColor = R.color.status_in_progress_stroke;
                break;
            case "completed":
                bgColor = R.color.status_completed;
                strokeColor = R.color.status_completed_stroke;
                break;
        }

        chip.setChipBackgroundColorResource(bgColor);
        chip.setChipStrokeColorResource(strokeColor);
        chip.setChipStrokeWidth(1.5f);
    }

    @Override
    public int getItemCount() {
        return projects.size();
    }

    public void filter(String text) {
        projects.clear();
        if (text.isEmpty()) {
            projects.addAll(projectsFull);
        } else {
            String filterPattern = text.toLowerCase().trim();
            for (ProjectManagementActivity.Project project : projectsFull) {
                if (project.getTitle().toLowerCase().contains(filterPattern) ||
                        project.getCompanyName().toLowerCase().contains(filterPattern) ||
                        project.getDescription().toLowerCase().contains(filterPattern) ||
                        (project.getTags() != null && containsTag(project.getTags(), filterPattern))) {
                    projects.add(project);
                }
            }
        }
        notifyDataSetChanged();
    }

    private boolean containsTag(List<String> tags, String filterPattern) {
        for (String tag : tags) {
            if (tag.toLowerCase().contains(filterPattern)) {
                return true;
            }
        }
        return false;
    }

    private void editTags(ProjectManagementActivity.Project project) {
        // Placeholder for editing tags
        Toast.makeText(null, "Edit tags for: " + project.getTitle(), Toast.LENGTH_SHORT).show();
    }

    static class ProjectViewHolder extends RecyclerView.ViewHolder {
        TextView tvProjectTitle, tvCompanyName, tvProjectDescription;
        Chip chipStatus;
        ChipGroup chipGroupTags;
        View btnApprove, btnReject, btnTags;

        ProjectViewHolder(View itemView) {
            super(itemView);
            tvProjectTitle = itemView.findViewById(R.id.tvProjectTitle);
            tvCompanyName = itemView.findViewById(R.id.tvCompanyName);
            tvProjectDescription = itemView.findViewById(R.id.tvProjectDescription);
            chipStatus = itemView.findViewById(R.id.chipStatus);
            chipGroupTags = itemView.findViewById(R.id.chipGroupTags);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnReject = itemView.findViewById(R.id.btnReject);
            btnTags = itemView.findViewById(R.id.btnTags);
        }
    }
}
