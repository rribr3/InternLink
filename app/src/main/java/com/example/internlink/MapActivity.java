package com.example.internlink;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private static final String TAG = "MapActivity";
    private GoogleMap mMap;
    private DatabaseReference projectsRef;
    private DatabaseReference companiesRef;
    private ProgressBar loadingProgress;
    private Map<String, CompanyInfo> companiesMap = new HashMap<>();
    private Map<Marker, List<ProjectInfo>> markerProjectsMap = new HashMap<>();
    private com.example.internlink.LocationUtils LocationUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        Log.d(TAG, "MapActivity onCreate started");

        // Initialize views
        initializeViews();

        // Setup map
        setupMap();

        // Initialize Firebase references
        projectsRef = FirebaseDatabase.getInstance().getReference("projects");
        companiesRef = FirebaseDatabase.getInstance().getReference("users");

        Log.d(TAG, "Firebase references initialized");
    }

    private void initializeViews() {
        ImageButton backButton = findViewById(R.id.back_button);
        loadingProgress = findViewById(R.id.loading_progress);

        if (backButton != null) {
            backButton.setOnClickListener(v -> onBackPressed());
        } else {
            Log.w(TAG, "Back button not found in layout");
        }

        if (loadingProgress != null) {
            Log.d(TAG, "Loading progress bar found");
        } else {
            Log.w(TAG, "Loading progress bar not found in layout");
        }
    }

    private void setupMap() {
        Log.d(TAG, "Setting up map fragment");
        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.mapFragment);

        if (mapFragment != null) {
            Log.d(TAG, "Map fragment found, getting map async");
            mapFragment.getMapAsync(this);
        } else {
            Log.e(TAG, "Map fragment not found! Check your layout file.");
            Toast.makeText(this, "Map fragment not found", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        Log.d(TAG, "onMapReady called - Map is ready!");
        mMap = googleMap;

        // Configure map settings
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.setOnMarkerClickListener(this);

        // Center on Lebanon
        LatLng lebanon = new LatLng(33.8547, 35.8623);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lebanon, 8.0f));

        Log.d(TAG, "Map configured, camera moved to Lebanon");

        // Add a test marker first to verify map is working
        addTestMarker();

        // Load companies and projects
        loadCompaniesAndProjects();
    }

    private void addTestMarker() {
        // Add a test marker in Beirut to verify map is working
        LatLng beirut = new LatLng(33.8938, 35.5018);
        MarkerOptions testMarker = new MarkerOptions()
                .position(beirut)
                .title("Test Marker")
                .snippet("This is a test marker in Beirut")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));

        Marker marker = mMap.addMarker(testMarker);
        Log.d(TAG, "Test marker added: " + (marker != null ? "Success" : "Failed"));

        Toast.makeText(this, "Test marker added in Beirut", Toast.LENGTH_SHORT).show();
    }

    private void loadCompaniesAndProjects() {
        Log.d(TAG, "Starting to load companies and projects");

        if (loadingProgress != null) {
            loadingProgress.setVisibility(View.VISIBLE);
        }

        // First, load all companies
        companiesRef.orderByChild("role").equalTo("company")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Log.d(TAG, "Companies data received, count: " + snapshot.getChildrenCount());
                        companiesMap.clear();

                        for (DataSnapshot companySnapshot : snapshot.getChildren()) {
                            try {
                                String companyId = companySnapshot.getKey();
                                Log.d(TAG, "Processing company: " + companyId);

                                CompanyInfo company = extractCompanyInfo(companySnapshot);

                                if (company != null && company.hasValidLocation()) {
                                    companiesMap.put(companyId, company);
                                    Log.d(TAG, "Added company: " + company.name + " at " + company.address);
                                } else {
                                    Log.w(TAG, "Company " + companyId + " has no valid location");
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error loading company: " + e.getMessage());
                                e.printStackTrace();
                            }
                        }

                        Log.d(TAG, "Loaded " + companiesMap.size() + " companies with locations");

                        // Now load projects for these companies
                        if (!companiesMap.isEmpty()) {
                            loadProjectsForCompanies();
                        } else {
                            if (loadingProgress != null) {
                                loadingProgress.setVisibility(View.GONE);
                            }
                            Toast.makeText(MapActivity.this,
                                    "No companies with valid addresses found",
                                    Toast.LENGTH_LONG).show();
                            Log.w(TAG, "No companies with valid locations found");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (loadingProgress != null) {
                            loadingProgress.setVisibility(View.GONE);
                        }
                        Log.e(TAG, "Companies database error: " + error.getMessage());
                        Toast.makeText(MapActivity.this,
                                "Failed to load companies: " + error.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private CompanyInfo extractCompanyInfo(DataSnapshot companySnapshot) {
        try {
            CompanyInfo company = new CompanyInfo();
            company.companyId = companySnapshot.getKey();
            company.name = getStringValue(companySnapshot, "name");
            company.address = getStringValue(companySnapshot, "address");
            company.logoUrl = getStringValue(companySnapshot, "logoUrl");
            company.industry = getStringValue(companySnapshot, "industry");
            company.description = getStringValue(companySnapshot, "description");

            Log.d(TAG, "Company " + company.name + " has address: " + company.address);

            // Get coordinates from company address
            if (company.address != null && !company.address.trim().isEmpty()) {
                company.coordinates = LocationUtils.getCoordinatesFromString(company.address);

                // If no coordinates found, try with location field
                if (company.coordinates == null) {
                    String location = getStringValue(companySnapshot, "location");
                    if (location != null && !location.trim().isEmpty()) {
                        company.coordinates = LocationUtils.getCoordinatesFromString(location);
                        Log.d(TAG, "Trying location field: " + location);
                    }
                }

                if (company.coordinates != null) {
                    Log.d(TAG, "Found coordinates for " + company.name + ": " +
                            company.coordinates.latitude + ", " + company.coordinates.longitude);
                } else {
                    Log.w(TAG, "No coordinates found for company: " + company.name + " with address: " + company.address);
                }
            }

            return company;
        } catch (Exception e) {
            Log.e(TAG, "Error extracting company info: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private void loadProjectsForCompanies() {
        Log.d(TAG, "Loading projects for companies");

        projectsRef.orderByChild("status").equalTo("approved")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Log.d(TAG, "Projects data received, count: " + snapshot.getChildrenCount());

                        Map<String, List<ProjectInfo>> companyProjectsMap = new HashMap<>();
                        int totalProjects = 0;

                        for (DataSnapshot projectSnapshot : snapshot.getChildren()) {
                            try {
                                String projectId = projectSnapshot.getKey();
                                String companyId = getStringValue(projectSnapshot, "companyId");

                                Log.d(TAG, "Processing project: " + projectId + " for company: " + companyId);

                                // Check if we have location info for this company
                                if (companyId != null && companiesMap.containsKey(companyId)) {
                                    ProjectInfo project = extractProjectInfo(projectSnapshot, projectId);
                                    if (project != null) {
                                        companyProjectsMap.computeIfAbsent(companyId, k -> new ArrayList<>()).add(project);
                                        totalProjects++;
                                        Log.d(TAG, "Added project: " + project.title + " for company: " + companyId);
                                    }
                                } else {
                                    Log.w(TAG, "No company location found for project: " + projectId + ", companyId: " + companyId);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error processing project: " + e.getMessage());
                                e.printStackTrace();
                            }
                        }

                        Log.d(TAG, "Total projects found: " + totalProjects + " for " + companyProjectsMap.size() + " companies");

                        // Create markers for companies with projects
                        createCompanyMarkers(companyProjectsMap);

                        if (loadingProgress != null) {
                            loadingProgress.setVisibility(View.GONE);
                        }

                        if (totalProjects == 0) {
                            Toast.makeText(MapActivity.this,
                                    "No projects found for companies with addresses",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(MapActivity.this,
                                    totalProjects + " projects loaded from " + companyProjectsMap.size() + " companies",
                                    Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (loadingProgress != null) {
                            loadingProgress.setVisibility(View.GONE);
                        }
                        Log.e(TAG, "Projects database error: " + error.getMessage());
                        Toast.makeText(MapActivity.this,
                                "Failed to load projects: " + error.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private ProjectInfo extractProjectInfo(DataSnapshot projectSnapshot, String projectId) {
        try {
            ProjectInfo project = new ProjectInfo();
            project.projectId = projectId;
            project.title = getStringValue(projectSnapshot, "title");
            project.description = getStringValue(projectSnapshot, "description");
            project.category = getStringValue(projectSnapshot, "category");
            project.compensationType = getStringValue(projectSnapshot, "compensationType");
            project.companyId = getStringValue(projectSnapshot, "companyId");
            project.studentsRequired = getIntValue(projectSnapshot, "studentsRequired");
            project.applicants = getIntValue(projectSnapshot, "applicants");
            project.amount = getIntValue(projectSnapshot, "amount");
            project.deadline = getLongValue(projectSnapshot, "deadline");

            return project;
        } catch (Exception e) {
            Log.e(TAG, "Error extracting project info: " + e.getMessage());
            return null;
        }
    }

    private void createCompanyMarkers(Map<String, List<ProjectInfo>> companyProjectsMap) {
        Log.d(TAG, "Creating markers for " + companyProjectsMap.size() + " companies");
        markerProjectsMap.clear();

        for (Map.Entry<String, List<ProjectInfo>> entry : companyProjectsMap.entrySet()) {
            String companyId = entry.getKey();
            List<ProjectInfo> projects = entry.getValue();
            CompanyInfo company = companiesMap.get(companyId);

            if (company != null && company.coordinates != null) {
                Log.d(TAG, "Creating marker for company: " + company.name +
                        " at coordinates: " + company.coordinates.latitude + ", " + company.coordinates.longitude);

                // Create marker for company location
                MarkerOptions markerOptions = new MarkerOptions()
                        .position(company.coordinates)
                        .title(company.name)
                        .snippet(buildCompanySnippet(company, projects));

                // Set marker color based on company's projects
                float markerColor = getMarkerColorForCompany(projects);
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(markerColor));

                Marker marker = mMap.addMarker(markerOptions);
                if (marker != null) {
                    markerProjectsMap.put(marker, projects);
                    Log.d(TAG, "Marker created successfully for " + company.name);
                } else {
                    Log.e(TAG, "Failed to create marker for " + company.name);
                }
            } else {
                Log.w(TAG, "Cannot create marker for company " + companyId + " - missing company info or coordinates");
            }
        }

        Log.d(TAG, "Total markers created: " + markerProjectsMap.size());
    }

    private String buildCompanySnippet(CompanyInfo company, List<ProjectInfo> projects) {
        StringBuilder snippet = new StringBuilder();
        snippet.append("🏢 ").append(company.name);

        if (company.industry != null && !company.industry.isEmpty()) {
            snippet.append("\n🏭 ").append(company.industry);
        }

        snippet.append("\n📋 ").append(projects.size()).append(" Project(s)");

        // Count paid vs unpaid projects
        int paidCount = 0;
        int unpaidCount = 0;
        for (ProjectInfo project : projects) {
            if ("Paid".equalsIgnoreCase(project.compensationType)) {
                paidCount++;
            } else {
                unpaidCount++;
            }
        }

        if (paidCount > 0) {
            snippet.append("\n💰 ").append(paidCount).append(" Paid");
        }
        if (unpaidCount > 0) {
            snippet.append("\n🆓 ").append(unpaidCount).append(" Unpaid");
        }

        snippet.append("\n📍 ").append(company.address);
        snippet.append("\n\n👆 Tap for project details");

        return snippet.toString();
    }

    private float getMarkerColorForCompany(List<ProjectInfo> projects) {
        boolean hasPaid = false;
        boolean hasUnpaid = false;

        for (ProjectInfo project : projects) {
            if ("Paid".equalsIgnoreCase(project.compensationType)) {
                hasPaid = true;
            } else {
                hasUnpaid = true;
            }
        }

        // Green if has paid projects, Blue if only unpaid, Orange if mixed
        if (hasPaid && hasUnpaid) {
            return BitmapDescriptorFactory.HUE_ORANGE; // Mixed
        } else if (hasPaid) {
            return BitmapDescriptorFactory.HUE_GREEN; // Paid only
        } else {
            return BitmapDescriptorFactory.HUE_BLUE; // Unpaid only
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        Log.d(TAG, "Marker clicked: " + marker.getTitle());

        // Show info window
        marker.showInfoWindow();

        // Get projects for this company and show details
        List<ProjectInfo> projects = markerProjectsMap.get(marker);
        if (projects != null && !projects.isEmpty()) {
            showCompanyProjectsDialog(marker.getTitle(), projects);
        }

        return true;
    }

    private void showCompanyProjectsDialog(String companyName, List<ProjectInfo> projects) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle(companyName + " - Projects");

        // Create list of project titles
        String[] projectTitles = new String[projects.size()];
        for (int i = 0; i < projects.size(); i++) {
            ProjectInfo project = projects.get(i);
            String title = project.title;
            if (project.compensationType != null) {
                title += " (" + project.compensationType;
                if ("Paid".equalsIgnoreCase(project.compensationType) && project.amount > 0) {
                    title += " - $" + project.amount;
                }
                title += ")";
            }
            projectTitles[i] = title;
        }

        builder.setItems(projectTitles, (dialog, which) -> {
            ProjectInfo selectedProject = projects.get(which);
            navigateToProjectDetails(selectedProject.projectId);
        });

        builder.setNegativeButton("Close", null);
        builder.create().show();
    }

    private void navigateToProjectDetails(String projectId) {
        Intent intent = new Intent(this, ProjectDetailsActivity.class);
        intent.putExtra("projectId", projectId);
        startActivity(intent);
    }

    // Helper methods for safe value extraction
    private String getStringValue(DataSnapshot snapshot, String key) {
        if (snapshot.hasChild(key)) {
            Object value = snapshot.child(key).getValue();
            return value != null ? value.toString() : null;
        }
        return null;
    }

    private long getLongValue(DataSnapshot snapshot, String key) {
        if (snapshot.hasChild(key)) {
            Object value = snapshot.child(key).getValue();
            if (value instanceof Long) {
                return (Long) value;
            } else if (value instanceof Integer) {
                return ((Integer) value).longValue();
            } else if (value instanceof String) {
                try {
                    return Long.parseLong((String) value);
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Could not parse long value for " + key + ": " + value);
                }
            }
        }
        return 0L;
    }

    private int getIntValue(DataSnapshot snapshot, String key) {
        if (snapshot.hasChild(key)) {
            Object value = snapshot.child(key).getValue();
            if (value instanceof Integer) {
                return (Integer) value;
            } else if (value instanceof Long) {
                return ((Long) value).intValue();
            } else if (value instanceof String) {
                try {
                    return Integer.parseInt((String) value);
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Could not parse int value for " + key + ": " + value);
                }
            }
        }
        return 0;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (markerProjectsMap != null) {
            markerProjectsMap.clear();
        }
        if (companiesMap != null) {
            companiesMap.clear();
        }
        Log.d(TAG, "onDestroy called");
    }

    // Inner classes for data models
    private static class CompanyInfo {
        String companyId;
        String name;
        String address;
        String logoUrl;
        String industry;
        String description;
        LatLng coordinates;

        boolean hasValidLocation() {
            return coordinates != null && address != null && !address.trim().isEmpty();
        }
    }

    private static class ProjectInfo {
        String projectId;
        String title;
        String description;
        String category;
        String compensationType;
        String companyId;
        int studentsRequired;
        int applicants;
        int amount;
        long deadline;
    }
}