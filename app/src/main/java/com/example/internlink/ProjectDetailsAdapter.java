package com.example.internlink;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ProjectDetailsAdapter extends RecyclerView.Adapter<ProjectDetailsAdapter.ViewHolder> {

    private List<EmployerProject> projectList;
    private OnItemClickListener itemClickListener;

    // Interface for item click listener
    public interface OnItemClickListener {
        void onItemClick(EmployerProject project);
    }

    // Constructor
    public ProjectDetailsAdapter(List<EmployerProject> projects, OnItemClickListener listener) {
        this.projectList = projects;
        this.itemClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout for each project item
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_company_vertical, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Get the project at the given position
        EmployerProject project = projectList.get(position);

        // Bind the data to the views
        holder.projectTitle.setText(project.getTitle());
        holder.positionsCount.setText(String.valueOf(project.getPositionsCount()));
        holder.applicantsCount.setText(String.valueOf(project.getApplicantsCount()));

        // Set onClickListener for the "View Details" button
        holder.viewDetailsButton.setOnClickListener(v -> {
            // Call the listener's method when the button is clicked
            itemClickListener.onItemClick(project);
        });
    }

    @Override
    public int getItemCount() {
        // Return the size of the project list
        return projectList.size();
    }

    // ViewHolder class that holds the views
    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView projectTitle, positionsCount, applicantsCount;
        Button viewDetailsButton;

        // Constructor for ViewHolder
        public ViewHolder(View itemView) {
            super(itemView);

            // Initialize the views
            projectTitle = itemView.findViewById(R.id.project_title);
            positionsCount = itemView.findViewById(R.id.positions_count);
            applicantsCount = itemView.findViewById(R.id.applicants_count);
            viewDetailsButton = itemView.findViewById(R.id.view_details_button); // The button to view details
        }
    }
}