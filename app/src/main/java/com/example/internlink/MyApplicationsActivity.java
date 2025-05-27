package com.example.internlink;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MyApplicationsActivity extends AppCompatActivity {

    private static final String TAG = "MyApplicationsActivity";
    private RecyclerView applicationsRecycler;
    private TextView emptyView;
    private ApplicationsAdapter adapter;
    private List<ApplicationWithProject> applications = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_applications);

        applicationsRecycler = findViewById(R.id.applications_recycler);
        emptyView = findViewById(R.id.empty_view);

        applicationsRecycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ApplicationsAdapter(applications);
        applicationsRecycler.setAdapter(adapter);

        loadApplications();
    }

    private void loadApplications() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please login to view applications", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d(TAG, "Loading applications for user: " + user.getUid());

        // Alternative approach: Load all applications and filter locally
        DatabaseReference appsRef = FirebaseDatabase.getInstance().getReference("applications");

        appsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "Total applications in database: " + snapshot.getChildrenCount());
                applications.clear();

                List<Application> userApplications = new ArrayList<>();

                // Filter applications for current user
                for (DataSnapshot appSnapshot : snapshot.getChildren()) {
                    Application app = appSnapshot.getValue(Application.class);
                    if (app != null && user.getUid().equals(app.getUserId())) {
                        userApplications.add(app);
                        Log.d(TAG, "Found user application: " + app.getProjectId());
                    }
                }

                Log.d(TAG, "User applications found: " + userApplications.size());

                if (userApplications.isEmpty()) {
                    showEmptyView();
                    return;
                }

                // Counter to track when all project details are loaded
                final int totalApplications = userApplications.size();
                final int[] loadedCount = {0};

                for (Application app : userApplications) {
                    fetchProjectDetails(app, totalApplications, loadedCount);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load applications: " + error.getMessage());
                Toast.makeText(MyApplicationsActivity.this, "Failed to load applications", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchProjectDetails(Application application, int totalApplications, int[] loadedCount) {
        DatabaseReference projectRef = FirebaseDatabase.getInstance()
                .getReference("projects")
                .child(application.getProjectId());

        projectRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "Fetching project details for: " + application.getProjectId());
                Project project = snapshot.getValue(Project.class);
                if (project != null) {
                    applications.add(new ApplicationWithProject(application, project));
                    Log.d(TAG, "Added application with project. Total count: " + applications.size());
                } else {
                    Log.w(TAG, "Project not found for ID: " + application.getProjectId());
                }

                // Increment the loaded count
                loadedCount[0]++;
                Log.d(TAG, "Loaded count: " + loadedCount[0] + "/" + totalApplications);

                // Update UI only when all applications are loaded
                if (loadedCount[0] == totalApplications) {
                    Log.d(TAG, "All applications loaded. Final count: " + applications.size());
                    adapter.notifyDataSetChanged();

                    if (applications.isEmpty()) {
                        showEmptyView();
                    } else {
                        hideEmptyView();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MyApplicationsActivity.this, "Error loading project details", Toast.LENGTH_SHORT).show();

                // Still increment count even on error to prevent hanging
                loadedCount[0]++;
                if (loadedCount[0] == totalApplications) {
                    adapter.notifyDataSetChanged();

                    if (applications.isEmpty()) {
                        showEmptyView();
                    } else {
                        hideEmptyView();
                    }
                }
            }
        });
    }

    private void showEmptyView() {
        applicationsRecycler.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);
    }

    private void hideEmptyView() {
        applicationsRecycler.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
    }
}