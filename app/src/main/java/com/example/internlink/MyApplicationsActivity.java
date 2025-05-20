package com.example.internlink;

import android.os.Bundle;
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
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MyApplicationsActivity extends AppCompatActivity {

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

        DatabaseReference appsRef = (DatabaseReference) FirebaseDatabase.getInstance()
                .getReference("applications")
                .orderByChild("userId")
                .equalTo(user.getUid());

        appsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                applications.clear();
                for (DataSnapshot appSnapshot : snapshot.getChildren()) {
                    Application app = appSnapshot.getValue(Application.class);
                    if (app != null) {
                        // Fetch project details for each application
                        fetchProjectDetails(app);
                    }
                }

                if (applications.isEmpty()) {
                    showEmptyView();
                } else {
                    hideEmptyView();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MyApplicationsActivity.this, "Failed to load applications", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchProjectDetails(Application application) {
        DatabaseReference projectRef = FirebaseDatabase.getInstance()
                .getReference("projects")
                .child(application.getProjectId());

        projectRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Project project = snapshot.getValue(Project.class);
                if (project != null) {
                    applications.add(new ApplicationWithProject(application, project));
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