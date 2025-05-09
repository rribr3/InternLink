package com.example.internlink;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.List;

public class ProjectAdapterHome extends RecyclerView.Adapter<ProjectAdapterHome.ProjectViewHolder> {

    private final List<Project> projects;
    private final OnProjectClickListener listener;

    public interface OnProjectClickListener {
        void onProjectClick(Project project);
    }

    public ProjectAdapterHome(List<Project> projects, OnProjectClickListener listener, boolean b) {
        this.projects = projects;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_project_home, parent, false);
        return new ProjectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
        Project project = projects.get(position);
        holder.bind(project);
        holder.itemView.setOnClickListener(v -> listener.onProjectClick(project));
    }

    @Override
    public int getItemCount() {
        return projects.size();
    }

    static class ProjectViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleText;
        private final ImageView companyLogo;
        private final ChipGroup skillsChipGroup;
        private final TextView timeLeftText;

        public ProjectViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.project_title);
            companyLogo = itemView.findViewById(R.id.company_logo);
            skillsChipGroup = itemView.findViewById(R.id.skills_chip_group);
            timeLeftText = itemView.findViewById(R.id.time_left);
        }

        public void bind(Project project) {
            titleText.setText(project.getTitle());
            //companyLogo.setImageResource(project.getCompanyLogo());
            //timeLeftText.setText(project.getTimeLeft());

            skillsChipGroup.removeAllViews();
            for (String skill : project.getSkills()) {
                Chip chip = new Chip(itemView.getContext());
                chip.setText(skill);

                skillsChipGroup.addView(chip);
            }
        }
    }
}