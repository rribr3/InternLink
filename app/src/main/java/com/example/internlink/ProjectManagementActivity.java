package com.example.internlink;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ProjectManagementActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProjectAdapter adapter;
    private List<Project> projectList;
    private DatabaseReference projectsRef;
    private ChipGroup chipGroup;
    private ExtendedFloatingActionButton fabSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_management);

        // Initialize Firebase
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        projectsRef = database.getReference("projects");

        // Initialize views
        recyclerView = findViewById(R.id.recyclerView);
        chipGroup = findViewById(R.id.chipGroup);
        fabSearch = findViewById(R.id.fabSearch);

        // Setup toolbar
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize project list and adapter
        projectList = new ArrayList<>();
        adapter = new ProjectAdapter(projectList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Setup filter chips
        setupFilterChips();

        // Load initial projects
        loadProjects("all");

        // Search button click listener
        fabSearch.setOnClickListener(v -> showSearchDialog());
    }

    private void setupFilterChips() {
        chipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String status = "all";
            if (checkedId == R.id.chipPending) status = "pending";
            else if (checkedId == R.id.chipApproved) status = "approved";
            else if (checkedId == R.id.chipRejected) status = "rejected";
            else if (checkedId == R.id.chipInProgress) status = "in progress";
            else if (checkedId == R.id.chipCompleted) status = "completed";
            loadProjects(status);
        });

        // Select "All" chip by default
        Chip chipAll = findViewById(R.id.chipAll);
        chipAll.setChecked(true);
    }

    private void loadProjects(String status) {
        projectsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                projectList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Project project = dataSnapshot.getValue(Project.class);
                    if (project != null) {
                        project.setId(dataSnapshot.getKey());
                        if (status.equals("all") || project.getStatus().equalsIgnoreCase(status)) {
                            projectList.add(project);
                        }
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ProjectManagementActivity.this,
                        "Failed to load projects", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showSearchDialog() {
        // Implement your search dialog here
        Toast.makeText(this, "Search functionality will be implemented here", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.project_management_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Project Adapter
    private class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder> {

        private List<Project> projects;

        public ProjectAdapter(List<Project> projects) {
            this.projects = projects;
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
            Project project = projects.get(position);

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
            holder.btnApprove.setOnClickListener(v -> approveProject(project));
            holder.btnReject.setOnClickListener(v -> rejectProject(project));
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

        class ProjectViewHolder extends RecyclerView.ViewHolder {
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

    private void approveProject(Project project) {
        projectsRef.child(project.getId()).child("status").setValue("approved")
                .addOnSuccessListener(aVoid -> showToast("Project approved"))
                .addOnFailureListener(e -> showToast("Approval failed"));
    }

    private void rejectProject(Project project) {
        projectsRef.child(project.getId()).child("status").setValue("rejected")
                .addOnSuccessListener(aVoid -> showToast("Project rejected"))
                .addOnFailureListener(e -> showToast("Rejection failed"));
    }

    private void editTags(Project project) {
        // Implement tag editing dialog
        Toast.makeText(this, "Tag editing will be implemented here", Toast.LENGTH_SHORT).show();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // Project model class
    public static class Project {
        private String id;
        private String title;
        private String companyName;
        private String description;
        private String status;
        private List<String> tags;

        public Project() {
            // Default constructor required for Firebase
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getCompanyName() {
            return companyName;
        }

        public void setCompanyName(String companyName) {
            this.companyName = companyName;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public List<String> getTags() {
            return tags;
        }

        public void setTags(List<String> tags) {
            this.tags = tags;
        }
    }
}