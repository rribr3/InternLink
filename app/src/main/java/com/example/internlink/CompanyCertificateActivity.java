package com.example.internlink;

import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CompanyCertificateActivity extends AppCompatActivity {

    private AutoCompleteTextView projectSpinner;
    private RecyclerView applicantsRecyclerView;
    private LinearLayout emptyStateLayout;
    private MaterialToolbar topAppBar;

    private String companyId;
    private DatabaseReference projectsRef;
    private DatabaseReference applicationsRef;
    private List<Project> completedProjects = new ArrayList<>();
    private List<CompletedApplicant> completedApplicants = new ArrayList<>();
    private CertificateApplicantsAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;

    private static final String DROPBOX_ACCESS_TOKEN = "sl.u.AF234H4ssati8aUFvG1Akpf8HwodqtWNC8s36lt7HwI2mxQQDgepFaVM5cEN8NitiYhKk1jngniBGxw_XU7hL78byTolwW8EoJCZElNwr6NM6aghEKN1sLk_kWHOfV0vqO7oYl3ylCSn-eYpKmLQFaP06j3Pu05g-n_nyVZjky0d9lwFJ73fORWnhRuWQ3INhyu9U22wDjhcaOhxTROaI9zzqizoctOKr8X8WpxNm8B8PRF3pIIhPsi-QlbAe4WbiU1VhyAa4AywznzL7LI9vMQRCePrB2eCStFdLiZHbeRzlxl-eS-sLlXRs2N2phHQIluL4m_p9dpoY4uYJFspoekFxmuzpEktTaws4vu799c2yLYx8tfe9AlgNwuDoDsIK22hjXZBEEYyYhU8xTS8CUCTMZe-EyPksOrD4-Qkt0PrWcTRO9PPMFAJ5N_ZeutcK7f6UbXTjor3JuADOTqtcHmVmjVchTXoNhzKDSjF9nk9iXgerLiwlOYgqJDyi8m-STL0NpUwVZeBizhUb2Ay3HZGeBEdFL1HBIL2SWEcmWkJf5aVtDs18k27sLMDZ4ODPYGfsA4XdJu7sbtRQnIgeIW0gbENxZy3BU2IWf-YD_CkO1IeppCGYQoX1NoQJpafYP-5QtHXLSCAmZL3sU9oipZfvQMb6mFKH8PcNV8B50HjMmsgy4vNiz_UWtPbiGFgPsbnHYOicLOQkDeodjDt2uSN-yT4C05j8Qj9Gdg8kQalbGmtTUzT7rFz3_Wq1raHoTv_perfKihsWbpmEzo2A8w-HVoT9RyC6XGCfeXI4dWglb6nvIDD-dDKZQgmUFGJR0vqxzJn_5Kn2gddJHJAN2Z97XFz4YYqhaRbW22mCv7M3HN4eIH7MSceGOBkDLDB9aM4gcNu7xioi06mWdDoNu0Wy4unvx85fd9SXjYXwN-upES152rLSk0bOqBJ_1ZiXU8k-Jxfp-DmmX0MpBxrKZ6UsdlPV0wycB1vwE4ov2Q1OMEAMyP0dtcaX_MRfMBRbcJ-0p3Qaf9Wu0gG5b4YX7n92kF7cVeanORIYhk7f6Oca9WwXVIPdYkL8iN74cSFvKMOgiIVgTY8I8fdNORA8p-AF_CFZIF15jA7eYLl3XIz6SnZ_jKni97FG-BsUNAXqzTwX2t17o9WE_WLqeHmUCimTrIhDwpoOS9mcR3zJtxCzNbUuunH3D4zaxWyY_k62WGDBX4Je0Hl6DQg-YkfhtY3s1sjZcLXoS91aI2s5D5H-IaHTQe1eMtQaQWI34ZkERxcLuB7OIm6BeIIdJhm2oN7XUhVUcfyH_J4NO6iYRAcNPNVuRiC5cNIfAY1gSCBzXsMK6Q2pkc3Xw6jRf0tOkBk8S7yRuEv25FEAnq4SPyvntGZr6n3TQKRL7ZcxBESqKoOKq7kpO7LpMzMRXYFHEHSyo9FfovuVLgq2bS4dP2A_Q";
    private final ActivityResultLauncher<String> pdfPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    // Get selected applicant from adapter
                    CompletedApplicant selectedApplicant = adapter.getSelectedApplicant();
                    if (selectedApplicant != null) {
                        uploadCertificateToDropbox(uri, selectedApplicant);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_company_certificate);

        initViews();
        setupFirebase();
        setupRefreshLayout();
        loadCompletedProjects();

        topAppBar.setNavigationOnClickListener(v -> finish());
    }

    private void initViews() {
        projectSpinner = findViewById(R.id.projectSpinner);
        applicantsRecyclerView = findViewById(R.id.applicantsRecyclerView);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        topAppBar = findViewById(R.id.topAppBar);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);

        applicantsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CertificateApplicantsAdapter(completedApplicants, this::handleCertificateAction);
        applicantsRecyclerView.setAdapter(adapter);

        projectSpinner.setOnItemClickListener((parent, view, position, id) -> {
            Project selectedProject = completedProjects.get(position);
            loadCompletedApplicants(selectedProject.getProjectId());
        });
    }
    private void setupRefreshLayout() {
        swipeRefreshLayout.setColorSchemeResources(
                R.color.blue_500,
                R.color.green,
                R.color.red,
                R.color.yellow
        );

        swipeRefreshLayout.setOnRefreshListener(this::refreshData);
    }

    private void refreshData() {
        // Clear existing data
        completedProjects.clear();
        completedApplicants.clear();
        adapter.notifyDataSetChanged();

        // Reload data
        loadCompletedProjects();
    }
    private void sendCertificateAnnouncement(CompletedApplicant applicant, String certificateUrl) {
        DatabaseReference projectRef = FirebaseDatabase.getInstance()
                .getReference("projects")
                .child(applicant.getProjectId());

        projectRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String projectTitle = snapshot.child("title").getValue(String.class);
                String companyName = snapshot.child("companyName").getValue(String.class);
                if (companyName == null) {
                    companyName = "the company"; // Fallback name
                }

                // Create announcement data
                Map<String, Object> announcement = new HashMap<>();
                announcement.put("title", "Certificate Received");
                announcement.put("message", String.format(
                        "🎓 Congratulations! You have received your completion certificate for \"%s\" from %s.\n\n" +
                                "Check your email address.",
                        projectTitle,
                        companyName
                ));
                announcement.put("timestamp", System.currentTimeMillis());
                announcement.put("type", "certificate");
                announcement.put("recipientId", applicant.getUserId());
                announcement.put("projectId", applicant.getProjectId());
                announcement.put("certificateUrl", certificateUrl);

                // Add announcement to student announcements
                DatabaseReference announcementRef = FirebaseDatabase.getInstance()
                        .getReference("announcements_by_role")
                        .child("student")
                        .push();

                announcementRef.setValue(announcement);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error silently - the main certificate functionality still worked
            }
        });
    }

    private void setupFirebase() {
        companyId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        projectsRef = FirebaseDatabase.getInstance().getReference("projects");
        applicationsRef = FirebaseDatabase.getInstance().getReference("applications");
    }

    private void loadCompletedProjects() {
        projectsRef.orderByChild("companyId").equalTo(companyId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        completedProjects.clear();
                        for (DataSnapshot projectSnap : snapshot.getChildren()) {
                            String status = projectSnap.child("status").getValue(String.class);
                            if ("completed".equals(status)) {
                                Project project = new Project(
                                        projectSnap.getKey(),
                                        projectSnap.child("title").getValue(String.class)
                                );
                                completedProjects.add(project);
                            }
                        }

                        updateProjectSpinner();
                        swipeRefreshLayout.setRefreshing(false);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(CompanyCertificateActivity.this,
                                "Failed to load projects", Toast.LENGTH_SHORT).show();
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
    }

    private void updateProjectSpinner() {
        List<String> projectTitles = new ArrayList<>();
        for (Project project : completedProjects) {
            projectTitles.add(project.getTitle());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                projectTitles
        );
        projectSpinner.setAdapter(adapter);
    }

    private void loadCompletedApplicants(String projectId) {
        // Show loading state or progress indicator if needed
        completedApplicants.clear();
        adapter.notifyDataSetChanged();

        applicationsRef.orderByChild("projectId").equalTo(projectId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int expectedApplicants = 0;
                        for (DataSnapshot appSnap : snapshot.getChildren()) {
                            String status = appSnap.child("status").getValue(String.class);
                            if ("Accepted".equals(status)) {
                                expectedApplicants++;
                                String userId = appSnap.child("userId").getValue(String.class);
                                loadApplicantDetails(userId, projectId, expectedApplicants);
                            }
                        }

                        // If there are no accepted applicants at all
                        if (expectedApplicants == 0) {
                            updateUI();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(CompanyCertificateActivity.this,
                                "Failed to load applicants", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadApplicantDetails(String userId, String projectId, int expectedTotal) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
        DatabaseReference projectRef = FirebaseDatabase.getInstance().getReference("projects").child(projectId);
        DatabaseReference certificateRef = FirebaseDatabase.getInstance()
                .getReference("certificates")
                .child(projectId)
                .child(userId);

        projectRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot projectSnapshot) {
                String projectTitle = projectSnapshot.child("title").getValue(String.class);

                userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String name = snapshot.child("name").getValue(String.class);
                        String email = snapshot.child("email").getValue(String.class);
                        String profileUrl = snapshot.child("logoUrl").getValue(String.class);

                        CompletedApplicant applicant = new CompletedApplicant(
                                userId,
                                name,
                                email,
                                profileUrl,
                                projectId,
                                projectTitle  // Add project title here
                        );

                        certificateRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot certificateSnapshot) {
                                if (certificateSnapshot.exists()) {
                                    String certificateUrl = certificateSnapshot.child("certificateUrl").getValue(String.class);
                                    applicant.setCertificateUrl(certificateUrl);
                                }

                                completedApplicants.add(applicant);

                                // Only update UI when all applicants are loaded
                                if (completedApplicants.size() == expectedTotal) {
                                    adapter.notifyDataSetChanged();
                                    updateUI();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Toast.makeText(CompanyCertificateActivity.this,
                                        "Failed to load certificate details", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(CompanyCertificateActivity.this,
                                "Failed to load applicant details", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CompanyCertificateActivity.this,
                        "Failed to load project details", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI() {
        if (completedApplicants.isEmpty()) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            applicantsRecyclerView.setVisibility(View.GONE);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            applicantsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void handleCertificateAction(CompletedApplicant applicant, String action) {
        switch (action) {
            case "send":
                pdfPickerLauncher.launch("application/pdf");
                adapter.setSelectedApplicant(applicant);
                break;
            case "view":
                viewCertificate(applicant);
                break;
            case "delete":
                deleteCertificate(applicant);
                break;
        }
    }

    private void uploadCertificateToDropbox(Uri pdfUri, CompletedApplicant applicant) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Uploading certificate...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        try {
            InputStream inputStream = getContentResolver().openInputStream(pdfUri);
            byte[] pdfData = new byte[inputStream.available()];
            inputStream.read(pdfData);
            inputStream.close();

            String fileName = String.format("/certificates/%s_%s.pdf", applicant.getUserId(), applicant.getProjectId());

            JSONObject dropboxArg = new JSONObject();
            dropboxArg.put("path", fileName);
            dropboxArg.put("mode", "overwrite");
            dropboxArg.put("autorename", false);
            dropboxArg.put("mute", false);

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .build();

            RequestBody uploadBody = RequestBody.create(MediaType.parse("application/octet-stream"), pdfData);

            Request uploadRequest = new Request.Builder()
                    .url("https://content.dropboxapi.com/2/files/upload")
                    .addHeader("Authorization", "Bearer " + DROPBOX_ACCESS_TOKEN)
                    .addHeader("Dropbox-API-Arg", dropboxArg.toString())
                    .addHeader("Content-Type", "application/octet-stream")
                    .post(uploadBody)
                    .build();

            client.newCall(uploadRequest).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(CompanyCertificateActivity.this,
                                "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(CompanyCertificateActivity.this,
                                    "Upload failed", Toast.LENGTH_SHORT).show();
                        });
                        return;
                    }

                    // Create sharing link
                    JSONObject shareBody = new JSONObject();
                    try {
                        shareBody.put("path", fileName);

                        Request shareRequest = new Request.Builder()
                                .url("https://api.dropboxapi.com/2/sharing/create_shared_link")
                                .addHeader("Authorization", "Bearer " + DROPBOX_ACCESS_TOKEN)
                                .addHeader("Content-Type", "application/json")
                                .post(RequestBody.create(MediaType.parse("application/json"), shareBody.toString()))
                                .build();

                        client.newCall(shareRequest).enqueue(new Callback() {
                            @Override
                            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                                runOnUiThread(() -> {
                                    progressDialog.dismiss();
                                    Toast.makeText(CompanyCertificateActivity.this,
                                            "Failed to create share link", Toast.LENGTH_SHORT).show();
                                });
                            }

                            @Override
                            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                                if (!response.isSuccessful() && response.code() != 409) {
                                    runOnUiThread(() -> {
                                        progressDialog.dismiss();
                                        Toast.makeText(CompanyCertificateActivity.this,
                                                "Failed to create share link", Toast.LENGTH_SHORT).show();
                                    });
                                    return;
                                }

                                try {
                                    String shareLink;
                                    if (response.code() == 409) {
                                        // Link already exists, get existing link
                                        shareLink = "https://www.dropbox.com/home" + fileName;
                                    } else {
                                        JSONObject json = new JSONObject(response.body().string());
                                        shareLink = json.getString("url").replace("?dl=0", "?dl=1");
                                    }

                                    // Get project details before saving certificate
                                    DatabaseReference projectRef = FirebaseDatabase.getInstance()
                                            .getReference("projects")
                                            .child(applicant.getProjectId());

                                    projectRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            String projectTitle = snapshot.child("title").getValue(String.class);
                                            String companyName = snapshot.child("companyName").getValue(String.class);

                                            if (companyName == null) {
                                                companyName = "tech"; // Default fallback name
                                            }

                                            // Save to Firebase
                                            Map<String, Object> certificateData = new HashMap<>();
                                            certificateData.put("certificateUrl", shareLink);
                                            certificateData.put("certificatePath", fileName);
                                            certificateData.put("timestamp", System.currentTimeMillis());

                                            DatabaseReference certificateRef = FirebaseDatabase.getInstance()
                                                    .getReference("certificates")
                                                    .child(applicant.getProjectId())
                                                    .child(applicant.getUserId());

                                            // Use final variables for the inner class
                                            final String finalProjectTitle = projectTitle;
                                            final String finalCompanyName = companyName;

                                            certificateRef.setValue(certificateData)
                                                    .addOnSuccessListener(aVoid -> {
                                                        applicant.setCertificateUrl(shareLink);
                                                        sendCertificateAnnouncement(applicant, shareLink);
                                                        sendCertificateEmail(
                                                                applicant.getEmail(),
                                                                shareLink,
                                                                finalProjectTitle,
                                                                finalCompanyName
                                                        );
                                                        runOnUiThread(() -> {
                                                            progressDialog.dismiss();
                                                            Toast.makeText(CompanyCertificateActivity.this,
                                                                    "Certificate sent successfully", Toast.LENGTH_SHORT).show();
                                                            adapter.notifyDataSetChanged();
                                                        });
                                                    })
                                                    .addOnFailureListener(e -> runOnUiThread(() -> {
                                                        progressDialog.dismiss();
                                                        Toast.makeText(CompanyCertificateActivity.this,
                                                                "Failed to save certificate data", Toast.LENGTH_SHORT).show();
                                                    }));
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                            runOnUiThread(() -> {
                                                progressDialog.dismiss();
                                                Toast.makeText(CompanyCertificateActivity.this,
                                                        "Failed to load project details", Toast.LENGTH_SHORT).show();
                                            });
                                        }
                                    });

                                } catch (Exception e) {
                                    runOnUiThread(() -> {
                                        progressDialog.dismiss();
                                        Toast.makeText(CompanyCertificateActivity.this,
                                                "Error processing response", Toast.LENGTH_SHORT).show();
                                    });
                                }
                            }
                        });

                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(CompanyCertificateActivity.this,
                                    "Error creating share request", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            });

        } catch (Exception e) {
            progressDialog.dismiss();
            Toast.makeText(this, "Error reading PDF", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendCertificateEmail(String email, String certificateUrl, String projectTitle, String companyName) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            String currentDateTime = dateFormat.format(new Date());

            String subject = "InternLink - Project Completion Certificate: " + projectTitle;

            // Create the email body with the friendly name for the certificate
            String body = String.format(
                    "Dear Student,\n\n" +
                            "Congratulations on completing the project \"%s\"!\n\n" +
                            "Your certificate: %s (%s)\n\n" +
                            "Certificate issued on: %s\n\n" +
                            "Best regards,\n" +
                            "%s",
                    projectTitle,
                    String.format("%s's Certificate.pdf", projectTitle), // Friendly name
                    certificateUrl, // URL in parentheses
                    currentDateTime,
                    companyName);

            // Create Gmail URI
            Uri gmailUri = Uri.parse("mailto:" + email +
                    "?subject=" + Uri.encode(subject) +
                    "&body=" + Uri.encode(body));

            Intent emailIntent = new Intent(Intent.ACTION_SENDTO, gmailUri);

            // Try to find Gmail specifically
            emailIntent.setPackage("com.google.android.gm");

            if (emailIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(emailIntent);
            } else {
                // If Gmail is not found, try any email app
                emailIntent.setPackage(null);
                try {
                    startActivity(Intent.createChooser(emailIntent, "Send email using..."));
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(this,
                            "No email app found. Please install an email app to send certificates.",
                            Toast.LENGTH_LONG).show();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this,
                    "Error sending email: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void viewCertificate(CompletedApplicant applicant) {
        DatabaseReference certificateRef = FirebaseDatabase.getInstance()
                .getReference("certificates")
                .child(applicant.getProjectId())
                .child(applicant.getUserId());

        certificateRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String certificateUrl = snapshot.child("certificateUrl").getValue(String.class);
                    if (certificateUrl != null && !certificateUrl.isEmpty()) {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(certificateUrl));
                        startActivity(intent);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CompanyCertificateActivity.this,
                        "Failed to load certificate", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteCertificate(CompletedApplicant applicant) {
        DatabaseReference certificateRef = FirebaseDatabase.getInstance()
                .getReference("certificates")
                .child(applicant.getProjectId())
                .child(applicant.getUserId());

        certificateRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String certificatePath = snapshot.child("certificatePath").getValue(String.class);
                    if (certificatePath != null && !certificatePath.isEmpty()) {
                        deleteCertificateFromDropbox(certificatePath, certificateRef);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CompanyCertificateActivity.this,
                        "Failed to delete certificate", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteCertificateFromDropbox(String filePath, DatabaseReference certificateRef) {
        OkHttpClient client = new OkHttpClient();

        try {
            JSONObject requestJson = new JSONObject();
            requestJson.put("path", filePath);

            Request request = new Request.Builder()
                    .url("https://api.dropboxapi.com/2/files/delete_v2")
                    .addHeader("Authorization", "Bearer " + DROPBOX_ACCESS_TOKEN)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(
                            MediaType.parse("application/json"),
                            requestJson.toString()))
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> Toast.makeText(CompanyCertificateActivity.this,
                            "Failed to delete certificate", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful()) {
                        certificateRef.removeValue();
                        runOnUiThread(() -> {
                            Toast.makeText(CompanyCertificateActivity.this,
                                    "Certificate deleted successfully", Toast.LENGTH_SHORT).show();
                            adapter.notifyDataSetChanged();
                        });
                    }
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Error deleting certificate", Toast.LENGTH_SHORT).show();
        }
    }
}