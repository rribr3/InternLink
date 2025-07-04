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

    private static final String DROPBOX_ACCESS_TOKEN = "sl.u.AF0p4s-AA_2SxQPlpstEqB-uKW9OoZmPN8EBqQbsCDm-hcrJk3zEw9RTV_wRBTf_49_7Gdpg6_6Phu8DoO06_LIeh865B-7E4WpzYOzw3puvGF8zKp7qOz59kYBIo5Cs4bdrub8NNHu4pMgVw8FfxdsTu7Ebb12D1y0LARMc8ZBKNcYv5SgfnyWC_i5yyfkC1XDa4DADgATKXcrAeMNYJ8bscJOSwpnTwM-qPcRNzT-2OdNG35fpNNmMdbizGX_KELCA4K0AazC-RM_2kNNhmEvgozYuxfbDBv58xpruqI6xBHOOyFOVltlkWfnTd7ulK01LGDg5rSGN5vDF1J7_8fwrrv7sBIRzx1x1gdHOUQ6zNivAuZHLSFeWdJqX6l4BqDfqBQGfiRixfQXE5l8vPAFg4iSMlQ9hE8OxQcK2COwU5GgQSylpaxL493XfEa0hy6PeUKo65d_lSP3E6s6mxX0qVAf4v98ij6N1PKh0pBvRtJIsaUT3l5N0bbgIJTb7Gz1SrczG-S5MVgyuY8W2GeIzG1bS7fPpDtN_zTpBvK-3iBiBo3GiVcpsdVMcbUCzydClb8o5ctrhLK7JEWtnFezkMeMuwL_c9FywSj-xS-cTxQ4UtQ0Z3_hOR0ZQMkqJP7r5OQ2KW0oqiSoIY7gnQcuN7U5h0WhUTEKqOra9mYtOwi7McpyryNB0oHYE7MJDHF-OmfDswlVq1BXjcoiomOth1Bz1W_Vyly-gkqo3SfYnOp68Y1XFRtaLOibfCikP3aOAAmBUVw7dJuDvKWj43t7jWXZDLXSUdymfQT1TqqUU_KZ8maQthUV5Sw34cquVURa-LkYl3LIC9AMFvx2ZkXq2wV0If1XcQDJBVDgbeIA9FY1X8WCTIX9NgEBFMeC3gy0ZokAaOqz9hHcvhKss7WoMC3pdsK-lqO5OHOZu1uJlCqFE_IP6WcqeolDNTMi-3lapY3CK-SO3hSzJrWkywMTGZQmSpt6c2YM6DKMn0AYSELOQHlTqKeYNoRXYd9v2Jpl35v4U0BmwLzwjg3Ehc--fxJe_7FlVSpZUd9VcEsodN5tiy102KmX6LTx4Zco0tDaJqgH-WMU9_vzLWbv5YJiLHQxR2vJj0LKfLGF-zA0SpXjE8TUvBbY0nBzjDYOg98ilONxR3FLVYcM9lRPRwrx6mKej-bKOOv-P_JBa_cfVBO-9B68ZZUlCPko6VwFpMf01flAlYNuZ1IF5wDl8IF-WnWiPRyqMVEEwjQuXMnuhYrl7fa-GPd_xpfXVAT4UJA9xdsJZjVpf-xkQl1VcMK_AFKfeKO9hO6F3tHoZIjrQjvjLmqZ0JGRrl0mfsIwPKhZVUCAjFa57njo6ajuOtzLDigeBqLNcvZUm3vpz3tNqT_p-huInG8uExQss2MG1eC73Jn5s0AyJ4Y3vm_yWtU-qyK6tlT3Mf9wR-MkR7AKUOQ";
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