package com.example.internlink;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CompanyProjectsAdapter extends RecyclerView.Adapter<CompanyProjectsAdapter.ProjectViewHolder> {

    private List<EmployerProject> projects;

    public CompanyProjectsAdapter(List<EmployerProject> projects) {
        this.projects = projects;
    }

    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_company_project, parent, false);
        return new ProjectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
        EmployerProject project = projects.get(position);
        holder.projectTitle.setText(project.getTitle());
        holder.applicantsCount.setText(String.valueOf(project.getApplicantsCount()));
        holder.positionsCount.setText(String.valueOf(project.getPositionsCount()));
        holder.projectIcon.setImageResource(project.getIconResId());
    }

    @Override
    public int getItemCount() {
        return projects.size();
    }

    public static class ProjectViewHolder extends RecyclerView.ViewHolder {
        TextView projectTitle;
        TextView applicantsCount;
        TextView positionsCount;
        ImageView projectIcon;

        public ProjectViewHolder(@NonNull View itemView) {
            super(itemView);
            projectTitle = itemView.findViewById(R.id.project_title);
            applicantsCount = itemView.findViewById(R.id.applicants_count);
            positionsCount = itemView.findViewById(R.id.positions_count);
            projectIcon = itemView.findViewById(R.id.project_icon);
        }
    }
}