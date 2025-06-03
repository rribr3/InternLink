package com.example.internlink;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MyProjectsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MaterialButton filterButton;
    private FloatingActionButton fabAddProject;
    private MyProjectsAdapter adapter;
    private List<Project> allProjects = new ArrayList<>();
    private List<Project> filteredProjects = new ArrayList<>();
    private ChipGroup selectedFiltersChipGroup;
    private LinearLayout emptyStateView;

    private final List<String> selectedFilters = new ArrayList<>();

    private MaterialToolbar toolbar;
    private TextView txtProjectsCount;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_projects);

        initViews();
        setupRecyclerView();
        setupClickListeners();
        loadProjects();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recycler_projects);
        filterButton = findViewById(R.id.btn_filter);
        fabAddProject = findViewById(R.id.fab_add_project);
        selectedFiltersChipGroup = findViewById(R.id.selected_filters_chip_group);
        toolbar = findViewById(R.id.topAppBar);
        txtProjectsCount = findViewById(R.id.txt_projects_count);
        emptyStateView = findViewById(R.id.empty_state);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MyProjectsAdapter(filteredProjects, project -> {
            Intent intent = new Intent(MyProjectsActivity.this, ProjectDetailsActivity.class);
            intent.putExtra("PROJECT_ID", project.getProjectId());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);
    }

    private void setupClickListeners() {
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        fabAddProject.setOnClickListener(v -> {
            Intent intent = new Intent(MyProjectsActivity.this, CreateProject.class);
            startActivity(intent);
        });

        filterButton.setOnClickListener(v -> showFilterPopup());
    }

    private void showFilterPopup() {
        String[] filterOptions = {"approved", "rejected", "pending", "completed"};
        boolean[] checkedItems = new boolean[filterOptions.length];

        // Pre-check previously selected items
        for (int i = 0; i < filterOptions.length; i++) {
            checkedItems[i] = selectedFilters.contains(filterOptions[i]);
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Select Filters")
                .setMultiChoiceItems(filterOptions, checkedItems, (dialog, which, isChecked) -> {
                    String selected = filterOptions[which];
                    if (isChecked) {
                        if (!selectedFilters.contains(selected)) selectedFilters.add(selected);
                    } else {
                        selectedFilters.remove(selected);
                    }
                })
                .setPositiveButton("Apply", (dialog, which) -> {
                    selectedFiltersChipGroup.removeAllViews();
                    for (String filter : selectedFilters) {
                        addFilterChip(filter);
                    }
                    applyFilters();
                })
                .setNegativeButton("Clear All", (dialog, which) -> {
                    selectedFilters.clear();
                    selectedFiltersChipGroup.removeAllViews();
                    applyFilters();
                })
                .show();
    }

    private void addFilterChip(String filterText) {
        Chip chip = new Chip(this);
        chip.setText(capitalize(filterText));
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> {
            selectedFilters.remove(filterText);
            selectedFiltersChipGroup.removeView(chip);
            applyFilters();
        });
        selectedFiltersChipGroup.addView(chip);
    }

    private String capitalize(String input) {
        if (input == null || input.length() == 0) return input;
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }

    private void loadProjects() {
        String companyId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("projects");

        ref.orderByChild("companyId").equalTo(companyId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        allProjects.clear();
                        for (DataSnapshot snap : snapshot.getChildren()) {
                            Project project = snap.getValue(Project.class);
                            if (project != null) {
                                project.setProjectId(snap.getKey());
                                project.setApplicants((int) snap.child("applicants").getChildrenCount());
                                allProjects.add(project);
                            }
                        }

                        // Apply current filters
                        applyFilters();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(MyProjectsActivity.this, "Failed to load projects", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void applyFilters() {
        filteredProjects.clear();

        // If no filters selected, show all projects
        if (selectedFilters.isEmpty()) {
            filteredProjects.addAll(allProjects);
        } else {
            // Filter projects based on selected status filters
            for (Project project : allProjects) {
                if (selectedFilters.contains(project.getStatus())) {
                    filteredProjects.add(project);
                }
            }
        }

        updateUI();
    }

    private void updateUI() {
        adapter.notifyDataSetChanged();
        txtProjectsCount.setText(filteredProjects.size() + " projects found");

        // Show/hide empty state
        if (filteredProjects.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyStateView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateView.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload projects when returning to this activity
        loadProjects();
    }
}