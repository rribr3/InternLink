package com.example.internlink;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.SearchView;
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
    private SearchView searchInput;
    private MaterialButton filterButton;
    private FloatingActionButton fabAddProject;
    private MyProjectsAdapter adapter;
    private List<Project> allProjects = new ArrayList<>();
    private List<Project> filteredProjects = new ArrayList<>();
    private ChipGroup selectedFiltersChipGroup;

    private final List<String> selectedFilters = new ArrayList<>();

    private MaterialToolbar toolbar;
    private TextView txtProjectsCount;


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_projects);

        recyclerView = findViewById(R.id.recycler_projects);
        searchInput = findViewById(R.id.search_view);
        filterButton = findViewById(R.id.btn_filter);
        fabAddProject = findViewById(R.id.fab_add_project);
        selectedFiltersChipGroup = findViewById(R.id.selected_filters_chip_group);
        toolbar = findViewById(R.id.topAppBar);
        txtProjectsCount = findViewById(R.id.txt_projects_count);

        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MyProjectsAdapter(filteredProjects, project -> {
            Intent intent = new Intent(MyProjectsActivity.this, ProjectDetailsActivity.class);
            intent.putExtra("PROJECT_ID", project.getProjectId());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        fabAddProject.setOnClickListener(v -> {
            Intent intent = new Intent(MyProjectsActivity.this, CreateProject.class);
            startActivity(intent);
        });

        loadProjects();

        searchInput.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterAndSearchProjects(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterAndSearchProjects(newText);
                return true;
            }
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
                    filterAndSearchProjects(searchInput.getQuery().toString());
                })
                .setNegativeButton("Clear All", (dialog, which) -> {
                    selectedFilters.clear();
                    selectedFiltersChipGroup.removeAllViews();
                    filterAndSearchProjects(searchInput.getQuery().toString());
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
            filterAndSearchProjects(searchInput.getQuery().toString());
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

                        // Set project count text
                        int projectCount = allProjects.size();
                        txtProjectsCount.setText(projectCount + " projects found");

                        // Apply filters & search
                        String currentQuery = searchInput.getQuery() != null ? searchInput.getQuery().toString() : "";
                        filterAndSearchProjects(currentQuery);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(MyProjectsActivity.this, "Failed to load projects", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void filterAndSearchProjects(String query) {
        txtProjectsCount.setText("Loading...");
        query = query.toLowerCase().trim();
        filteredProjects.clear();

        String companyId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("projects");

        if (selectedFilters.isEmpty()) {
            // No filter selected, load all company projects
            String finalQuery = query;
            ref.orderByChild("companyId").equalTo(companyId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot snap : snapshot.getChildren()) {
                                Project project = snap.getValue(Project.class);
                                if (project != null && project.getTitle().toLowerCase().contains(finalQuery)) {
                                    project.setProjectId(snap.getKey());
                                    project.setApplicants((int) snap.child("applicants").getChildrenCount());
                                    filteredProjects.add(project);
                                }
                            }
                            updateUIAfterFilter();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(MyProjectsActivity.this, "Failed to load projects", Toast.LENGTH_SHORT).show();
                        }
                    });

        } else {
            // Filter selected: we need to query each status separately
            List<Project> tempList = new ArrayList<>();
            List<String> filters = new ArrayList<>(selectedFilters);
            final int[] completedQueries = {0};

            for (String status : filters) {
                String finalQuery1 = query;
                ref.orderByChild("status").equalTo(status)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                for (DataSnapshot snap : snapshot.getChildren()) {
                                    Project project = snap.getValue(Project.class);
                                    if (project != null &&
                                            companyId.equals(project.getCompanyId()) &&
                                            project.getTitle().toLowerCase().contains(finalQuery1)) {
                                        project.setProjectId(snap.getKey());
                                        project.setApplicants((int) snap.child("applicants").getChildrenCount());

                                        // Prevent duplicates
                                        boolean alreadyExists = false;
                                        for (Project p : tempList) {
                                            if (p.getProjectId().equals(project.getProjectId())) {
                                                alreadyExists = true;
                                                break;
                                            }
                                        }
                                        if (!alreadyExists) {
                                            tempList.add(project);
                                        }
                                    }
                                }

                                // When all queries are done
                                completedQueries[0]++;
                                if (completedQueries[0] == filters.size()) {
                                    filteredProjects.clear();
                                    filteredProjects.addAll(tempList);
                                    updateUIAfterFilter();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Toast.makeText(MyProjectsActivity.this, "Failed to filter by " + status, Toast.LENGTH_SHORT).show();
                                completedQueries[0]++;
                                if (completedQueries[0] == filters.size()) {
                                    filteredProjects.clear();
                                    filteredProjects.addAll(tempList);
                                    updateUIAfterFilter();
                                }
                            }
                        });
            }
        }
    }

    private void updateUIAfterFilter() {
        adapter.notifyDataSetChanged();
        txtProjectsCount.setText(filteredProjects.size() + " projects found");
    }



}
