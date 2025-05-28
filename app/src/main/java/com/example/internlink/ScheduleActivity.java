package com.example.internlink;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ScheduleActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private View emptyState;
    private TextInputEditText searchEditText;
    private ChipGroup filterChips;
    private ShortlistedApplicantsAdapter adapter;
    private List<ShortlistedApplicant> applicantsList = new ArrayList<>();
    private List<ShortlistedApplicant> filteredList = new ArrayList<>();
    private String currentFilter = "All";
    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);

        initializeViews();
        setupToolbar();
        setupSearchAndFilters();
        setupBottomNavigation();
        loadShortlistedApplicants();
        setupFab();
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.applicants_recycler_view);
        progressBar = findViewById(R.id.progress_bar);
        emptyState = findViewById(R.id.empty_state);
        searchEditText = findViewById(R.id.search_edit_text);
        filterChips = findViewById(R.id.filter_chips);
        bottomNavigation = findViewById(R.id.bottom_navigation);

        // Setup RecyclerView with Grid Layout
        int spanCount = getResources().getConfiguration().screenWidthDp > 600 ? 2 : 1;
        recyclerView.setLayoutManager(new GridLayoutManager(this, spanCount));
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.navigation_Schedule);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.navigation_home) {
                Intent intent = new Intent(ScheduleActivity.this, CompanyHomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
                return true;
            } else if (itemId == R.id.navigation_Schedule) {
                // Already on Schedule page
                return true;
            } else if (itemId == R.id.navigation_profile) {
                Intent intent = new Intent(ScheduleActivity.this, CompanyProfileActivity.class);
                startActivity(intent);
                return true;
            }

            return false;
        });
    }

    private void setupSearchAndFilters() {
        // Search functionality
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

        // Filter chips
        filterChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                currentFilter = "All";
            } else {
                int checkedId = checkedIds.get(0);
                if (checkedId == R.id.chip_all) {
                    currentFilter = "All";
                } else if (checkedId == R.id.chip_scheduled) {
                    currentFilter = "Scheduled";
                } else if (checkedId == R.id.chip_pending) {
                    currentFilter = "Pending";
                } else if (checkedId == R.id.chip_completed) {
                    currentFilter = "Completed";
                }
            }
            filterApplicants(searchEditText.getText().toString());
        });
    }

    private void setupFab() {
        FloatingActionButton fab = findViewById(R.id.fab_add_interview);
        fab.setOnClickListener(v -> showAddInterviewDialog());
    }

    private void filterApplicants(String query) {
        filteredList.clear();
        String lowerQuery = query.toLowerCase();

        for (ShortlistedApplicant applicant : applicantsList) {
            boolean matchesQuery = applicant.getName().toLowerCase().contains(lowerQuery) ||
                    applicant.getProjectTitle().toLowerCase().contains(lowerQuery);
            boolean matchesFilter = currentFilter.equals("All") ||
                    currentFilter.equals(applicant.getInterviewStatus());

            if (matchesQuery && matchesFilter) {
                filteredList.add(applicant);
            }
        }

        if (adapter != null) {
            adapter.updateData(filteredList);
        }

        showEmptyState(filteredList.isEmpty());
    }

    private void showEmptyState(boolean show) {
        emptyState.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    public void loadShortlistedApplicants() {
        showLoading(true);
        String companyId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference applicationsRef = FirebaseDatabase.getInstance().getReference("applications");

        applicationsRef.orderByChild("companyId").equalTo(companyId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        applicantsList.clear();

                        List<DataSnapshot> shortlistedApps = new ArrayList<>();
                        for (DataSnapshot appSnapshot : snapshot.getChildren()) {
                            String status = appSnapshot.child("status").getValue(String.class);
                            if ("Shortlisted".equals(status)) {
                                shortlistedApps.add(appSnapshot);
                            }
                        }

                        if (shortlistedApps.isEmpty()) {
                            showLoading(false);
                            showEmptyState(true);
                            return;
                        }

                        final int[] processedCount = {0};
                        final int totalApplicants = shortlistedApps.size();

                        for (DataSnapshot appSnapshot : shortlistedApps) {
                            processShortlistedApplicant(appSnapshot, () -> {
                                processedCount[0]++;
                                if (processedCount[0] == totalApplicants) {
                                    // Sort by interview date
                                    Collections.sort(applicantsList, (a, b) -> {
                                        if (a.getInterviewDate() == null && b.getInterviewDate() == null) return 0;
                                        if (a.getInterviewDate() == null) return 1;
                                        if (b.getInterviewDate() == null) return -1;
                                        return a.getInterviewDate().compareTo(b.getInterviewDate());
                                    });

                                    filteredList = new ArrayList<>(applicantsList);
                                    if (adapter == null) {
                                        adapter = new ShortlistedApplicantsAdapter(filteredList, ScheduleActivity.this);
                                        recyclerView.setAdapter(adapter);
                                    } else {
                                        adapter.updateData(filteredList);
                                    }

                                    showLoading(false);
                                    showEmptyState(applicantsList.isEmpty());
                                }
                            });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showLoading(false);
                        Toast.makeText(ScheduleActivity.this, "Failed to load applicants", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void processShortlistedApplicant(DataSnapshot appSnapshot, Runnable callback) {
        String userId = appSnapshot.child("userId").getValue(String.class);
        String projectId = appSnapshot.child("projectId").getValue(String.class);
        String interviewDate = appSnapshot.child("interviewDate").getValue(String.class);
        String interviewTime = appSnapshot.child("interviewTime").getValue(String.class);

        // Extract interview type and related fields
        String interviewType = appSnapshot.child("interviewType").getValue(String.class);
        String interviewMethod = appSnapshot.child("interviewMethod").getValue(String.class);
        String interviewLocation = appSnapshot.child("interviewLocation").getValue(String.class);
        String zoomLink = appSnapshot.child("zoomLink").getValue(String.class);

        String interviewNotes = appSnapshot.child("interviewNotes").getValue(String.class);

        if (userId != null && projectId != null) {
            DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
            DatabaseReference projectsRef = FirebaseDatabase.getInstance().getReference("projects");

            usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                    String userName = userSnapshot.child("name").getValue(String.class);
                    String userDegree = userSnapshot.child("degree").getValue(String.class);
                    String userUniversity = userSnapshot.child("university").getValue(String.class);

                    projectsRef.child(projectId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot projectSnapshot) {
                            String projectTitle = projectSnapshot.child("title").getValue(String.class);

                            if (userName != null && projectTitle != null) {
                                ShortlistedApplicant applicant = new ShortlistedApplicant();
                                applicant.setUserId(userId);
                                applicant.setProjectId(projectId);
                                applicant.setApplicationId(appSnapshot.getKey());
                                applicant.setName(userName);
                                applicant.setDegree(userDegree);
                                applicant.setUniversity(userUniversity);
                                applicant.setProjectTitle(projectTitle);
                                applicant.setInterviewDate(interviewDate);
                                applicant.setInterviewTime(interviewTime);

                                // Set interview type (this will be displayed in tv_mode)
                                applicant.setInterviewMode(interviewType != null ? interviewType : "Not Set");

                                // Set interview method for online interviews
                                applicant.setInterviewMethod(interviewMethod);

                                // Set location/zoom link based on interview type
                                if ("In-person".equals(interviewType)) {
                                    applicant.setInterviewLocation(interviewLocation);
                                } else if ("Online".equals(interviewType)) {
                                    if ("Zoom".equals(interviewMethod)) {
                                        applicant.setInterviewLocation(zoomLink);
                                    } else {
                                        applicant.setInterviewLocation("Chat");
                                    }
                                }

                                applicant.setInterviewNotes(interviewNotes);

                                // Determine interview status
                                String status = "Pending";
                                if (interviewDate != null && interviewTime != null) {
                                    status = "Scheduled";
                                }
                                applicant.setInterviewStatus(status);

                                applicantsList.add(applicant);
                            }
                            callback.run();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            callback.run();
                        }
                    });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    callback.run();
                }
            });
        } else {
            callback.run();
        }
    }


    private void showAddInterviewDialog() {
        // Show dialog to manually add interview (optional feature)
        Toast.makeText(this, "Add Interview Feature Coming Soon!", Toast.LENGTH_SHORT).show();
    }

    // Public methods for adapter callbacks
    public void editInterview(ShortlistedApplicant applicant) {
        showEditInterviewDialog(applicant);
    }

    public void startChat(ShortlistedApplicant applicant) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("CHAT_WITH_ID", applicant.getUserId());
        intent.putExtra("CHAT_WITH_NAME", applicant.getName());
        startActivity(intent);
    }

    public void joinInterview(ShortlistedApplicant applicant) {
        if ("Online".equals(applicant.getInterviewMode())) {
            if ("Zoom".equals(applicant.getInterviewMethod())) {
                String zoomLink = applicant.getInterviewLocation();
                if (zoomLink != null && zoomLink.startsWith("http")) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(zoomLink));
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Zoom link not available", Toast.LENGTH_SHORT).show();
                }
            } else if ("Chat".equals(applicant.getInterviewMethod())) {
                // Redirect to chat
                startChat(applicant);
            } else {
                Toast.makeText(this, "Interview method not specified", Toast.LENGTH_SHORT).show();
            }
        } else if ("In-person".equals(applicant.getInterviewMode())) {
            // Show location details
            new AlertDialog.Builder(this)
                    .setTitle("Interview Location")
                    .setMessage("📍 " + (applicant.getInterviewLocation() != null ?
                            applicant.getInterviewLocation() : "Location not specified"))
                    .setPositiveButton("OK", null)
                    .show();
        } else {
            Toast.makeText(this, "Interview type not set", Toast.LENGTH_SHORT).show();
        }
    }


    public void removeApplicant(ShortlistedApplicant applicant) {
        new AlertDialog.Builder(this)
                .setTitle("Remove Applicant")
                .setMessage("Are you sure you want to remove " + applicant.getName() + " from shortlist?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    updateApplicantStatus(applicant, "Under Review");
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public void viewCV(ShortlistedApplicant applicant) {
        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users").child(applicant.getUserId());

        userRef.child("cvUrl").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String cvUrl = snapshot.getValue(String.class);
                if (cvUrl != null && !cvUrl.isEmpty()) {
                    if (cvUrl.contains("www.dropbox.com")) {
                        cvUrl = cvUrl.replace("www.dropbox.com", "dl.dropboxusercontent.com").replace("?dl=0", "");
                    }
                    Intent intent = new Intent(ScheduleActivity.this, PdfViewerActivity.class);
                    intent.putExtra("pdf_url", cvUrl);
                    startActivity(intent);
                }
                else {
                    Toast.makeText(ScheduleActivity.this, "CV not available", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ScheduleActivity.this, "Failed to load CV", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showEditInterviewDialog(ShortlistedApplicant applicant) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_interview, null);

        // Initialize views
        TextView titleText = dialogView.findViewById(R.id.dialog_title);
        TextInputEditText dateInput = dialogView.findViewById(R.id.et_date);
        TextInputEditText timeInput = dialogView.findViewById(R.id.et_time);
        TextInputEditText modeInput = dialogView.findViewById(R.id.et_mode);
        TextInputEditText locationInput = dialogView.findViewById(R.id.et_location);
        TextInputEditText notesInput = dialogView.findViewById(R.id.et_notes);
        MaterialButton saveButton = dialogView.findViewById(R.id.btn_save);
        MaterialButton cancelButton = dialogView.findViewById(R.id.btn_cancel);

        titleText.setText("Edit Interview - " + applicant.getName());

        // Pre-fill existing data
        if (applicant.getInterviewDate() != null) dateInput.setText(applicant.getInterviewDate());
        if (applicant.getInterviewTime() != null) timeInput.setText(applicant.getInterviewTime());
        if (applicant.getInterviewMode() != null) modeInput.setText(applicant.getInterviewMode());
        if (applicant.getInterviewLocation() != null) locationInput.setText(applicant.getInterviewLocation());
        if (applicant.getInterviewNotes() != null) notesInput.setText(applicant.getInterviewNotes());

        // Disable keyboard for date and time inputs
        dateInput.setKeyListener(null);
        timeInput.setKeyListener(null);

        // Date picker
        dateInput.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                calendar.set(year, month, dayOfMonth);
                SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
                dateInput.setText(sdf.format(calendar.getTime()));
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Time picker
        timeInput.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);
                SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                timeInput.setText(sdf.format(calendar.getTime()));
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show();
        });

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        saveButton.setOnClickListener(v -> {
            String date = dateInput.getText().toString().trim();
            String time = timeInput.getText().toString().trim();
            String mode = modeInput.getText().toString().trim();
            String location = locationInput.getText().toString().trim();
            String notes = notesInput.getText().toString().trim();

            updateInterviewDetails(applicant, date, time, mode, location, notes);
            dialog.dismiss();
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void updateInterviewDetails(ShortlistedApplicant applicant, String date, String time,
                                        String mode, String location, String notes) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy hh:mm a", Locale.getDefault());
            Date selectedDateTime = sdf.parse(date + " " + time);
            long selectedTimeMillis = selectedDateTime.getTime();

            checkInterviewConflicts(FirebaseAuth.getInstance().getCurrentUser().getUid(), applicant.getUserId(), selectedTimeMillis, canProceed -> {
                if (!canProceed) {
                    Toast.makeText(ScheduleActivity.this, "❌ Conflict with another interview time", Toast.LENGTH_LONG).show();
                    return;
                }

                DatabaseReference applicationRef = FirebaseDatabase.getInstance()
                        .getReference("applications")
                        .child(applicant.getApplicationId());

                Map<String, Object> updates = new HashMap<>();
                updates.put("interviewDate", date.isEmpty() ? null : date);
                updates.put("interviewTime", time.isEmpty() ? null : time);
                updates.put("interviewMode", mode.isEmpty() ? "Pending" : mode);
                updates.put("interviewLocation", location.isEmpty() ? null : location);
                updates.put("interviewNotes", notes.isEmpty() ? null : notes);
                updates.put("lastUpdated", System.currentTimeMillis());

                applicationRef.updateChildren(updates)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(ScheduleActivity.this, "✅ Interview updated!", Toast.LENGTH_SHORT).show();

                            applicant.setInterviewDate(date.isEmpty() ? null : date);
                            applicant.setInterviewTime(time.isEmpty() ? null : time);
                            applicant.setInterviewMode(mode.isEmpty() ? "Pending" : mode);
                            applicant.setInterviewLocation(location.isEmpty() ? null : location);
                            applicant.setInterviewNotes(notes.isEmpty() ? null : notes);

                            String status = (date.isEmpty() || time.isEmpty()) ? "Pending" : "Scheduled";
                            applicant.setInterviewStatus(status);

                            if (adapter != null) adapter.notifyDataSetChanged();
                            createInterviewUpdateAnnouncement(applicant);
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(ScheduleActivity.this, "❌ Failed to update interview", Toast.LENGTH_SHORT).show();
                        });
            });

        } catch (Exception e) {
            Toast.makeText(ScheduleActivity.this, "Invalid date or time", Toast.LENGTH_SHORT).show();
        }
    }
    private void checkInterviewConflicts(String companyId, String studentId, long selectedTimeMillis, ConflictCallback callback) {
        DatabaseReference applicationsRef = FirebaseDatabase.getInstance().getReference("applications");

        applicationsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long buffer = 30 * 60 * 1000; // 30 minutes

                for (DataSnapshot app : snapshot.getChildren()) {
                    String appCompanyId = app.child("companyId").getValue(String.class);
                    String appUserId = app.child("userId").getValue(String.class);
                    String interviewDate = app.child("interviewDate").getValue(String.class);
                    String interviewTime = app.child("interviewTime").getValue(String.class);

                    if (interviewDate == null || interviewTime == null) continue;

                    try {
                        Date existing = new SimpleDateFormat("MMMM d, yyyy hh:mm a", Locale.getDefault())
                                .parse(interviewDate + " " + interviewTime);
                        long existingTime = existing.getTime();

                        boolean companyConflict = appCompanyId != null && appCompanyId.equals(companyId)
                                && Math.abs(existingTime - selectedTimeMillis) < buffer;
                        boolean studentConflict = appUserId != null && appUserId.equals(studentId)
                                && existingTime == selectedTimeMillis;

                        if (companyConflict || studentConflict) {
                            callback.onCheckComplete(false);
                            return;
                        }
                    } catch (Exception ignored) {}
                }

                callback.onCheckComplete(true); // No conflicts
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onCheckComplete(false);
            }
        });
    }

    interface ConflictCallback {
        void onCheckComplete(boolean canProceed);
    }



    void updateApplicantStatus(ShortlistedApplicant applicant, String newStatus) {
        DatabaseReference applicationRef = FirebaseDatabase.getInstance()
                .getReference("applications")
                .child(applicant.getApplicationId());

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);
        updates.put("lastUpdated", System.currentTimeMillis());

        applicationRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, applicant.getName() + " removed from shortlist", Toast.LENGTH_SHORT).show();

                    // Remove from local list
                    applicantsList.remove(applicant);
                    filterApplicants(searchEditText.getText().toString());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update status", Toast.LENGTH_SHORT).show();
                });
    }

    private void createInterviewUpdateAnnouncement(ShortlistedApplicant applicant) {
        DatabaseReference announcementsRef = FirebaseDatabase.getInstance()
                .getReference("announcements_by_role").child("student");
        DatabaseReference companyRef = FirebaseDatabase.getInstance()
                .getReference("users").child(FirebaseAuth.getInstance().getCurrentUser().getUid());

        companyRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot companySnapshot) {
                String companyName = companySnapshot.child("name").getValue(String.class);

                String message = "📅 Your interview details for \"" + applicant.getProjectTitle() +
                        "\" with \"" + companyName + "\" have been updated.\n\n" +
                        "📆 Date: " + (applicant.getInterviewDate() != null ? applicant.getInterviewDate() : "TBD") + "\n" +
                        "⏰ Time: " + (applicant.getInterviewTime() != null ? applicant.getInterviewTime() : "TBD") + "\n" +
                        "🌐 Mode: " + applicant.getInterviewMode() + "\n\n[View Details]";

                Map<String, Object> announceData = new HashMap<>();
                announceData.put("title", "Interview Details Updated");
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
}