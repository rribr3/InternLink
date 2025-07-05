package com.example.internlink;

import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class ProjectVerticalAdapter extends RecyclerView.Adapter<ProjectVerticalAdapter.ProjectViewHolder> {

    private final List<Project> projects;
    private final OnProjectClickListener listener;

    public interface OnProjectClickListener {
        void onProjectClick(Project project);
    }

    public ProjectVerticalAdapter(List<Project> projects, OnProjectClickListener listener, boolean b) {
        this.projects = projects;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_project_vertical, parent, false);
        return new ProjectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
        Project project = projects.get(position);
        String projectId = project.getProjectId();
        holder.bind(project, projectId);
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
        private final TextView companyName;

        private final Button applyNowButton; // Add this in ViewHolder class

        public ProjectViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.project_title);
            companyLogo = itemView.findViewById(R.id.company_logo);
            skillsChipGroup = itemView.findViewById(R.id.skills_chip_group);
            timeLeftText = itemView.findViewById(R.id.time_left);
            companyName = itemView.findViewById(R.id.company_name);
            applyNowButton = itemView.findViewById(R.id.apply_button); // make sure this is in your XML
        }


        public void bind(Project project, String projectId) {
            titleText.setText(project.getTitle());

            // Clear before setting
            companyName.setText("Loading...");
            timeLeftText.setText("");


            // Load company name from Firebase
            if (project.getCompanyId() != null && !project.getCompanyId().isEmpty()) {
                DatabaseReference companyRef = FirebaseDatabase.getInstance()
                        .getReference("users")
                        .child(project.getCompanyId());

                companyRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Log.d("DebugProjectAdapter", "Snapshot exists: " + snapshot.exists());
                        Log.d("DebugProjectAdapter", "Snapshot key: " + snapshot.getKey());
                        Log.d("DebugProjectAdapter", "Name field: " + snapshot.child("name").getValue());

                        String fetchedName = snapshot.child("name").getValue(String.class);
                        if (fetchedName != null && !fetchedName.trim().isEmpty()) {
                            companyName.setText(fetchedName);
                        } else {
                            companyName.setText("Unknown Company");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("DebugProjectAdapter", "Firebase error: " + error.getMessage());
                        companyName.setText("Company info not available");
                    }
                });

            } else {
                companyName.setText("Company info not available");
            }

            // Calculate time left for application based on startDate
            long now = System.currentTimeMillis();
            long startDate = project.getStartDate();
            long diff = startDate - now;

            if (diff > 0) {
                long daysLeft = diff / (1000 * 60 * 60 * 24);
                if (daysLeft > 1) {
                    timeLeftText.setText(daysLeft + " days left");
                } else if (daysLeft == 1) {
                    timeLeftText.setText("1 day left");
                } else {
                    timeLeftText.setText("Less than a day left");
                }
            } else {
                timeLeftText.setText("Application closed");
            }

            // Set skills chips
            skillsChipGroup.removeAllViews();
            for (String skill : project.getSkills()) {
                Chip chip = new Chip(itemView.getContext());
                chip.setText(skill);
                skillsChipGroup.addView(chip);
            }
            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(itemView.getContext(), ApplyNowActivity.class);
                intent.putExtra("PROJECT_ID", projectId);
                itemView.getContext().startActivity(intent);
            });
            applyNowButton.setOnClickListener(v -> {
                Intent intent = new Intent(itemView.getContext(), ApplyNowActivity.class);
                intent.putExtra("PROJECT_ID", projectId);
                itemView.getContext().startActivity(intent);
            });

        }

    }
}