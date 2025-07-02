package com.example.internlink;

import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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

    private static final String DROPBOX_ACCESS_TOKEN = "sl.u.AF1FaJkBGm9geTN1gVYH76CH8UNh7wOjsGIEZcQt3IcMQRb8Htwea4qYTZftALp9jonkqr1iuTvno185bQsztcYttdc6Kk0eosKOYiNJbKSWerXRyKuWWlZ7lW7w5qNI_P_2pf26fWUjDRjLZImKuOslNQWVNrXa10e6WcmPS1Ouf4lOTT_nwwz9YkWrWgIT4eqm24TCyEai5yLYvgO5TUE-lTuRsu4DP1KQ3mMWMt0Wb_Sg0xvJ_z2l3_Ysssu4YcUZEt9mAcd7dhLMcy1PVJKQUizV9WoPZQA7mCaMpcAF-lWRICXSGWsLeAlFjKEcZa6-2O-DIkvm0f8fP4pk_v8QjTw2QJPEvXZN5H0DMhki5uqBKiAtWGkpey0fjOe87bKZ-oOJsdj4FlByoydgLpqQQpgL18Xf5j1FHn627Ou-p7AGzt_5pXPbzH3CxhfLfA8m-o-FR88mH5PAzv1i3fk7UeEp2P0m_H7gSYJcsMKTBtk1W7C8GLt1e1Co-UoHWJiYbev-oRMeW5NazpFrWPteuVJFo7IOCDhh44u78mBlTE-hCZho4PK8da5Fa0E9KHw1Ow_rB2hHvyzCXaOgKOydPB6aBHSFeLRtMkvB82RM939G84uSn353Z-sQ2AlgS2NHmiE3LITz_W_9xo55x-O5IzevwZOiOcj2yQUb1WTGB5j1Btyk4cJP2xmkS5ciTKGwgHYI5JMWTVatuY22UUhFsqM2pPESkTd9u3HLnO0d6wt4oDuGYTxe1CUyfTPpCD2x6-qw2jPAoQKkKKamR1dPkcvlfck2NGImECSdi3BpL72c1mXw6IAa9XYhr3KlIp8i4wMVQxm3Ymw2yddhOhY9QvXkYl7-kYyEvnkM9EHA36GXtz-dooSH6t00JiZv-TDP-heJGj4o434-2AAD-qUz5OyOhaP-q_SI-fAsHePdSo0gHN6Bti0YhJEXk0QpT-ChRpsvnlglpHmrWtEV9ThbtQB8asXHOQQ9Di8YhdKcMvg7x0Cj4Ig_6-Ebzsz9r358wmgPoeaboqZkDmq1lUvgkogWQwR9zUyTrq7lhL6absIRAAO5ca3lLWt1uvKDWEeWYiERPDxOmv8mF8t9rBj4gioGqTSQxGdPwNk46zIRbC494iP5QKJCBuciLfCsyuGtkjPafq_M7hZSzv90zDR-l76o9RYUJ50rRPHqILa4kVrJaXsaHdKf2naUfGO0RqKKOBRT1Gjo2aWPXPtwlBXEACAd8NYHnf7nm842yhCYeAMQQp0VGRxO3nCKd7tCPjuyIQ1tR3HaMQkHdRFH4YP0yxqNlVn8uI0MLCqdMWPOqz0vg_oU2HCPlpRsUM-iS0o9KaZ-PB9lF9Ers4rsU25UI1ucK4xZK6w-5ckTdJ-odSMmd7Gf3fbXJZ6THXDQML0H45ku4GP0GhiWUpHzPtFAfM_65Q2vp_5e44s6zWK_UA";

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
        loadCompletedProjects();

        topAppBar.setNavigationOnClickListener(v -> finish());
    }

    private void initViews() {
        projectSpinner = findViewById(R.id.projectSpinner);
        applicantsRecyclerView = findViewById(R.id.applicantsRecyclerView);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        topAppBar = findViewById(R.id.topAppBar);

        applicantsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CertificateApplicantsAdapter(completedApplicants, this::handleCertificateAction);
        applicantsRecyclerView.setAdapter(adapter);

        projectSpinner.setOnItemClickListener((parent, view, position, id) -> {
            Project selectedProject = completedProjects.get(position);
            loadCompletedApplicants(selectedProject.getProjectId());
        });
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
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(CompanyCertificateActivity.this,
                                "Failed to load projects", Toast.LENGTH_SHORT).show();
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
        applicationsRef.orderByChild("projectId").equalTo(projectId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        completedApplicants.clear();
                        for (DataSnapshot appSnap : snapshot.getChildren()) {
                            String status = appSnap.child("status").getValue(String.class);
                            if ("Accepted".equals(status)) {
                                String userId = appSnap.child("userId").getValue(String.class);
                                loadApplicantDetails(userId, projectId);
                            }
                        }

                        updateUI();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(CompanyCertificateActivity.this,
                                "Failed to load applicants", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadApplicantDetails(String userId, String projectId) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
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
                        projectId
                );
                completedApplicants.add(applicant);
                adapter.notifyDataSetChanged();
                updateUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CompanyCertificateActivity.this,
                        "Failed to load applicant details", Toast.LENGTH_SHORT).show();
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
            String body = String.format(
                    "Dear Student,\n\n" +
                            "Congratulations on completing the project \"%s\"!\n\n" +
                            "Your certificate is available at:\n" +
                            "%s\n\n" +
                            "Certificate issued on: %s\n\n" +
                            "Best regards,\n" +
                            "%s",
                    projectTitle,
                    certificateUrl,
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