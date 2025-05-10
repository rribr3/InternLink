package com.example.internlink;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MyProjectsAdapter extends RecyclerView.Adapter<MyProjectsAdapter.ProjectViewHolder> {

    public interface OnViewDetailsClickListener {
        void onViewDetailsClick(Project project);
    }

    private List<Project> projectList;
    private OnViewDetailsClickListener listener = null;

    public MyProjectsAdapter(List<Project> projectList) {
        this.projectList = projectList;
        this.listener = listener;
    }

    public void updateList(List<Project> newList) {
        this.projectList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_company_vertical, parent, false);
        return new ProjectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
        Project project = projectList.get(position);
        holder.bind(project);
    }

    @Override
    public int getItemCount() {
        return projectList.size();
    }

    class ProjectViewHolder extends RecyclerView.ViewHolder {

        private final TextView titleText, positionsText, applicantsText;
        private final Button viewDetailsButton;

        public ProjectViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.project_title);
            positionsText = itemView.findViewById(R.id.positions_count);
            applicantsText = itemView.findViewById(R.id.applicants_count);
            viewDetailsButton = itemView.findViewById(R.id.view_details_button);
        }

        public void bind(Project project) {
            titleText.setText(project.getTitle());
            positionsText.setText(String.valueOf(project.getStudentsRequired()));
            applicantsText.setText(String.valueOf(project.getAmount()));
            viewDetailsButton.setOnClickListener(v -> listener.onViewDetailsClick(project));
        }
    }
}
