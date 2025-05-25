package com.example.internlink;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import android.content.Intent;

public class MyApplicants extends AppCompatActivity implements EnhancedApplicantsAdapter.OnApplicantActionListener {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private MaterialToolbar toolbar;
    private TextInputEditText searchEditText;
    private ChipGroup chipGroup;
    private View emptyState;
    private ProjectsWithApplicantsAdapter adapter;
    private List<CompanyHomeActivity.ProjectWithApplicants> projectsWithApplicantsList = new ArrayList<>();
    private List<CompanyHomeActivity.ProjectWithApplicants> filteredList = new ArrayList<>();
    private String currentFilter = "All";
    private static final String DROPBOX_ACCESS_TOKEN = "sl.u.AFvaFcbcyP-RN7Ge1VGwFnal_ugB9O6nH_xnLssUGP4P2WpAv1xXPG1zXxrSaIHyndQzLtJCXPtlhdFXC13Tz5jewEgdGwEyZWv9d8LvszV6OhBwjrTceztKBW12pNUjMktAzPnuVSlLx8TnPkq-PjdpMKCg_RMqxvXl-bKEk__6hdYAiUdT4d0MeR13Z5BEpyEEve_MVxRNgO0EqHcvcOvBdgjt2joaRYHwsAHylIzgOnsmyWoxnsZ1OioPaDPyZHU39dJU7LnygXFnO4mRCHrBt-zCF_KXxmBKNuQVVHnKAOxcdyfWa8sK57d5e4fs4A8kcNQveoHeP_hZHW06ByCHGkuRaieDB-lzNeDbXfsyi5M6w72Ofn7zV_bEVYMu0rrD8Kws1ln4vbVC66pNFNtGcwbE8Rk4Pr7p32rc3k9gp0QjITgepQmaJNERbISln5U1EUuhTnLWUb3s1X0V3cSWRCk8Os9z_qVk05bhPoy1dgIZycUjTeaWx4CvNB0B2iLuUc6VYFDpk1KX_rPn6gOyeFb-fDfVXuNCFNrwQxH9wimO81VsD4dOVtrYxU12WhWD_Cze5CnV6bk6HjU1hmr3p_swojONY4eVeEvQII5suCWiehCHrmov7i5yM3QyJUi62lJwSD8mCYXpjPujddAaJ5vsWbabtORELi2cpIfR05y6NiQR2YEr-MR5QgiZXkRaY73OdYWuNTB_4Y1OAlbn4-zRdOsuXQeaxnYEPq_vq017VNHrNlgBW-oF-atVeffyyCCvvVBhha4VeLBpd83miU-X3ck7Ed9aY4y9D2MggK56pwVukGcbQySnQH_F30UxpyAIuJTgK65JoKqUiMEKxWdeyAd6BwNIxnLfqq9rIAYvvm-OPNk7JmHoYEZBm2trWt_6kCv9HhWKNVEKIkZTGLS6ZBee59BOHRuga6bBmgXfRg8lWXWzKfvZTv82WzjGHU_fbdfsJeX3hLad10mWku32Pv7O9gx_fXtg83mdoXKkOh9nRT5OzjWNXyzuVWWh57P7SpKBaowUI3K9XIBRxxudPCw5Rs1FZbeMjvpLVUUlb5VM1GLvjFnAuuGfNOttCnY9qS8MqaGtnibPSp2UzkYQAOn-sEexDq-8NoLXRW2iGLqKE5viDOaxGMAuSFb7iT3LYkdSZ6kkjfflIPZvZQBjnto-2ec86RIu9MIzd2mxQeB3D1HmAYH2Ajm9JTkI-Wn_HiQffh_HQyL5KgoKr7BnmVa2-6Ro38ojqxXsnsGydvalGUQmrHcmazMgdmv-Yr46HkcXzbzKsfRflR693_YGRiTsQa50F6f2ZOtpB3HwsVXSh3bW8JK2GTJ2a8es-WUgCHU3neV_aEGQYGob_qx1ljsv1VdQivdNd1FYCrxstHed92YwYqAbxDsFT2HFPQ4dX36PmSu1_L4p6Ks7V-EvxhqbCmxIBdjZSaa14w";
    private String cvUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_applicants);

        initializeViews();
        setupToolbar();
        setupSearchAndFilters();
        loadApplicantsData();
    }

    private void setupSearchAndFilters() {
        // Search functionality
        if (searchEditText != null) {
            searchEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterApplicants(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        // Filter chips
        if (chipGroup != null) {
            chipGroup.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId == R.id.chip_all) {
                    currentFilter = "All";
                } else if (checkedId == R.id.chip_pending) {
                    currentFilter = "Pending";
                } else if (checkedId == R.id.chip_accepted) {
                    currentFilter = "Accepted";
                } else if (checkedId == R.id.chip_rejected) {
                    currentFilter = "Rejected";
                } else if (checkedId == R.id.chip_shortlisted) {
                    currentFilter = "Shortlisted";
                }

                filterApplicants(searchEditText.getText().toString());
            });
        }
    }

    private void filterApplicants(String query) {
        filteredList.clear();
        String lowerQuery = query.toLowerCase();

        for (CompanyHomeActivity.ProjectWithApplicants project : projectsWithApplicantsList) {
            CompanyHomeActivity.ProjectWithApplicants filteredProject = new CompanyHomeActivity.ProjectWithApplicants();
            filteredProject.setProjectId(project.getProjectId());
            filteredProject.setProjectTitle(project.getProjectTitle());

            List<Applicant> matchedApplicants = new ArrayList<>();
            for (Applicant applicant : project.getApplicants()) {
                boolean matchesQuery = applicant.getName().toLowerCase().contains(lowerQuery) ||
                        applicant.getPosition().toLowerCase().contains(lowerQuery);
                boolean matchesFilter = currentFilter.equals("All") || currentFilter.equals(applicant.getStatus());

                if (matchesQuery && matchesFilter) {
                    matchedApplicants.add(applicant);
                }
            }

            if (!matchedApplicants.isEmpty()) {
                filteredProject.setApplicants(matchedApplicants);
                filteredList.add(filteredProject);
            }
        }

        if (adapter != null) {
            adapter.updateData(filteredList);
        }

        showEmptyState(filteredList.isEmpty());
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.projects_with_applicants_recycler);
        progressBar = findViewById(R.id.progress_bar);
        toolbar = findViewById(R.id.topAppBar);
        searchEditText = findViewById(R.id.search_edit_text);
        chipGroup = findViewById(R.id.filter_chip_group);
        emptyState = findViewById(R.id.empty_state);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupToolbar() {
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void loadApplicantsData() {
        showLoading(true);
        fetchProjectsWithApplicants(projectsWithApplicants -> {
            showLoading(false);
            projectsWithApplicantsList = projectsWithApplicants;
            filteredList = new ArrayList<>(projectsWithApplicantsList);

            if (projectsWithApplicants.isEmpty()) {
                showEmptyState(true);
            } else {
                showEmptyState(false);
                adapter = new ProjectsWithApplicantsAdapter(filteredList, this);
                recyclerView.setAdapter(adapter);
            }
        });
    }

    private void showEmptyState(boolean show) {
        if (emptyState != null) {
            emptyState.setVisibility(show ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private String getCurrentCompanyUid() {
        return FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    private void fetchProjectsWithApplicants(ProjectsWithApplicantsCallback callback) {
        String companyId = getCurrentCompanyUid();
        DatabaseReference applicationsRef = FirebaseDatabase.getInstance().getReference("applications");
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        DatabaseReference projectsRef = FirebaseDatabase.getInstance().getReference("projects");

        Map<String, CompanyHomeActivity.ProjectWithApplicants> projectsMap = new HashMap<>();

        applicationsRef.orderByChild("companyId").equalTo(companyId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            callback.onProjectsWithApplicantsLoaded(new ArrayList<>());
                            return;
                        }

                        List<DataSnapshot> applicationsList = new ArrayList<>();
                        for (DataSnapshot appSnapshot : snapshot.getChildren()) {
                            applicationsList.add(appSnapshot);
                        }

                        final int[] processedCount = {0};
                        final int totalApplications = applicationsList.size();

                        for (DataSnapshot appSnapshot : applicationsList) {
                            String userId = appSnapshot.child("userId").getValue(String.class);
                            String projectId = appSnapshot.child("projectId").getValue(String.class);
                            String status = appSnapshot.child("status").getValue(String.class);
                            Long appliedDate = appSnapshot.child("appliedDate").getValue(Long.class);

                            if (userId != null && projectId != null) {
                                // Get user and project details
                                usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                                        String userName = userSnapshot.child("name").getValue(String.class);
                                        String userEmail = userSnapshot.child("email").getValue(String.class);
                                        String userBio = userSnapshot.child("bio").getValue(String.class);
                                        String userUniversity = userSnapshot.child("university").getValue(String.class);
                                        String userDegree = userSnapshot.child("degree").getValue(String.class);

                                        projectsRef.child(projectId).addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot projectSnapshot) {
                                                String projectTitle = projectSnapshot.child("title").getValue(String.class);

                                                if (userName != null && projectTitle != null) {
                                                    // Create or get existing project
                                                    if (!projectsMap.containsKey(projectId)) {
                                                        CompanyHomeActivity.ProjectWithApplicants project = new CompanyHomeActivity.ProjectWithApplicants();
                                                        project.setProjectId(projectId);
                                                        project.setProjectTitle(projectTitle);
                                                        project.setApplicants(new ArrayList<>());
                                                        projectsMap.put(projectId, project);
                                                    }

                                                    // Create applicant with more details
                                                    Applicant applicant = new Applicant(
                                                            userName,
                                                            createApplicantPosition(userBio, userUniversity, userDegree),
                                                            status != null ? status : "Pending",
                                                            R.drawable.ic_profile
                                                    );
                                                    applicant.setUserId(userId);
                                                    applicant.setProjectId(projectId);
                                                    applicant.setAppliedDate(appliedDate);

                                                    projectsMap.get(projectId).getApplicants().add(applicant);
                                                }

                                                processedCount[0]++;
                                                if (processedCount[0] == totalApplications) {
                                                    // Convert map to list and sort applicants by date
                                                    List<CompanyHomeActivity.ProjectWithApplicants> result = new ArrayList<>(projectsMap.values());
                                                    for (CompanyHomeActivity.ProjectWithApplicants project : result) {
                                                        Collections.sort(project.getApplicants(), (a, b) -> {
                                                            Long dateA = a.getAppliedDate();
                                                            Long dateB = b.getAppliedDate();
                                                            if (dateA == null) dateA = 0L;
                                                            if (dateB == null) dateB = 0L;
                                                            return Long.compare(dateB, dateA); // Most recent first
                                                        });
                                                    }
                                                    callback.onProjectsWithApplicantsLoaded(result);
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {
                                                processedCount[0]++;
                                                if (processedCount[0] == totalApplications) {
                                                    callback.onProjectsWithApplicantsLoaded(new ArrayList<>(projectsMap.values()));
                                                }
                                            }
                                        });
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        processedCount[0]++;
                                        if (processedCount[0] == totalApplications) {
                                            callback.onProjectsWithApplicantsLoaded(new ArrayList<>(projectsMap.values()));
                                        }
                                    }
                                });
                            } else {
                                processedCount[0]++;
                                if (processedCount[0] == totalApplications) {
                                    callback.onProjectsWithApplicantsLoaded(new ArrayList<>(projectsMap.values()));
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(MyApplicants.this,
                                "Failed to load applicants: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        callback.onProjectsWithApplicantsLoaded(new ArrayList<>());
                    }
                });
    }

    private String createApplicantPosition(String bio, String university, String degree) {
        StringBuilder position = new StringBuilder();

        if (bio != null && !bio.trim().isEmpty()) {
            position.append(bio.trim());
        }

        if (university != null && !university.trim().isEmpty()) {
            if (position.length() > 0) {
                position.append(" at ");
            }
            position.append(university.trim());
        }

        if (degree != null && !degree.trim().isEmpty()) {
            if (position.length() > 0) {
                position.append(" - ");
            }
            position.append(degree.trim());
        }

        // If no details available, return a default
        if (position.length() == 0) {
            return "Student Applicant";
        }

        return position.toString();
    }

    // EnhancedApplicantsAdapter.OnApplicantActionListener implementation
    @Override
    public void onViewProfile(Applicant applicant) {
        // Implementation for viewing profile
        Intent intent = new Intent(this, ApplicantProfileActivity.class);
        intent.putExtra("APPLICANT_ID", applicant.getUserId());
        intent.putExtra("APPLICANT_NAME", applicant.getName());
        startActivity(intent);
    }

    @Override
    public void onScheduleInterview(Applicant applicant) {
        // First check if interview is already scheduled
        fetchInterviewDetails(applicant, (interviewDate, interviewTime) -> {
            if (interviewDate != null && interviewTime != null) {
                // Interview already scheduled - show details with beautiful design
                showInterviewDetailsDialog(applicant, interviewDate, interviewTime);
            } else {
                // No interview scheduled - show scheduling dialog
                showScheduleInterviewDialog(applicant);
            }
        });
    }

    @Override
    public void onChat(Applicant applicant) {
        // Implementation for chat
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("CHAT_WITH_ID", applicant.getUserId());
        intent.putExtra("CHAT_WITH_NAME", applicant.getName());
        startActivity(intent);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onMoreOptions(Applicant applicant, View anchorView) {
        PopupMenu popup = new PopupMenu(this, anchorView);
        popup.getMenuInflater().inflate(R.menu.applicant_options_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();

            if (id == R.id.menu_view_cv) {
                // Fetch CV URL for this specific applicant
                fetchApplicantCvUrl(applicant.getUserId(), cvUrl -> {
                    if (cvUrl != null && !cvUrl.isEmpty()) {
                        Intent intent = new Intent(MyApplicants.this, PdfViewerActivity.class);
                        intent.putExtra("pdf_url", cvUrl);
                        startActivity(intent);
                    } else {
                        Toast.makeText(this, "No CV uploaded", Toast.LENGTH_SHORT).show();
                    }
                });
                return true;
            } else if (id == R.id.menu_shortlist) {
                // Show schedule interview dialog and update status to Shortlisted
                showScheduleInterviewDialog(applicant);
                return true;
            } else if (id == R.id.menu_accept) {
                // Update status to Accepted
                updateApplicationStatus(applicant, "Accepted");
                return true;
            } else if (id == R.id.menu_reject) {
                // Update status to Rejected
                updateApplicationStatus(applicant, "Rejected");
                return true;
            } else {
                return false;
            }
        });

        popup.show();
    }

    /**
     * Fetches interview details from Firebase for a specific applicant
     */
    private void fetchInterviewDetails(Applicant applicant, InterviewDetailsCallback callback) {
        String companyId = getCurrentCompanyUid();
        DatabaseReference applicationsRef = FirebaseDatabase.getInstance().getReference("applications");

        applicationsRef.orderByChild("companyId").equalTo(companyId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot appSnapshot : snapshot.getChildren()) {
                            String userId = appSnapshot.child("userId").getValue(String.class);
                            String projectId = appSnapshot.child("projectId").getValue(String.class);

                            if (userId != null && userId.equals(applicant.getUserId()) &&
                                    projectId != null && projectId.equals(applicant.getProjectId())) {

                                String interviewDate = appSnapshot.child("interviewDate").getValue(String.class);
                                String interviewTime = appSnapshot.child("interviewTime").getValue(String.class);

                                callback.onInterviewDetailsFetched(interviewDate, interviewTime);
                                return;
                            }
                        }
                        // No interview found
                        callback.onInterviewDetailsFetched(null, null);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(MyApplicants.this, "Failed to fetch interview details", Toast.LENGTH_SHORT).show();
                        callback.onInterviewDetailsFetched(null, null);
                    }
                });
    }

    /**
     * Shows a beautifully designed dialog with interview details
     */
    private void showInterviewDetailsDialog(Applicant applicant, String interviewDate, String interviewTime) {
        // First fetch complete interview details
        fetchCompleteInterviewDetails(applicant, (date, time, type, method, location, zoomLink) -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            // Create custom view
            LinearLayout dialogLayout = new LinearLayout(this);
            dialogLayout.setOrientation(LinearLayout.VERTICAL);
            dialogLayout.setPadding(60, 40, 60, 40);

            // Title
            TextView titleView = new TextView(this);
            titleView.setText("📅 Interview Scheduled");
            titleView.setTextSize(20);
            titleView.setTextColor(getResources().getColor(android.R.color.black));
            titleView.setPadding(0, 0, 0, 30);
            titleView.setTypeface(null, android.graphics.Typeface.BOLD);
            dialogLayout.addView(titleView);

            // Applicant name
            TextView nameView = new TextView(this);
            nameView.setText("👤 " + applicant.getName());
            nameView.setTextSize(16);
            nameView.setTextColor(getResources().getColor(android.R.color.black));
            nameView.setPadding(0, 0, 0, 20);
            dialogLayout.addView(nameView);

            // Date info
            TextView dateView = new TextView(this);
            dateView.setText("📆 Date: " + date);
            dateView.setTextSize(16);
            dateView.setTextColor(getResources().getColor(android.R.color.black));
            dateView.setPadding(0, 0, 0, 15);
            dialogLayout.addView(dateView);

            // Time info
            TextView timeView = new TextView(this);
            timeView.setText("🕐 Time: " + time);
            timeView.setTextSize(16);
            timeView.setTextColor(getResources().getColor(android.R.color.black));
            timeView.setPadding(0, 0, 0, 15);
            dialogLayout.addView(timeView);

            // Interview type and details
            if (type != null && !type.isEmpty()) {
                TextView typeView = new TextView(this);
                String typeText = "📱 Type: " + type;
                if (type.equals("Online") && method != null && !method.isEmpty()) {
                    typeText += " (" + method + ")";
                }
                typeView.setText(typeText);
                typeView.setTextSize(16);
                typeView.setTextColor(getResources().getColor(android.R.color.black));
                typeView.setPadding(0, 0, 0, 15);
                dialogLayout.addView(typeView);

                // Show location or zoom link
                if (type.equals("In-person") && location != null && !location.isEmpty()) {
                    TextView locationView = new TextView(this);
                    locationView.setText("📍 Location: " + location);
                    locationView.setTextSize(14);
                    locationView.setTextColor(getResources().getColor(android.R.color.black));
                    locationView.setPadding(0, 0, 0, 15);
                    dialogLayout.addView(locationView);
                } else if (type.equals("Online") && method != null && method.equals("Zoom") && zoomLink != null && !zoomLink.isEmpty()) {
                    TextView linkView = new TextView(this);
                    linkView.setText("🔗 Zoom Link:");
                    linkView.setTextSize(14);
                    linkView.setTextColor(getResources().getColor(android.R.color.black));
                    linkView.setPadding(0, 0, 0, 5);
                    dialogLayout.addView(linkView);

                    TextView linkUrlView = new TextView(this);
                    linkUrlView.setText(zoomLink);
                    linkUrlView.setTextSize(12);
                    linkUrlView.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
                    linkUrlView.setPadding(0, 0, 0, 15);
                    linkUrlView.setPaintFlags(linkUrlView.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
                    dialogLayout.addView(linkUrlView);
                }
            }

            // Status info
            TextView statusView = new TextView(this);
            statusView.setText("📋 Status: " + applicant.getStatus());
            statusView.setTextSize(14);
            statusView.setTextColor(getResources().getColor(android.R.color.darker_gray));
            statusView.setPadding(0, 0, 0, 30);
            dialogLayout.addView(statusView);

            builder.setView(dialogLayout);

            // Add buttons
            builder.setPositiveButton("✏ Reschedule", (dialog, which) -> {
                showRescheduleInterviewDialog(applicant, date, time);
            });

            builder.setNegativeButton("❌ Cancel Interview", (dialog, which) -> {
                showCancelInterviewConfirmation(applicant);
            });

            builder.setNeutralButton("Close", (dialog, which) -> dialog.dismiss());

            AlertDialog dialog = builder.create();
            dialog.show();

            // Style the buttons
            if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
            }
            if (dialog.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            }
        });
    }
    private void fetchCompleteInterviewDetails(Applicant applicant, CompleteInterviewDetailsCallback callback) {
        String companyId = getCurrentCompanyUid();
        DatabaseReference applicationsRef = FirebaseDatabase.getInstance().getReference("applications");

        applicationsRef.orderByChild("companyId").equalTo(companyId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot appSnapshot : snapshot.getChildren()) {
                            String userId = appSnapshot.child("userId").getValue(String.class);
                            String projectId = appSnapshot.child("projectId").getValue(String.class);

                            if (userId != null && userId.equals(applicant.getUserId()) &&
                                    projectId != null && projectId.equals(applicant.getProjectId())) {

                                String interviewDate = appSnapshot.child("interviewDate").getValue(String.class);
                                String interviewTime = appSnapshot.child("interviewTime").getValue(String.class);
                                String interviewType = appSnapshot.child("interviewType").getValue(String.class);
                                String interviewMethod = appSnapshot.child("interviewMethod").getValue(String.class);
                                String interviewLocation = appSnapshot.child("interviewLocation").getValue(String.class);
                                String zoomLink = appSnapshot.child("zoomLink").getValue(String.class);

                                callback.onDetailsRetrieved(interviewDate, interviewTime, interviewType,
                                        interviewMethod, interviewLocation, zoomLink);
                                return;
                            }
                        }
                        // No interview found
                        callback.onDetailsRetrieved(null, null, null, null, null, null);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(MyApplicants.this, "Failed to fetch interview details", Toast.LENGTH_SHORT).show();
                        callback.onDetailsRetrieved(null, null, null, null, null, null);
                    }
                });
    }
    public interface CompleteInterviewDetailsCallback {
        void onDetailsRetrieved(String interviewDate, String interviewTime, String interviewType,
                                String interviewMethod, String interviewLocation, String zoomLink);
    }

    /**
     * Shows reschedule interview dialog
     */
    private void showRescheduleInterviewDialog(Applicant applicant, String currentDate, String currentTime) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Create layout
        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(60, 40, 60, 40);

        // Title
        TextView titleView = new TextView(this);
        titleView.setText("📝 Reschedule Interview");
        titleView.setTextSize(18);
        titleView.setTextColor(getResources().getColor(android.R.color.black));
        titleView.setPadding(0, 0, 0, 20);
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        dialogLayout.addView(titleView);

        // Applicant name
        TextView nameView = new TextView(this);
        nameView.setText("Applicant: " + applicant.getName());
        nameView.setTextSize(16);
        nameView.setTextColor(getResources().getColor(android.R.color.black));
        nameView.setPadding(0, 0, 0, 15);
        dialogLayout.addView(nameView);

        // Current schedule info
        TextView currentView = new TextView(this);
        currentView.setText("Current: " + currentDate + " at " + currentTime);
        currentView.setTextSize(14);
        currentView.setTextColor(getResources().getColor(android.R.color.darker_gray));
        currentView.setPadding(0, 0, 0, 25);
        currentView.setBackgroundColor(getResources().getColor(android.R.color.background_light));
        currentView.setPadding(20, 15, 20, 15);
        dialogLayout.addView(currentView);

        // Add some space
        View spacer1 = new View(this);
        spacer1.setLayoutParams(new LinearLayout.LayoutParams(0, 20));
        dialogLayout.addView(spacer1);

        // New date input
        TextView dateLabel = new TextView(this);
        dateLabel.setText("📅 New Date:");
        dateLabel.setTextSize(14);
        dateLabel.setTextColor(getResources().getColor(android.R.color.black));
        dateLabel.setPadding(0, 0, 0, 10);
        dialogLayout.addView(dateLabel);

        EditText inputDate = new EditText(this);
        inputDate.setHint("Select new date");
        inputDate.setFocusable(false);
        inputDate.setClickable(true);
        inputDate.setPadding(20, 15, 20, 15);
        inputDate.setBackground(getResources().getDrawable(android.R.drawable.edit_text));
        dialogLayout.addView(inputDate);

        // Add some space
        View spacer2 = new View(this);
        spacer2.setLayoutParams(new LinearLayout.LayoutParams(0, 15));
        dialogLayout.addView(spacer2);

        // New time input
        TextView timeLabel = new TextView(this);
        timeLabel.setText("🕐 New Time:");
        timeLabel.setTextSize(14);
        timeLabel.setTextColor(getResources().getColor(android.R.color.black));
        timeLabel.setPadding(0, 0, 0, 10);
        dialogLayout.addView(timeLabel);

        EditText inputTime = new EditText(this);
        inputTime.setHint("Select new time");
        inputTime.setFocusable(false);
        inputTime.setClickable(true);
        inputTime.setPadding(20, 15, 20, 15);
        inputTime.setBackground(getResources().getDrawable(android.R.drawable.edit_text));
        dialogLayout.addView(inputTime);

        builder.setView(dialogLayout);

        // Date picker
        inputDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                calendar.set(year, month, dayOfMonth);
                SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
                inputDate.setText(sdf.format(calendar.getTime()));
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Time picker
        inputTime.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);
                SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                inputTime.setText(sdf.format(calendar.getTime()));
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show();
        });

        builder.setPositiveButton("✅ Update", (dialog, which) -> {
            String newDate = inputDate.getText().toString().trim();
            String newTime = inputTime.getText().toString().trim();

            if (!newDate.isEmpty() && !newTime.isEmpty()) {
                updateInterviewSchedule(applicant, newDate, newTime);
            } else {
                Toast.makeText(this, "Please select new date and time", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        // Style the buttons
        if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        }
    }

    /**
     * Shows confirmation dialog for canceling interview
     */
    private void showCancelInterviewConfirmation(Applicant applicant) {
        new AlertDialog.Builder(this)
                .setTitle("❌ Cancel Interview")
                .setMessage("Are you sure you want to cancel the interview with " + applicant.getName() + "?\n\nThis will change their status back to 'Under Review'.")
                .setPositiveButton("Yes, Cancel", (dialog, which) -> {
                    cancelInterview(applicant);
                })
                .setNegativeButton("No", null)
                .show();
    }

    /**
     * Updates interview schedule in Firebase
     */
    private void updateInterviewSchedule(Applicant applicant, String newDate, String newTime) {
        String companyId = getCurrentCompanyUid();
        DatabaseReference applicationsRef = FirebaseDatabase.getInstance().getReference("applications");

        applicationsRef.orderByChild("companyId").equalTo(companyId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot appSnapshot : snapshot.getChildren()) {
                            String userId = appSnapshot.child("userId").getValue(String.class);
                            String projectId = appSnapshot.child("projectId").getValue(String.class);

                            if (userId != null && userId.equals(applicant.getUserId()) &&
                                    projectId != null && projectId.equals(applicant.getProjectId())) {

                                String applicationId = appSnapshot.getKey();
                                Map<String, Object> updates = new HashMap<>();
                                updates.put("interviewDate", newDate);
                                updates.put("interviewTime", newTime);
                                updates.put("lastUpdated", System.currentTimeMillis());

                                applicationsRef.child(applicationId).updateChildren(updates)
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(MyApplicants.this, "✅ Interview rescheduled successfully!", Toast.LENGTH_SHORT).show();
                                            // Create reschedule announcement
                                            createRescheduleAnnouncement(companyId, projectId, applicant, newDate, newTime);
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(MyApplicants.this, "❌ Failed to reschedule: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                                break;
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(MyApplicants.this, "Failed to update schedule", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Cancels interview and updates status
     */
    private void cancelInterview(Applicant applicant) {
        String companyId = getCurrentCompanyUid();
        DatabaseReference applicationsRef = FirebaseDatabase.getInstance().getReference("applications");

        applicationsRef.orderByChild("companyId").equalTo(companyId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot appSnapshot : snapshot.getChildren()) {
                            String userId = appSnapshot.child("userId").getValue(String.class);
                            String projectId = appSnapshot.child("projectId").getValue(String.class);

                            if (userId != null && userId.equals(applicant.getUserId()) &&
                                    projectId != null && projectId.equals(applicant.getProjectId())) {

                                String applicationId = appSnapshot.getKey();
                                Map<String, Object> updates = new HashMap<>();
                                updates.put("status", "Under Review");
                                updates.put("interviewDate", null);
                                updates.put("interviewTime", null);
                                updates.put("lastUpdated", System.currentTimeMillis());

                                applicationsRef.child(applicationId).updateChildren(updates)
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(MyApplicants.this, "❌ Interview cancelled", Toast.LENGTH_SHORT).show();

                                            // Update local data
                                            applicant.setStatus("Under Review");
                                            updateLocalApplicantStatus(applicant.getUserId(), applicant.getProjectId(), "Under Review");

                                            if (adapter != null) adapter.notifyDataSetChanged();

                                            // Create cancellation announcement
                                            createCancellationAnnouncement(companyId, projectId, applicant);
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(MyApplicants.this, "Failed to cancel: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                                break;
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(MyApplicants.this, "Failed to cancel interview", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Creates announcement for interview reschedule
     */
    private void createRescheduleAnnouncement(String companyId, String projectId, Applicant applicant, String newDate, String newTime) {
        DatabaseReference announcementsRef = FirebaseDatabase.getInstance()
                .getReference("announcements_by_role").child("student");
        DatabaseReference companyRef = FirebaseDatabase.getInstance().getReference("users").child(companyId);
        DatabaseReference projectRef = FirebaseDatabase.getInstance().getReference("projects").child(projectId);

        companyRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot companySnapshot) {
                String companyName = companySnapshot.child("name").getValue(String.class);

                projectRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot projectSnapshot) {
                        String projectTitle = projectSnapshot.child("title").getValue(String.class);

                        String message = "📅 Your interview for \"" + projectTitle + "\" with \"" + companyName +
                                "\" has been rescheduled to " + newDate + " at " + newTime + ".\n\n[View Details]";

                        Map<String, Object> announceData = new HashMap<>();
                        announceData.put("title", "Interview Rescheduled");
                        announceData.put("message", message);
                        announceData.put("timestamp", System.currentTimeMillis());
                        announceData.put("applicant_status", "Shortlisted");
                        announceData.put("recipientId", applicant.getUserId());

                        announcementsRef.push().setValue(announceData);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    /**
     * Creates announcement for interview cancellation
     */
    private void createCancellationAnnouncement(String companyId, String projectId, Applicant applicant) {
        DatabaseReference announcementsRef = FirebaseDatabase.getInstance()
                .getReference("announcements_by_role").child("student");
        DatabaseReference companyRef = FirebaseDatabase.getInstance().getReference("users").child(companyId);
        DatabaseReference projectRef = FirebaseDatabase.getInstance().getReference("projects").child(projectId);

        companyRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot companySnapshot) {
                String companyName = companySnapshot.child("name").getValue(String.class);

                projectRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot projectSnapshot) {
                        String projectTitle = projectSnapshot.child("title").getValue(String.class);

                        String message = "❌ Your interview for \"" + projectTitle + "\" with \"" + companyName +
                                "\" has been cancelled. Your application is now under review.\n\n[View Status]";

                        Map<String, Object> announceData = new HashMap<>();
                        announceData.put("title", "Interview Cancelled");
                        announceData.put("message", message);
                        announceData.put("timestamp", System.currentTimeMillis());
                        announceData.put("applicant_status", "Under Review");
                        announceData.put("recipientId", applicant.getUserId());

                        announcementsRef.push().setValue(announceData);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showScheduleInterviewDialog(Applicant applicant) {
        AlertDialog dialog;
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_schedule_interview, null);

        // Find views
        TextView nameTextView = dialogView.findViewById(R.id.dialog_applicant_name);
        EditText inputDate = dialogView.findViewById(R.id.et_interview_date);
        EditText inputTime = dialogView.findViewById(R.id.et_interview_time);

        // Interview type views
        RadioGroup rgInterviewType = dialogView.findViewById(R.id.rg_interview_type);
        RadioButton rbOnline = dialogView.findViewById(R.id.rb_online);
        RadioButton rbInPerson = dialogView.findViewById(R.id.rb_in_person);

        // Online options
        LinearLayout llOnlineOptions = dialogView.findViewById(R.id.ll_online_options);
        RadioGroup rgOnlineMethod = dialogView.findViewById(R.id.rg_online_method);
        RadioButton rbChat = dialogView.findViewById(R.id.rb_chat);
        RadioButton rbZoom = dialogView.findViewById(R.id.rb_zoom);
        LinearLayout llZoomLink = dialogView.findViewById(R.id.ll_zoom_link);
        EditText etZoomLink = dialogView.findViewById(R.id.et_zoom_link);

        // In-person options
        LinearLayout llLocation = dialogView.findViewById(R.id.ll_location);
        EditText etLocation = dialogView.findViewById(R.id.et_location);

        // Set applicant name dynamically
        nameTextView.setText("Schedule interview with " + applicant.getName());

        // Disable keyboard input for date and time
        inputDate.setKeyListener(null);
        inputTime.setKeyListener(null);

        // Handle interview type selection
        rgInterviewType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_online) {
                llOnlineOptions.setVisibility(View.VISIBLE);
                llLocation.setVisibility(View.GONE);
                etLocation.setText(""); // Clear location if switching from in-person
            } else if (checkedId == R.id.rb_in_person) {
                llOnlineOptions.setVisibility(View.GONE);
                llZoomLink.setVisibility(View.GONE);
                llLocation.setVisibility(View.VISIBLE);
                etZoomLink.setText(""); // Clear zoom link if switching from online
            }
        });

        // Handle online method selection
        rgOnlineMethod.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_zoom) {
                llZoomLink.setVisibility(View.VISIBLE);
            } else if (checkedId == R.id.rb_chat) {
                llZoomLink.setVisibility(View.GONE);
                etZoomLink.setText(""); // Clear zoom link if switching to chat
            }
        });

        // Date picker
        inputDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                calendar.set(year, month, dayOfMonth);
                SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
                inputDate.setText(sdf.format(calendar.getTime()));
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Time picker
        inputTime.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);
                SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                inputTime.setText(sdf.format(calendar.getTime()));
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show();
        });

        dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        dialogView.findViewById(R.id.btn_schedule).setOnClickListener(v -> {
            String date = inputDate.getText().toString().trim();
            String time = inputTime.getText().toString().trim();

            // Validate basic fields
            if (date.isEmpty() || time.isEmpty()) {
                Toast.makeText(this, "Please enter date and time", Toast.LENGTH_SHORT).show();
                return;
            }

            // Validate interview type selection
            if (rgInterviewType.getCheckedRadioButtonId() == -1) {
                Toast.makeText(this, "Please select interview type", Toast.LENGTH_SHORT).show();
                return;
            }

            // Prepare interview details
            String interviewType = "";
            String interviewMethod = "";
            String interviewLocation = "";
            String zoomLink = "";

            if (rbOnline.isChecked()) {
                interviewType = "Online";

                // Validate online method selection
                if (rgOnlineMethod.getCheckedRadioButtonId() == -1) {
                    Toast.makeText(this, "Please select online interview method", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (rbChat.isChecked()) {
                    interviewMethod = "Chat";
                } else if (rbZoom.isChecked()) {
                    interviewMethod = "Zoom";
                    zoomLink = etZoomLink.getText().toString().trim();

                    // Validate zoom link
                    if (zoomLink.isEmpty()) {
                        Toast.makeText(this, "Please enter Zoom meeting link", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
            } else if (rbInPerson.isChecked()) {
                interviewType = "In-person";
                interviewLocation = etLocation.getText().toString().trim();

                // Validate location
                if (interviewLocation.isEmpty()) {
                    Toast.makeText(this, "Please enter interview location", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            // Save interview with all details
            saveInterviewToDatabase(applicant, date, time, interviewType, interviewMethod, interviewLocation, zoomLink);
            dialog.dismiss();
        });

        dialogView.findViewById(R.id.btn_cancel).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void saveInterviewToDatabase(Applicant applicant, String date, String time,
                                         String interviewType, String interviewMethod,
                                         String interviewLocation, String zoomLink) {
        String companyId = getCurrentCompanyUid();
        DatabaseReference applicationsRef = FirebaseDatabase.getInstance().getReference("applications");
        DatabaseReference announcementsRef = FirebaseDatabase.getInstance().getReference("announcements_by_role").child("student");

        applicationsRef.orderByChild("companyId").equalTo(companyId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean found = false;

                        for (DataSnapshot appSnapshot : snapshot.getChildren()) {
                            String userId = appSnapshot.child("userId").getValue(String.class);
                            String projectId = appSnapshot.child("projectId").getValue(String.class);

                            if (userId != null && userId.equals(applicant.getUserId()) &&
                                    projectId != null && projectId.equals(applicant.getProjectId())) {

                                found = true;
                                String applicationId = appSnapshot.getKey();

                                // Update application status and interview details
                                Map<String, Object> updates = new HashMap<>();
                                updates.put("status", "Shortlisted");
                                updates.put("interviewDate", date);
                                updates.put("interviewTime", time);
                                updates.put("interviewType", interviewType);
                                updates.put("interviewMethod", interviewMethod);
                                updates.put("interviewLocation", interviewLocation);
                                updates.put("zoomLink", zoomLink);
                                updates.put("lastUpdated", System.currentTimeMillis());

                                DatabaseReference appRef = applicationsRef.child(applicationId);
                                appRef.updateChildren(updates)
                                        .addOnSuccessListener(aVoid -> {
                                            android.util.Log.d("Interview", "Successfully scheduled interview and updated status");

                                            Toast.makeText(MyApplicants.this, "✅ Interview scheduled successfully!", Toast.LENGTH_SHORT).show();

                                            // Update local data
                                            applicant.setStatus("Shortlisted");
                                            updateLocalApplicantStatus(applicant.getUserId(), applicant.getProjectId(), "Shortlisted");

                                            if (adapter != null) adapter.notifyDataSetChanged();

                                            // Create announcement with interview details
                                            createInterviewAnnouncement(companyId, projectId, applicant, date, time,
                                                    interviewType, interviewMethod, interviewLocation,
                                                    zoomLink, announcementsRef);
                                        })
                                        .addOnFailureListener(e -> {
                                            android.util.Log.e("Interview", "Failed to schedule interview", e);
                                            Toast.makeText(MyApplicants.this,
                                                    "❌ Failed to schedule interview: " + e.getMessage(),
                                                    Toast.LENGTH_SHORT).show();
                                        });
                                break;
                            }
                        }

                        if (!found) {
                            Toast.makeText(MyApplicants.this, "Application not found", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(MyApplicants.this, "Failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void createInterviewAnnouncement(String companyId, String projectId, Applicant applicant,
                                             String date, String time, String interviewType,
                                             String interviewMethod, String interviewLocation,
                                             String zoomLink, DatabaseReference announcementsRef) {
        DatabaseReference companyRef = FirebaseDatabase.getInstance().getReference("users").child(companyId);
        companyRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot companySnapshot) {
                String companyName = companySnapshot.child("name").getValue(String.class);

                DatabaseReference projectRef = FirebaseDatabase.getInstance().getReference("projects").child(projectId);
                projectRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot projectSnapshot) {
                        String projectTitle = projectSnapshot.child("title").getValue(String.class);

                        // Build detailed message based on interview type
                        StringBuilder messageBuilder = new StringBuilder();
                        messageBuilder.append("📅 Your interview for \"").append(projectTitle)
                                .append("\" with \"").append(companyName)
                                .append("\" is scheduled on ").append(date)
                                .append(" at ").append(time).append(".\n\n");

                        if (interviewType.equals("Online")) {
                            messageBuilder.append("📱 Interview Type: Online");
                            if (interviewMethod.equals("Chat")) {
                                messageBuilder.append(" (Chat)\n");
                                messageBuilder.append("💬 Please be available for chat at the scheduled time.");
                            } else if (interviewMethod.equals("Zoom")) {
                                messageBuilder.append(" (Zoom Meeting)\n");
                                messageBuilder.append("🔗 Zoom Link: ").append(zoomLink);
                            }
                        } else if (interviewType.equals("In-person")) {
                            messageBuilder.append("🏢 Interview Type: In-person\n");
                            messageBuilder.append("📍 Location: ").append(interviewLocation);
                        }

                        messageBuilder.append("\n\n[View Details]");

                        Map<String, Object> announceData = new HashMap<>();
                        announceData.put("title", "Interview Scheduled");
                        announceData.put("message", messageBuilder.toString());
                        announceData.put("timestamp", System.currentTimeMillis());
                        announceData.put("applicant_status", "Shortlisted");
                        announceData.put("recipientId", applicant.getUserId());
                        announceData.put("interviewType", interviewType);
                        announceData.put("interviewMethod", interviewMethod);

                        announcementsRef.push().setValue(announceData)
                                .addOnSuccessListener(aVoid -> android.util.Log.d("Announcement", "Created successfully"))
                                .addOnFailureListener(e -> android.util.Log.e("Announcement", "Failed to create", e));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(MyApplicants.this, "Failed to fetch project title", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MyApplicants.this, "Failed to fetch company name", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void fetchApplicantCvUrl(String userId, CvUrlCallback callback) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String cvUrl = snapshot.child("cvUrl").getValue(String.class);
                callback.onCvUrlFetched(cvUrl);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MyApplicants.this,
                        "Failed to fetch CV: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
                callback.onCvUrlFetched(null);
            }
        });
    }

    /**
     * Updates the application status in Firebase for a specific applicant
     * @param applicant The applicant whose status needs to be updated
     * @param newStatus The new status to set (Accepted, Rejected, Shortlisted, etc.)
     */
    private void updateApplicationStatus(Applicant applicant, String newStatus) {
        String companyId = getCurrentCompanyUid();
        DatabaseReference applicationsRef = FirebaseDatabase.getInstance().getReference("applications");

        // Show progress to user
        Toast.makeText(this, "Updating status...", Toast.LENGTH_SHORT).show();

        // Find and update the application status
        applicationsRef.orderByChild("companyId").equalTo(companyId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean found = false;

                        for (DataSnapshot appSnapshot : snapshot.getChildren()) {
                            String userId = appSnapshot.child("userId").getValue(String.class);
                            String projectId = appSnapshot.child("projectId").getValue(String.class);

                            // Debug logging
                            android.util.Log.d("UpdateStatus", "Checking: userId=" + userId +
                                    ", projectId=" + projectId +
                                    ", applicantUserId=" + applicant.getUserId() +
                                    ", applicantProjectId=" + applicant.getProjectId());

                            if (userId != null && userId.equals(applicant.getUserId()) &&
                                    projectId != null && projectId.equals(applicant.getProjectId())) {

                                found = true;
                                String applicationId = appSnapshot.getKey();

                                // Log the current status before update
                                String currentStatus = appSnapshot.child("status").getValue(String.class);
                                android.util.Log.d("UpdateStatus", "Found application: " + applicationId +
                                        ", current status: " + currentStatus +
                                        ", new status: " + newStatus);

                                // Create updates map
                                Map<String, Object> updates = new HashMap<>();
                                updates.put("status", newStatus);
                                updates.put("lastUpdated", System.currentTimeMillis());

                                // Update status in Firebase with explicit path
                                DatabaseReference appRef = applicationsRef.child(applicationId);
                                appRef.updateChildren(updates)
                                        .addOnSuccessListener(aVoid -> {
                                            android.util.Log.d("UpdateStatus", "Successfully updated status to: " + newStatus);

                                            Toast.makeText(MyApplicants.this,
                                                    "✅ " + applicant.getName() + " " + newStatus.toLowerCase(),
                                                    Toast.LENGTH_SHORT).show();

                                            // Update local data
                                            applicant.setStatus(newStatus);

                                            // Update the filtered list
                                            updateLocalApplicantStatus(applicant.getUserId(), applicant.getProjectId(), newStatus);

                                            // Refresh the adapter
                                            if (adapter != null) {
                                                adapter.notifyDataSetChanged();
                                            }

                                            // Create announcement for status change (except for Shortlisted which is handled in interview scheduling)
                                            if (!newStatus.equals("Shortlisted")) {
                                                createStatusChangeAnnouncement(companyId, applicant.getProjectId(), applicant, newStatus);
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            android.util.Log.e("UpdateStatus", "Failed to update status", e);
                                            Toast.makeText(MyApplicants.this,
                                                    "❌ Failed to update status: " + e.getMessage(),
                                                    Toast.LENGTH_SHORT).show();
                                        });
                                break;
                            }
                        }

                        if (!found) {
                            android.util.Log.e("UpdateStatus", "Application not found for user: " +
                                    applicant.getUserId() + ", project: " + applicant.getProjectId());
                            Toast.makeText(MyApplicants.this,
                                    "Application not found",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        android.util.Log.e("UpdateStatus", "Database error", error.toException());
                        Toast.makeText(MyApplicants.this,
                                "Failed to update status: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Creates an announcement for status changes (Accept/Reject)
     * @param companyId The company ID
     * @param projectId The project ID
     * @param applicant The applicant
     * @param newStatus The new status
     */
    private void createStatusChangeAnnouncement(String companyId, String projectId, Applicant applicant, String newStatus) {
        DatabaseReference announcementsRef = FirebaseDatabase.getInstance()
                .getReference("announcements_by_role").child("student");
        DatabaseReference companyRef = FirebaseDatabase.getInstance().getReference("users").child(companyId);
        DatabaseReference projectRef = FirebaseDatabase.getInstance().getReference("projects").child(projectId);

        companyRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot companySnapshot) {
                String companyName = companySnapshot.child("name").getValue(String.class);

                projectRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot projectSnapshot) {
                        String projectTitle = projectSnapshot.child("title").getValue(String.class);

                        String title = "Application " + newStatus;
                        String message;

                        if (newStatus.equals("Accepted")) {
                            message = "🎉 Congratulations! Your application for \"" + projectTitle +
                                    "\" at \"" + companyName + "\" has been accepted.\n\n[View Details]";
                        } else if (newStatus.equals("Rejected")) {
                            message = "😔 Your application for \"" + projectTitle +
                                    "\" at \"" + companyName + "\" has been reviewed. Better luck next time!\n\n[View Other Opportunities]";
                        } else {
                            message = "📋 Your application status for \"" + projectTitle +
                                    "\" at \"" + companyName + "\" has been updated to " + newStatus + ".\n\n[View Details]";
                        }

                        Map<String, Object> announceData = new HashMap<>();
                        announceData.put("title", title);
                        announceData.put("message", message);
                        announceData.put("timestamp", System.currentTimeMillis());
                        announceData.put("applicant_status", newStatus);
                        announceData.put("recipientId", applicant.getUserId());

                        announcementsRef.push().setValue(announceData)
                                .addOnSuccessListener(aVoid -> android.util.Log.d("StatusAnnouncement", "Created successfully for " + newStatus))
                                .addOnFailureListener(e -> android.util.Log.e("StatusAnnouncement", "Failed to create for " + newStatus, e));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        android.util.Log.e("StatusAnnouncement", "Failed to fetch project title", error.toException());
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                android.util.Log.e("StatusAnnouncement", "Failed to fetch company name", error.toException());
            }
        });
    }

    /**
     * Updates the local applicant status in both main and filtered lists
     * @param userId The user ID of the applicant
     * @param projectId The project ID
     * @param newStatus The new status to set
     */
    private void updateLocalApplicantStatus(String userId, String projectId, String newStatus) {
        // Update in main list
        for (CompanyHomeActivity.ProjectWithApplicants project : projectsWithApplicantsList) {
            if (project.getProjectId().equals(projectId)) {
                for (Applicant applicant : project.getApplicants()) {
                    if (applicant.getUserId().equals(userId)) {
                        applicant.setStatus(newStatus);
                        break;
                    }
                }
                break;
            }
        }

        // Update in filtered list
        for (CompanyHomeActivity.ProjectWithApplicants project : filteredList) {
            if (project.getProjectId().equals(projectId)) {
                for (Applicant applicant : project.getApplicants()) {
                    if (applicant.getUserId().equals(userId)) {
                        applicant.setStatus(newStatus);
                        break;
                    }
                }
                break;
            }
        }

        // Reapply current filter to update the display
        filterApplicants(searchEditText != null ? searchEditText.getText().toString() : "");
    }

    // Callback interfaces
    public interface ProjectsWithApplicantsCallback {
        void onProjectsWithApplicantsLoaded(List<CompanyHomeActivity.ProjectWithApplicants> projects);
    }

    public interface CvUrlCallback {
        void onCvUrlFetched(String cvUrl);
    }

    // Callback interface for interview details
    public interface InterviewDetailsCallback {
        void onInterviewDetailsFetched(String interviewDate, String interviewTime);
    }
}