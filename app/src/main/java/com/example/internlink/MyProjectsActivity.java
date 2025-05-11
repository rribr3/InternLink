package com.example.internlink;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
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
    private Spinner filterButton;
    private MyProjectsAdapter adapter;
    private List<Project> allProjects = new ArrayList<>();
    private List<Project> filteredProjects = new ArrayList<>();

    private String currentFilter = "all";
    private MaterialToolbar toolbar;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_projects);

        recyclerView = findViewById(R.id.recycler_projects);
        searchInput = findViewById(R.id.search_view);
        filterButton = findViewById(R.id.filter_spinner);
        toolbar = findViewById(R.id.topAppBar);

        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MyProjectsAdapter(filteredProjects, project -> {
            // Handle view details click here
            Toast.makeText(MyProjectsActivity.this, "Clicked: " + project.getTitle(), Toast.LENGTH_SHORT).show();
        });
        recyclerView.setAdapter(adapter);

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

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[] {"All", "Approved", "Rejected", "Pending", "Completed"}
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filterButton.setAdapter(adapter);
        filterButton.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0: currentFilter = "all"; break;
                    case 1: currentFilter = "approved"; break;
                    case 2: currentFilter = "rejected"; break;
                    case 3: currentFilter = "pending"; break;
                    case 4: currentFilter = "completed"; break;
                }

                String currentQuery = "";
                if (searchInput != null && searchInput.getQuery() != null) {
                    currentQuery = searchInput.getQuery().toString();
                }

                filterAndSearchProjects(currentQuery);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

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

                        // Get the current query from the SearchView (if not null)
                        String currentQuery = "";
                        if (searchInput != null && searchInput.getQuery() != null) {
                            currentQuery = searchInput.getQuery().toString();
                        }

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


    private void showFilterMenu() {
        PopupMenu menu = new PopupMenu(this, filterButton);
        menu.getMenu().add(Menu.NONE, 0, 0, "All");
        menu.getMenu().add(Menu.NONE, 1, 1, "Approved");
        menu.getMenu().add(Menu.NONE, 2, 2, "Rejected");
        menu.getMenu().add(Menu.NONE, 3, 3, "Pending");
        menu.getMenu().add(Menu.NONE, 4, 4, "Completed");

        menu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 0: currentFilter = "all"; break;
                case 1: currentFilter = "approved"; break;
                case 2: currentFilter = "rejected"; break;
                case 3: currentFilter = "pending"; break;
                case 4: currentFilter = "completed"; break;
            }

            String currentQuery = "";
            if (searchInput != null && searchInput.getQuery() != null) {
                currentQuery = searchInput.getQuery().toString();
            }

            filterAndSearchProjects(currentQuery);
            return true;
        });

        menu.show();
    }

}
