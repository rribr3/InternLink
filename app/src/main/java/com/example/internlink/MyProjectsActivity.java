package com.example.internlink;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
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

    private String currentFilter = "all";
    private MaterialToolbar toolbar;

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

        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MyProjectsAdapter(filteredProjects, project -> {
            Toast.makeText(MyProjectsActivity.this, "Clicked: " + project.getTitle(), Toast.LENGTH_SHORT).show();
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
        PopupMenu popupMenu = new PopupMenu(MyProjectsActivity.this, filterButton);
        popupMenu.getMenu().add("All");
        popupMenu.getMenu().add("Approved");
        popupMenu.getMenu().add("Rejected");
        popupMenu.getMenu().add("Pending");
        popupMenu.getMenu().add("Completed");

        popupMenu.setOnMenuItemClickListener(item -> {
            String filterText = item.getTitle().toString();

            selectedFiltersChipGroup.removeAllViews();
            if (!filterText.equalsIgnoreCase("All")) {
                addFilterChip(filterText);
            }

            switch (filterText.toLowerCase()) {
                case "approved":
                    currentFilter = "approved";
                    break;
                case "rejected":
                    currentFilter = "rejected";
                    break;
                case "pending":
                    currentFilter = "pending";
                    break;
                case "completed":
                    currentFilter = "completed";
                    break;
                default:
                    currentFilter = "all";
                    break;
            }

            String currentQuery = searchInput.getQuery() != null ? searchInput.getQuery().toString() : "";
            filterAndSearchProjects(currentQuery);
            return true;
        });

        popupMenu.show();
    }

    private void addFilterChip(String filterText) {
        Chip chip = new Chip(this);
        chip.setText(filterText);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> {
            selectedFiltersChipGroup.removeView(chip);
            currentFilter = "all";
            String currentQuery = searchInput.getQuery() != null ? searchInput.getQuery().toString() : "";
            filterAndSearchProjects(currentQuery);
        });
        selectedFiltersChipGroup.addView(chip);
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
                                allProjects.add(project);
                            }
                        }

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
        query = query.toLowerCase().trim();
        filteredProjects.clear();

        for (Project p : allProjects) {
            boolean matchesSearch = p.getTitle().toLowerCase().contains(query);
            boolean matchesFilter = currentFilter.equals("all") || p.getStatus().equalsIgnoreCase(currentFilter);

            if (matchesSearch && matchesFilter) {
                filteredProjects.add(p);
            }
        }
        adapter.notifyDataSetChanged();
    }
}
