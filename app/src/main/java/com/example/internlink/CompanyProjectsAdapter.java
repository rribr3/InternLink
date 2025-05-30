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

public class CompanyProjectsAdapter extends RecyclerView.Adapter<CompanyProjectsAdapter.ProjectViewHolder> {

    private List<EmployerProject> projects;

    public CompanyProjectsAdapter(List<EmployerProject> projects) {
        this.projects = projects != null ? projects : new ArrayList<>();
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
        if (position < projects.size()) {
            EmployerProject project = projects.get(position);
            holder.projectTitle.setText(project.getTitle());
            holder.applicantsCount.setText(String.valueOf(project.getApplicantsCount()));
            holder.positionsCount.setText(String.valueOf(project.getPositionsCount()));
            holder.projectIcon.setImageResource(project.getIconResId());
        }
    }

    @Override
    public int getItemCount() {
        return projects != null ? projects.size() : 0;
    }

    // ✅ NEW: Update projects list and refresh the RecyclerView
    public void updateProjects(List<EmployerProject> newProjects) {
        if (newProjects != null) {
            this.projects.clear();
            this.projects.addAll(newProjects);
            notifyDataSetChanged();
        }
    }

    // ✅ NEW: Clear all data from the adapter
    public void clearData() {
        if (projects != null) {
            projects.clear();
            notifyDataSetChanged();
        }
    }

    // ✅ NEW: Add a single project to the list
    public void addProject(EmployerProject project) {
        if (project != null && projects != null) {
            projects.add(project);
            notifyItemInserted(projects.size() - 1);
        }
    }

    // ✅ NEW: Remove a project at specific position
    public void removeProject(int position) {
        if (projects != null && position >= 0 && position < projects.size()) {
            projects.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, projects.size());
        }
    }

    // ✅ NEW: Update a specific project
    public void updateProject(int position, EmployerProject updatedProject) {
        if (projects != null && position >= 0 && position < projects.size() && updatedProject != null) {
            projects.set(position, updatedProject);
            notifyItemChanged(position);
        }
    }

    // ✅ NEW: Find project by ID and update it
    public void updateProjectById(String projectId, EmployerProject updatedProject) {
        if (projects != null && projectId != null && updatedProject != null) {
            for (int i = 0; i < projects.size(); i++) {
                EmployerProject project = projects.get(i);
                if (project.getProjectId() != null && project.getProjectId().equals(projectId)) {
                    projects.set(i, updatedProject);
                    notifyItemChanged(i);
                    break;
                }
            }
        }
    }

    // ✅ NEW: Remove project by ID
    public void removeProjectById(String projectId) {
        if (projects != null && projectId != null) {
            for (int i = 0; i < projects.size(); i++) {
                EmployerProject project = projects.get(i);
                if (project.getProjectId() != null && project.getProjectId().equals(projectId)) {
                    projects.remove(i);
                    notifyItemRemoved(i);
                    notifyItemRangeChanged(i, projects.size());
                    break;
                }
            }
        }
    }

    // ✅ NEW: Get current projects count
    public int getProjectsCount() {
        return projects != null ? projects.size() : 0;
    }

    // ✅ NEW: Get project at specific position
    public EmployerProject getProject(int position) {
        if (projects != null && position >= 0 && position < projects.size()) {
            return projects.get(position);
        }
        return null;
    }

    // ✅ NEW: Get all projects
    public List<EmployerProject> getAllProjects() {
        return new ArrayList<>(projects);
    }

    // ✅ NEW: Check if adapter is empty
    public boolean isEmpty() {
        return projects == null || projects.isEmpty();
    }

    // ✅ NEW: Refresh specific project's applicant count
    public void updateProjectApplicantCount(String projectId, int newApplicantCount) {
        if (projects != null && projectId != null) {
            for (int i = 0; i < projects.size(); i++) {
                EmployerProject project = projects.get(i);
                if (project.getProjectId() != null && project.getProjectId().equals(projectId)) {
                    project.setApplicantsCount(newApplicantCount);
                    notifyItemChanged(i);
                    break;
                }
            }
        }
    }

    // ✅ NEW: Get project position by ID
    public int getProjectPosition(String projectId) {
        if (projects != null && projectId != null) {
            for (int i = 0; i < projects.size(); i++) {
                EmployerProject project = projects.get(i);
                if (project.getProjectId() != null && project.getProjectId().equals(projectId)) {
                    return i;
                }
            }
        }
        return -1; // Not found
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