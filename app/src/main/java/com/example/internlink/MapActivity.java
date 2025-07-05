package com.example.internlink;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapActivity extends AppCompatActivity {

    private static final String TAG = "MapActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    // UI Components
    private WebView mapWebView;
    private ProgressBar loadingProgressBar;

    private FloatingActionButton currentLocationFab;
    private LinearLayout projectDetailsSheet;
    private TextView projectTitle, companyName, projectLocation, projectCategory;
    private Button viewDetailsButton, applyButton;

    // Location
    private FusedLocationProviderClient fusedLocationClient;
    private double currentLatitude = 33.8938; // Default to Beirut
    private double currentLongitude = 35.5018;

    // Data
    private List<ProjectLocation> projectLocations;
    private List<ProjectLocation> filteredProjects;
    private DatabaseReference databaseReference;
    private FirebaseAuth firebaseAuth;
    private String currentProjectId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        initializeViews();
        initializeLocation();
        initializeFirebase();
        setupWebView();
        requestLocationPermission();
    }

    private void initializeViews() {
        mapWebView = findViewById(R.id.mapWebView);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);
        currentLocationFab = findViewById(R.id.currentLocationFab);
        projectDetailsSheet = findViewById(R.id.projectDetailsSheet);
        projectTitle = findViewById(R.id.projectTitle);
        companyName = findViewById(R.id.companyName);
        projectLocation = findViewById(R.id.projectLocation);
        projectCategory = findViewById(R.id.projectCategory);
        viewDetailsButton = findViewById(R.id.viewDetailsButton);
        applyButton = findViewById(R.id.applyButton);

        projectLocations = new ArrayList<>();
        filteredProjects = new ArrayList<>();

        // ADD THESE CLICK LISTENERS:
        setupClickListeners();
    }
    private void setupClickListeners() {
        // Apply button click listener
        applyButton.setOnClickListener(v -> {
            if (currentProjectId != null) {
                applyToProject(currentProjectId);
            } else {
                Toast.makeText(this, "No project selected", Toast.LENGTH_SHORT).show();
            }
        });
        // View details button click listener
        viewDetailsButton.setOnClickListener(v -> {
            if (currentProjectId != null) {
                openProjectDetails(currentProjectId);
            } else {
                Toast.makeText(this, "No project selected", Toast.LENGTH_SHORT).show();
            }
        });

        // Current location FAB click listener
        currentLocationFab.setOnClickListener(v -> {
            getCurrentLocation();
        });

        // Hide project details when clicking outside (optional)
        projectDetailsSheet.setOnClickListener(v -> {
            // Do nothing - prevents click from propagating
        });
    }

    private void initializeLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    private void initializeFirebase() {
        databaseReference = FirebaseDatabase.getInstance().getReference();
        firebaseAuth = FirebaseAuth.getInstance();
        loadProjectsDataOptimized();
    }

    private void setupWebView() {
        WebSettings webSettings = mapWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);

        mapWebView.addJavascriptInterface(new MapJavaScriptInterface(), "Android");

        mapWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                loadingProgressBar.setVisibility(View.GONE);
                initializeMap();
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                Log.e(TAG, "WebView error: " + description);
                Toast.makeText(MapActivity.this, "Map loading error: " + description, Toast.LENGTH_SHORT).show();
            }
        });

        loadMapHTML();
    }


    private void loadProjectsDataOptimized() {
        // Use Firebase queries for better performance
        Query approvedProjectsQuery = databaseReference.child("projects")
                .orderByChild("status")
                .equalTo("approved");

        approvedProjectsQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot projectsSnapshot) {
                // Load companies data
                databaseReference.child("users")
                        .orderByChild("role")
                        .equalTo("company")
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot usersSnapshot) {
                                processProjectsDataOptimized(projectsSnapshot, usersSnapshot);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e(TAG, "Failed to load companies data", error.toException());
                                showError("Failed to load company data");
                            }
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load projects data", error.toException());
                showError("Failed to load projects data");
            }
        });
    }

    private void processProjectsDataOptimized(DataSnapshot projectsSnapshot, DataSnapshot usersSnapshot) {
        projectLocations.clear();

        for (DataSnapshot projectSnapshot : projectsSnapshot.getChildren()) {
            try {
                String projectId = projectSnapshot.getKey();
                String title = projectSnapshot.child("title").getValue(String.class);
                String companyId = projectSnapshot.child("companyId").getValue(String.class);
                String category = projectSnapshot.child("category").getValue(String.class);
                String description = projectSnapshot.child("description").getValue(String.class);
                String location = projectSnapshot.child("location").getValue(String.class);
                String status = projectSnapshot.child("status").getValue(String.class);
                Long applicants = projectSnapshot.child("applicants").getValue(Long.class);

                // Filter for approved projects with In Office location
                if (!"approved".equals(status) || !"In Office".equals(location)) {
                    continue;
                }

                // Get company details
                DataSnapshot companySnapshot = usersSnapshot.child(companyId);
                if (companySnapshot.exists()) {
                    String companyNameStr = companySnapshot.child("name").getValue(String.class);
                    String address = companySnapshot.child("address").getValue(String.class);
                    String industry = companySnapshot.child("industry").getValue(String.class);

                    if (address != null && !address.trim().isEmpty() &&
                            title != null && companyNameStr != null) {

                        ProjectLocation projectLocation = new ProjectLocation(
                                projectId, title, companyNameStr, address, category,
                                description, industry, 0.0, 0.0
                        );

                        if (applicants != null) {
                            projectLocation.setApplicantCount(applicants.intValue());
                        }

                        projectLocations.add(projectLocation);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error processing project data: " + e.getMessage(), e);
            }
        }

        filteredProjects.clear();
        filteredProjects.addAll(projectLocations);

        // Show results
        runOnUiThread(() -> {
            if (projectLocations.isEmpty()) {
                Toast.makeText(this, "No in-office projects found", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Found " + projectLocations.size() + " projects", Toast.LENGTH_SHORT).show();
            }
        });

        geocodeAddressesAndUpdateMap();
    }

    private void geocodeAddressesAndUpdateMap() {
        // Enhanced address mapping
        Map<String, double[]> addressCoordinates = new HashMap<>();

        // Lebanon locations
        addressCoordinates.put("beirut,lebanon", new double[]{33.8938, 35.5018});
        addressCoordinates.put("beirut", new double[]{33.8938, 35.5018});
        addressCoordinates.put("lebanon, beirut", new double[]{33.8938, 35.5018});
        addressCoordinates.put("Lebanon, beirut", new double[]{33.8938, 35.5018});
        addressCoordinates.put("tripoli, lebanon", new double[]{34.4332, 35.8497});
        addressCoordinates.put("sidon, lebanon", new double[]{33.5630, 35.3690});
        addressCoordinates.put("tyre, lebanon", new double[]{33.2704, 35.2038});

        // Other Middle East locations
        addressCoordinates.put("dubai", new double[]{25.2048, 55.2708});
        addressCoordinates.put("riyadh", new double[]{24.7136, 46.6753});
        addressCoordinates.put("amman", new double[]{31.9454, 35.9284});
        addressCoordinates.put("cairo", new double[]{30.0444, 31.2357});

        for (ProjectLocation project : filteredProjects) {
            String address = project.getAddress().toLowerCase().trim();
            double[] coords = null;

            // Try exact match first
            coords = addressCoordinates.get(address);

            // Try partial matches
            if (coords == null) {
                for (Map.Entry<String, double[]> entry : addressCoordinates.entrySet()) {
                    if (address.contains(entry.getKey()) || entry.getKey().contains(address)) {
                        coords = entry.getValue();
                        break;
                    }
                }
            }

            if (coords != null) {
                // Add small random offset to avoid overlapping markers
                project.setLatitude(coords[0] + (Math.random() - 0.5) * 0.01);
                project.setLongitude(coords[1] + (Math.random() - 0.5) * 0.01);
            } else {
                // Default to Beirut area with larger random offset for unknown addresses
                project.setLatitude(33.8938 + (Math.random() - 0.5) * 0.05);
                project.setLongitude(35.5018 + (Math.random() - 0.5) * 0.05);
                Log.w(TAG, "Unknown address: " + address + ", using default location");
            }
        }

        updateMapMarkers();
    }

    private void updateMapMarkers() {
        if (mapWebView != null) {
            try {
                JSONArray projectsArray = new JSONArray();
                for (ProjectLocation project : filteredProjects) {
                    JSONObject projectJson = new JSONObject();
                    projectJson.put("id", project.getProjectId());
                    projectJson.put("title", project.getTitle());
                    projectJson.put("company", project.getCompanyName());
                    projectJson.put("category", project.getCategory());
                    projectJson.put("address", project.getAddress());
                    projectJson.put("lat", project.getLatitude());
                    projectJson.put("lng", project.getLongitude());
                    projectJson.put("applicants", project.getApplicantCount());
                    projectJson.put("description", project.getDescription() != null ?
                            project.getDescription().substring(0, Math.min(100, project.getDescription().length())) + "..." :
                            "No description available");
                    projectsArray.put(projectJson);
                }

                String projectsJson = projectsArray.toString();
                mapWebView.post(() -> {
                    mapWebView.evaluateJavascript("updateMarkers(" + projectsJson + ");", null);
                });
            } catch (JSONException e) {
                Log.e(TAG, "Error creating projects JSON", e);
                showError("Error displaying projects on map");
            }
        }
    }

    private void filterProjects(String query) {
        filteredProjects.clear();

        if (query.trim().isEmpty()) {
            filteredProjects.addAll(projectLocations);
        } else {
            String lowerQuery = query.toLowerCase();
            for (ProjectLocation project : projectLocations) {
                if (project.getTitle().toLowerCase().contains(lowerQuery) ||
                        project.getCompanyName().toLowerCase().contains(lowerQuery) ||
                        project.getAddress().toLowerCase().contains(lowerQuery) ||
                        (project.getCategory() != null && project.getCategory().toLowerCase().contains(lowerQuery)) ||
                        (project.getIndustry() != null && project.getIndustry().toLowerCase().contains(lowerQuery))) {
                    filteredProjects.add(project);
                }
            }
        }

        updateMapMarkers();
    }

    private void showFilterDialog() {
        // TODO: Implement comprehensive filter dialog
        Toast.makeText(this, "Advanced filters coming soon!\nCurrently showing: " +
                filteredProjects.size() + " projects", Toast.LENGTH_SHORT).show();
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getCurrentLocation();
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                currentLatitude = location.getLatitude();
                                currentLongitude = location.getLongitude();
                                centerMapOnLocation(currentLatitude, currentLongitude);
                                Toast.makeText(MapActivity.this, "Centered on your location", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(MapActivity.this, "Unable to get current location", Toast.LENGTH_SHORT).show();
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to get location", e);
                        Toast.makeText(MapActivity.this, "Failed to get location", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void centerMapOnLocation(double lat, double lng) {
        mapWebView.evaluateJavascript("centerMap(" + lat + ", " + lng + ");", null);
    }

    private void initializeMap() {
        mapWebView.evaluateJavascript("initMap(" + currentLatitude + ", " + currentLongitude + ");", null);
    }

    private void loadMapHTML() {
        String htmlContent = generateEnhancedMapHTML();
        mapWebView.loadDataWithBaseURL("file:///android_asset/", htmlContent, "text/html", "UTF-8", null);
    }

    private String generateEnhancedMapHTML() {
        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <meta charset=\"utf-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>InternLink Project Map</title>\n" +
                "    <link rel=\"stylesheet\" href=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.css\" />\n" +
                "    <script src=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.js\"></script>\n" +
                "    <style>\n" +
                "        body { margin: 0; padding: 0; font-family: Arial, sans-serif; }\n" +
                "        #map { height: 100vh; width: 100%; }\n" +
                "        .custom-marker {\n" +
                "            background-color: #2196F3;\n" +
                "            border: 3px solid white;\n" +
                "            border-radius: 50%;\n" +
                "            width: 24px;\n" +
                "            height: 24px;\n" +
                "            box-shadow: 0 2px 4px rgba(0,0,0,0.3);\n" +
                "        }\n" +
                "        .marker-web { background-color: #4CAF50; }\n" +
                "        .marker-mobile { background-color: #FF9800; }\n" +
                "        .marker-cybersecurity { background-color: #F44336; }\n" +
                "        .popup-content {\n" +
                "            min-width: 250px;\n" +
                "            font-family: Arial, sans-serif;\n" +
                "        }\n" +
                "        .popup-title {\n" +
                "            font-size: 16px;\n" +
                "            font-weight: bold;\n" +
                "            color: #2196F3;\n" +
                "            margin: 0 0 8px 0;\n" +
                "        }\n" +
                "        .popup-company {\n" +
                "            font-size: 14px;\n" +
                "            font-weight: bold;\n" +
                "            color: #333;\n" +
                "            margin: 4px 0;\n" +
                "        }\n" +
                "        .popup-address {\n" +
                "            font-size: 12px;\n" +
                "            color: #666;\n" +
                "            margin: 4px 0;\n" +
                "        }\n" +
                "        .popup-category {\n" +
                "            background: #e3f2fd;\n" +
                "            padding: 4px 8px;\n" +
                "            border-radius: 12px;\n" +
                "            font-size: 11px;\n" +
                "            color: #1976d2;\n" +
                "            display: inline-block;\n" +
                "            margin: 4px 0;\n" +
                "        }\n" +
                "        .popup-applicants {\n" +
                "            font-size: 12px;\n" +
                "            color: #FF5722;\n" +
                "            margin: 4px 0;\n" +
                "        }\n" +
                "        .popup-description {\n" +
                "            font-size: 12px;\n" +
                "            color: #666;\n" +
                "            margin: 8px 0 4px 0;\n" +
                "            line-height: 1.4;\n" +
                "        }\n" +
                "        .current-location-marker {\n" +
                "            background-color: #FF4444;\n" +
                "            border: 3px solid white;\n" +
                "            border-radius: 50%;\n" +
                "            width: 20px;\n" +
                "            height: 20px;\n" +
                "            box-shadow: 0 0 10px rgba(255, 68, 68, 0.5);\n" +
                "        }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div id=\"map\"></div>\n" +
                "\n" +
                "    <script>\n" +
                "        let map;\n" +
                "        let markers = [];\n" +
                "        let currentLocationMarker;\n" +
                "\n" +
                "        function initMap(lat, lng) {\n" +
                "            try {\n" +
                "                map = L.map('map').setView([lat, lng], 12);\n" +
                "                \n" +
                "                L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {\n" +
                "                    attribution: '© OpenStreetMap contributors',\n" +
                "                    maxZoom: 19\n" +
                "                }).addTo(map);\n" +
                "                \n" +
                "                // Add current location marker\n" +
                "                let currentLocationIcon = L.divIcon({\n" +
                "                    className: 'current-location-marker',\n" +
                "                    iconSize: [20, 20],\n" +
                "                    iconAnchor: [10, 10]\n" +
                "                });\n" +
                "                \n" +
                "                currentLocationMarker = L.marker([lat, lng], {icon: currentLocationIcon})\n" +
                "                    .addTo(map)\n" +
                "                    .bindPopup('<b>Your Current Location</b>');\n" +
                "                    \n" +
                "                console.log('Map initialized successfully');\n" +
                "            } catch (error) {\n" +
                "                console.error('Error initializing map:', error);\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        function updateMarkers(projects) {\n" +
                "            try {\n" +
                "                // Clear existing project markers\n" +
                "                markers.forEach(marker => {\n" +
                "                    if (marker !== currentLocationMarker) {\n" +
                "                        map.removeLayer(marker);\n" +
                "                    }\n" +
                "                });\n" +
                "                markers = [];\n" +
                "\n" +
                "                // Add current location marker back to array\n" +
                "                if (currentLocationMarker) {\n" +
                "                    markers.push(currentLocationMarker);\n" +
                "                }\n" +
                "\n" +
                "                // Add new project markers\n" +
                "                projects.forEach(project => {\n" +
                "                    let markerClass = 'custom-marker';\n" +
                "                    if (project.category === 'Web Development') markerClass += ' marker-web';\n" +
                "                    else if (project.category === 'Mobile Development') markerClass += ' marker-mobile';\n" +
                "                    else if (project.category === 'Cybersecurity') markerClass += ' marker-cybersecurity';\n" +
                "\n" +
                "                    let customIcon = L.divIcon({\n" +
                "                        className: markerClass,\n" +
                "                        iconSize: [24, 24],\n" +
                "                        iconAnchor: [12, 12]\n" +
                "                    });\n" +
                "\n" +
                "                    let popupContent = `\n" +
                "                        <div class=\"popup-content\">\n" +
                "                            <div class=\"popup-title\">${project.title}</div>\n" +
                "                            <div class=\"popup-company\">${project.company}</div>\n" +
                "                            <div class=\"popup-address\">📍 ${project.address}</div>\n" +
                "                            <div class=\"popup-category\">${project.category}</div>\n" +
                "                            <div class=\"popup-applicants\">👥 ${project.applicants || 0} applicant(s)</div>\n" +
                "                            <div class=\"popup-description\">${project.description || 'No description available'}</div>\n" +
                "                        </div>\n" +
                "                    `;\n" +
                "\n" +
                "                    let marker = L.marker([project.lat, project.lng], {icon: customIcon})\n" +
                "                        .addTo(map)\n" +
                "                        .bindPopup(popupContent)\n" +
                "                        .on('click', function() {\n" +
                "                            Android.onMarkerClick(project.id, project.title, project.company, project.address, project.category);\n" +
                "                        });\n" +
                "\n" +
                "                    markers.push(marker);\n" +
                "                });\n" +
                "                \n" +
                "                console.log('Updated map with', projects.length, 'projects');\n" +
                "            } catch (error) {\n" +
                "                console.error('Error updating markers:', error);\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        function centerMap(lat, lng) {\n" +
                "            try {\n" +
                "                if (map) {\n" +
                "                    map.setView([lat, lng], 15);\n" +
                "                    \n" +
                "                    // Update current location marker\n" +
                "                    if (currentLocationMarker) {\n" +
                "                        currentLocationMarker.setLatLng([lat, lng]);\n" +
                "                    }\n" +
                "                }\n" +
                "            } catch (error) {\n" +
                "                console.error('Error centering map:', error);\n" +
                "            }\n" +
                "        }\n" +
                "    </script>\n" +
                "</body>\n" +
                "</html>";
    }

    // Also, make sure your showProjectDetails method properly sets the currentProjectId:
    private void showProjectDetails(String projectId, String title, String company, String address, String category) {
        currentProjectId = projectId; // This is already correct in your code
        projectTitle.setText(title);
        companyName.setText(company);
        projectLocation.setText(address);
        projectCategory.setText(category);

        // Make sure the buttons are enabled and visible
        applyButton.setEnabled(true);
        applyButton.setVisibility(View.VISIBLE);
        viewDetailsButton.setEnabled(true);
        viewDetailsButton.setVisibility(View.VISIBLE);

        projectDetailsSheet.setVisibility(View.VISIBLE);
    }

    private void hideProjectDetails() {
        projectDetailsSheet.setVisibility(View.GONE);
        currentProjectId = null;
    }

    private void openProjectDetails(String projectId) {
        try {
            // Navigate to project details activity
            Intent intent = new Intent(this, ApplyNowActivity.class);
            intent.putExtra("PROJECT_ID", projectId);
            startActivity(intent);
            hideProjectDetails();
        } catch (Exception e) {
            Toast.makeText(this, "Project details: " + projectId, Toast.LENGTH_SHORT).show();
            Log.w(TAG, "ApplyNow not found, add it to navigate to project details");
        }
    }
    private void applyToProject(String projectId) {
        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login to apply for projects", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if user is a student
        String currentUserId = firebaseAuth.getCurrentUser().getUid();
        databaseReference.child("users").child(currentUserId).child("role")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String role = snapshot.getValue(String.class);
                        if ("student".equals(role)) {
                            try {
                                // Navigate to application activity
                                Intent intent = new Intent(MapActivity.this, ApplyNowActivity.class);
                                intent.putExtra("PROJECT_ID", projectId);
                                startActivity(intent);
                                hideProjectDetails();
                            } catch (Exception e) {
                                // If ApplyProjectActivity doesn't exist, show application dialog
                                showQuickApplicationDialog(projectId);
                                Log.w(TAG, "ApplyProjectActivity not found, showing quick apply dialog");
                            }
                        } else {
                            Toast.makeText(MapActivity.this, "Only students can apply for projects", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error checking user role", error.toException());
                        Toast.makeText(MapActivity.this, "Error checking user permissions", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showQuickApplicationDialog(String projectId) {
        // Create a simple application dialog
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Apply to Project");
        builder.setMessage("Would you like to apply to this project? Your profile information will be submitted.");

        builder.setPositiveButton("Apply", (dialog, which) -> {
            submitQuickApplication(projectId);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.dismiss();
        });

        builder.show();
    }

    private void submitQuickApplication(String projectId) {
        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login to apply", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = firebaseAuth.getCurrentUser().getUid();
        String applicationId = databaseReference.child("applications").push().getKey();

        if (applicationId == null) {
            Toast.makeText(this, "Error creating application", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get project company ID first
        databaseReference.child("projects").child(projectId).child("companyId")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String companyId = snapshot.getValue(String.class);
                        if (companyId != null) {
                            // Create application object
                            Map<String, Object> application = new HashMap<>();
                            application.put("projectId", projectId);
                            application.put("userId", userId);
                            application.put("companyId", companyId);
                            application.put("status", "Pending");
                            application.put("appliedDate", System.currentTimeMillis());
                            application.put("reapplication", false);

                            // Submit application
                            databaseReference.child("applications").child(applicationId)
                                    .setValue(application)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(MapActivity.this, "Application submitted successfully!", Toast.LENGTH_SHORT).show();
                                        hideProjectDetails();

                                        // Update project applicants count
                                        updateProjectApplicantCount(projectId);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error submitting application", e);
                                        Toast.makeText(MapActivity.this, "Failed to submit application", Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            Toast.makeText(MapActivity.this, "Error: Project company not found", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error getting company ID", error.toException());
                        Toast.makeText(MapActivity.this, "Error submitting application", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateProjectApplicantCount(String projectId) {
        databaseReference.child("projects").child(projectId).child("applicants")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Long currentCount = snapshot.getValue(Long.class);
                        long newCount = (currentCount != null ? currentCount : 0) + 1;

                        databaseReference.child("projects").child(projectId).child("applicants")
                                .setValue(newCount);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.w(TAG, "Failed to update applicant count", error.toException());
                    }
                });
    }

    private void showError(String message) {
        runOnUiThread(() -> {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            loadingProgressBar.setVisibility(View.GONE);
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Location permission denied. Using default location.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapWebView != null) {
            mapWebView.removeAllViews();
            mapWebView.destroy();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapWebView != null) {
            mapWebView.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapWebView != null) {
            mapWebView.onResume();
        }
    }

    @Override
    public void onBackPressed() {
        if (projectDetailsSheet.getVisibility() == View.VISIBLE) {
            hideProjectDetails();
        } else {
            super.onBackPressed();
        }
    }

    // JavaScript Interface for WebView communication
    public class MapJavaScriptInterface {
        @JavascriptInterface
        public void onMarkerClick(String projectId, String title, String company, String address, String category) {
            runOnUiThread(() -> showProjectDetails(projectId, title, company, address, category));
        }

        @JavascriptInterface
        public void onMapReady() {
            runOnUiThread(() -> {
                Log.d(TAG, "Map is ready");
                loadingProgressBar.setVisibility(View.GONE);
            });
        }

        @JavascriptInterface
        public void onMapError(String error) {
            runOnUiThread(() -> {
                Log.e(TAG, "Map error: " + error);
                Toast.makeText(MapActivity.this, "Map error: " + error, Toast.LENGTH_SHORT).show();
            });
        }
    }

    // Enhanced ProjectLocation data class
    public static class ProjectLocation {
        private String projectId;
        private String title;
        private String companyName;
        private String address;
        private String category;
        private String description;
        private String industry;
        private double latitude;
        private double longitude;
        private int applicantCount;

        public ProjectLocation(String projectId, String title, String companyName, String address,
                               String category, String description, String industry, double latitude, double longitude) {
            this.projectId = projectId;
            this.title = title;
            this.companyName = companyName;
            this.address = address;
            this.category = category;
            this.description = description;
            this.industry = industry;
            this.latitude = latitude;
            this.longitude = longitude;
            this.applicantCount = 0;
        }

        // Getters and setters
        public String getProjectId() { return projectId; }
        public String getTitle() { return title; }
        public String getCompanyName() { return companyName; }
        public String getAddress() { return address; }
        public String getCategory() { return category; }
        public String getDescription() { return description; }
        public String getIndustry() { return industry; }
        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }
        public int getApplicantCount() { return applicantCount; }

        public void setLatitude(double latitude) { this.latitude = latitude; }
        public void setLongitude(double longitude) { this.longitude = longitude; }
        public void setApplicantCount(int applicantCount) { this.applicantCount = applicantCount; }

        public void setProjectId(String projectId) { this.projectId = projectId; }
        public void setTitle(String title) { this.title = title; }
        public void setCompanyName(String companyName) { this.companyName = companyName; }
        public void setAddress(String address) { this.address = address; }
        public void setCategory(String category) { this.category = category; }
        public void setDescription(String description) { this.description = description; }
        public void setIndustry(String industry) { this.industry = industry; }

        @Override
        public String toString() {
            return "ProjectLocation{" +
                    "projectId='" + projectId + '\'' +
                    ", title='" + title + '\'' +
                    ", companyName='" + companyName + '\'' +
                    ", address='" + address + '\'' +
                    ", category='" + category + '\'' +
                    ", latitude=" + latitude +
                    ", longitude=" + longitude +
                    ", applicantCount=" + applicantCount +
                    '}';
        }
    }
}