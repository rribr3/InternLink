package com.example.internlink;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class SavedProjectsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProjectVerticalAdapter adapter;
    private List<Project> savedProjects;
    private ProgressBar progressBar;
    private LinearLayout emptyStateLayout;
    private SwipeRefreshLayout swipeRefreshLayout;
    private String studentId;
    private EditText searchEditText;
    private ImageButton clearSearchButton;
    private List<Project> allSavedProjects; // Store all projects for filtering
    private TextView emptyStateTitle;
    private TextView emptyStateMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_projects);

        // Initialize views
        recyclerView = findViewById(R.id.rv_saved_projects);
        progressBar = findViewById(R.id.progress_bar);
        emptyStateLayout = findViewById(R.id.empty_state_layout);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        searchEditText = findViewById(R.id.search_edit_text);
        clearSearchButton = findViewById(R.id.clear_search_button);
        emptyStateTitle = findViewById(R.id.empty_state_title);
        emptyStateMessage = findViewById(R.id.empty_state_message);

        savedProjects = new ArrayList<>();
        allSavedProjects = new ArrayList<>();

        setupSearch();

        // Set up back button
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> onBackPressed());

        // Initialize Firebase
        studentId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        savedProjects = new ArrayList<>();

        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProjectVerticalAdapter(savedProjects, project -> {
            // Handle project click - navigate to project details
            Intent intent = new Intent(this, ApplyNowActivity.class);
            intent.putExtra("PROJECT_ID", project.getProjectId());
            startActivity(intent);
        }, false);
        recyclerView.setAdapter(adapter);

        // Set up SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(this::loadSavedProjects);

        // Load saved projects
        loadSavedProjects();
    }
    private void setupSearch() {
        // Setup clear button
        clearSearchButton.setOnClickListener(v -> {
            searchEditText.setText("");
            clearSearchButton.setVisibility(View.GONE);
            filterProjects("");
        });

        // Setup search input listener
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                clearSearchButton.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                filterProjects(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Setup keyboard search action
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                filterProjects(searchEditText.getText().toString());
                return true;
            }
            return false;
        });
    }

    private void filterProjects(String query) {
        query = query.toLowerCase().trim();

        if (query.isEmpty()) {
            savedProjects.clear();
            savedProjects.addAll(allSavedProjects);
        } else {
            List<Project> filteredList = new ArrayList<>();

            for (Project project : allSavedProjects) {
                if (matchesSearchCriteria(project, query)) {
                    filteredList.add(project);
                }
            }

            savedProjects.clear();
            savedProjects.addAll(filteredList);
        }

        adapter.notifyDataSetChanged();
        updateEmptyState(query);
    }

    private boolean matchesSearchCriteria(Project project, String query) {
        // Check title
        if (project.getTitle() != null &&
                project.getTitle().toLowerCase().contains(query)) {
            return true;
        }

        // Check description
        if (project.getDescription() != null &&
                project.getDescription().toLowerCase().contains(query)) {
            return true;
        }

        // Check category
        if (project.getCategory() != null &&
                project.getCategory().toLowerCase().contains(query)) {
            return true;
        }

        // Check skills
        if (project.getSkills() != null) {
            for (String skill : project.getSkills()) {
                if (skill.toLowerCase().contains(query)) {
                    return true;
                }
            }
        }

        return false;
    }

    private void updateEmptyState(String query) {
        if (savedProjects.isEmpty()) {
            if (query.isEmpty()) {
                emptyStateTitle.setText("No Saved Projects");
                emptyStateMessage.setText("Projects you save will appear here");
            } else {
                emptyStateTitle.setText("No Results Found");
                emptyStateMessage.setText("Try different keywords or filters");
            }
            emptyStateLayout.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }


    private void loadSavedProjects() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.GONE);

        DatabaseReference savedRef = FirebaseDatabase.getInstance()
                .getReference("saved_projects")
                .child(studentId);

        savedRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                savedProjects.clear();
                allSavedProjects = new ArrayList<>(); // Clear the full list
                List<String> projectIds = new ArrayList<>();

                // First, get all saved project IDs
                for (DataSnapshot savedSnap : snapshot.getChildren()) {
                    String projectId = savedSnap.child("projectId").getValue(String.class);
                    if (projectId != null) {
                        projectIds.add(projectId);
                    }
                }

                if (projectIds.isEmpty()) {
                    showEmptyState();
                    return;
                }

                // Then, fetch project details for each saved project
                DatabaseReference projectsRef = FirebaseDatabase.getInstance().getReference("projects");
                int[] loadedCount = {0};

                for (String projectId : projectIds) {
                    projectsRef.child(projectId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot projectSnap) {
                            loadedCount[0]++;

                            if (projectSnap.exists()) {
                                Project project = projectSnap.getValue(Project.class);
                                if (project != null) {
                                    project.setProjectId(projectSnap.getKey());
                                    savedProjects.add(project);
                                    allSavedProjects.add(project); // Add to full list
                                }
                            }

                            // Check if all projects have been loaded
                            if (loadedCount[0] == projectIds.size()) {
                                if (savedProjects.isEmpty()) {
                                    showEmptyState();
                                } else {
                                    // Apply any existing search filter
                                    String currentQuery = searchEditText.getText().toString();
                                    if (!currentQuery.isEmpty()) {
                                        filterProjects(currentQuery);
                                    } else {
                                        adapter.notifyDataSetChanged();
                                    }
                                    recyclerView.setVisibility(View.VISIBLE);
                                    emptyStateLayout.setVisibility(View.GONE);
                                }
                                progressBar.setVisibility(View.GONE);
                                swipeRefreshLayout.setRefreshing(false);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            loadedCount[0]++;
                            checkLoadingComplete(loadedCount[0], projectIds.size());
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SavedProjectsActivity.this, "Error loading saved projects", Toast.LENGTH_SHORT).show();
                showEmptyState();
            }
        });
    }

    private void showEmptyState() {
        progressBar.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.VISIBLE);
        swipeRefreshLayout.setRefreshing(false);
    }

    private void checkLoadingComplete(int loadedCount, int totalCount) {
        if (loadedCount == totalCount) {
            if (savedProjects.isEmpty()) {
                showEmptyState();
            } else {
                adapter.notifyDataSetChanged();
                recyclerView.setVisibility(View.VISIBLE);
                emptyStateLayout.setVisibility(View.GONE);
            }
            progressBar.setVisibility(View.GONE);
            swipeRefreshLayout.setRefreshing(false);
        }
    }
}