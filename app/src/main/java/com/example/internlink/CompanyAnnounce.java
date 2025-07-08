package com.example.internlink;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class CompanyAnnounce extends AppCompatActivity implements AnnouncementAdapter.AnnouncementClickListener  {

    private LinearLayout announcementContainer;
    private RecyclerView recyclerView;
    private TextInputEditText searchEditText;
    private ChipGroup chipGroup;
    private AnnouncementAdapter adapter;
    private MaterialToolbar toolbar;
    private List<Announcement> announcementList = new ArrayList<>();
    private SwipeRefreshLayout swipeRefreshLayout;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_company_announce);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        setupSwipeRefresh();

        recyclerView = findViewById(R.id.announcement_recycler_view);
        searchEditText = findViewById(R.id.search_edit_text);
        chipGroup = findViewById(R.id.filter_chip_group);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new AnnouncementAdapter(announcementList, this);

        recyclerView.setAdapter(adapter);

        toolbar = findViewById(R.id.topAppBar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Load announcements from Firebase
        loadAllAnnouncements();

        // Search listener
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filterBy(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        chipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            Chip chip = findViewById(checkedId);
            if (chip != null) {
                String chipText = chip.getText().toString();
                if (chipText.equalsIgnoreCase("Earliest")) {
                    sortAnnouncementsByDate(true);
                } else if (chipText.equalsIgnoreCase("Latest")) {
                    sortAnnouncementsByDate(false);
                } else {
                    adapter.filterChip(chipText);
                }
            }
        });
    }

    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            // Set custom colors for the refresh indicator
            swipeRefreshLayout.setColorSchemeResources(
                    R.color.blue_500,
                    R.color.green,
                    R.color.red,
                    R.color.yellow
            );

            // Set the listener for refresh action
            swipeRefreshLayout.setOnRefreshListener(this::refreshAnnouncements);
        }
    }

    private void refreshAnnouncements() {
        // Clear existing data
        announcementList.clear();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }

        // Load fresh data
        loadAllAnnouncements();
    }

    private void addAnnouncement(String announcementId, String title, String message, String date, boolean isRead, long timestamp) {
        Announcement announcement = new Announcement(announcementId, title, message, date, isRead);
        announcement.setTimestamp(timestamp);
        announcementList.add(announcement);
        adapter.notifyItemInserted(announcementList.size() - 1);
    }

    // UPDATED: Enhanced popup method to handle all clickable links
    @Override
    public void showAnnouncementPopup(String announcementId, String title, String body, String date) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View popupView = LayoutInflater.from(this).inflate(R.layout.announcement_item, null);

        // Find views
        TextView titleView = popupView.findViewById(R.id.announcement_title);
        TextView bodyView = popupView.findViewById(R.id.announcement_body);
        TextView dateView = popupView.findViewById(R.id.announcement_date);
        ImageView closeIcon = popupView.findViewById(R.id.delete_icon);

        closeIcon.setVisibility(View.VISIBLE);

        // Find the announcement object
        Announcement announcement = null;
        for (Announcement a : announcementList) {
            if (a.getId().equals(announcementId)) {
                announcement = a;
                break;
            }
        }

        // Apply warning-specific styling if needed
        if (announcement != null && "warning".equals(announcement.getCategory())) {
            titleView.setTextColor(Color.RED);
            popupView.setBackgroundColor(Color.parseColor("#FFEBEE")); // Light red background
        }

        titleView.setText(title);
        bodyView.setText(body);
        dateView.setText("Posted: " + date);

        builder.setView(popupView);
        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();

        closeIcon.setOnClickListener(v -> dialog.dismiss());

        // Mark the announcement as read
        markAnnouncementAsRead(announcementId);
    }

    // NEW: Method to create clickable spans for different link types
    private SpannableString createClickableSpannable(String body) {
        SpannableString spannable = new SpannableString(body);

        // Handle [View Applicants] links
        handleClickableLink(spannable, body, "[View Applicants]", () -> {
            Intent intent = new Intent(CompanyAnnounce.this, MyApplicants.class);
            startActivity(intent);
        });

        return spannable;
    }

    // NEW: Helper method to handle clickable links
    private void handleClickableLink(SpannableString spannable, String body, String linkText, Runnable action) {
        int start = body.indexOf(linkText);
        if (start != -1) {
            int end = start + linkText.length();

            ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    action.run();
                }

                @Override
                public void updateDrawState(@NonNull TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setUnderlineText(true);
                    ds.setColor(Color.BLUE);
                }
            };

            spannable.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private void markAnnouncementAsRead(String announcementId) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userReadsRef = FirebaseDatabase.getInstance()
                .getReference("user_reads")
                .child(userId)
                .child(announcementId);

        userReadsRef.setValue(System.currentTimeMillis())
                .addOnSuccessListener(aVoid -> {
                    for (Announcement announcement : announcementList) {
                        if (announcement.getId().equals(announcementId)) {
                            announcement.setRead(true);
                            adapter.notifyDataSetChanged();
                            break;
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(CompanyAnnounce.this, "Failed to mark as read", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadAllAnnouncements() {
        announcementList.clear();
        adapter.notifyDataSetChanged();

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userReadsRef = FirebaseDatabase.getInstance().getReference("user_reads").child(userId);

        userReadsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot readsSnapshot) {
                // Load general announcements
                loadAnnouncementsFromRef(FirebaseDatabase.getInstance().getReference("announcements"), readsSnapshot);
                // Load company-specific announcements
                loadAnnouncementsFromRef(FirebaseDatabase.getInstance().getReference("announcements_by_role").child("company"), readsSnapshot);
                // Load warning announcements specifically for this user
                loadWarningAnnouncements(readsSnapshot, userId);
                if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(CompanyAnnounce.this, "Failed to load data", Toast.LENGTH_SHORT).show();
                if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }
        });
    }

    private void loadWarningAnnouncements(DataSnapshot readsSnapshot, String userId) {
        DatabaseReference warningsRef = FirebaseDatabase.getInstance()
                .getReference("announcements_by_role")
                .child("company");

        warningsRef.orderByChild("targetUserId").equalTo(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot snap : snapshot.getChildren()) {
                    String id = snap.getKey();
                    String category = snap.child("category").getValue(String.class);

                    // Only process if it's a warning announcement
                    if ("disciplinary_action".equals(category)) {
                        String title = snap.child("title").getValue(String.class);
                        String message = snap.child("message").getValue(String.class);

                        // Handle timestamp conversion safely
                        long timestampLong = 0;
                        Object timestampObj = snap.child("timestamp").getValue();
                        if (timestampObj != null) {
                            if (timestampObj instanceof Long) {
                                timestampLong = (Long) timestampObj;
                            } else if (timestampObj instanceof String) {
                                try {
                                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
                                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                                    Date date = sdf.parse((String) timestampObj);
                                    if (date != null) {
                                        timestampLong = date.getTime();
                                    }
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                        String severity = snap.child("severity").getValue(String.class);
                        String priority = snap.child("priority").getValue(String.class);
                        boolean isRead = readsSnapshot.hasChild(id);

                        // Create announcement
                        Announcement announcement = new Announcement(id, title, message, formatTimestamp(timestampLong), isRead);
                        announcement.setTimestamp(timestampLong);
                        announcement.setCategory("warning");
                        announcement.setSeverity(severity);
                        announcement.setPriority(priority);

                        announcementList.add(announcement);
                        adapter.notifyItemInserted(announcementList.size() - 1);
                    }
                }
                // Sort announcements after adding warnings
                sortAnnouncementsByDate(false);
                if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(CompanyAnnounce.this, "Failed to load warnings", Toast.LENGTH_SHORT).show();
                if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }
        });
    }

    private void loadAnnouncementsFromRef(DatabaseReference ref, DataSnapshot readsSnapshot) {
        // Get current company's ID
        String currentCompanyId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Log.d("CompanyAnnounce", "Loading announcements for company ID: " + currentCompanyId);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("RestrictedApi")
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Log.d("CompanyAnnounce", "Found " + snapshot.getChildrenCount() + " announcements in " + ref.getPath().toString());

                for (DataSnapshot snap : snapshot.getChildren()) {
                    try {
                        String id = snap.getKey();
                        Log.d("CompanyAnnounce", "Processing announcement: " + id);

                        // For company-specific announcements, check if this announcement is for the current company
                        if (ref.getPath().toString().contains("announcements_by_role/company")) {
                            boolean shouldShowAnnouncement = false;

                            // Check if announcement has a specific company target via companyId
                            if (snap.hasChild("companyId")) {
                                String targetCompanyId = snap.child("companyId").getValue(String.class);
                                Log.d("CompanyAnnounce", "Found companyId: " + targetCompanyId);
                                if (targetCompanyId != null && targetCompanyId.equals(currentCompanyId)) {
                                    shouldShowAnnouncement = true;
                                    Log.d("CompanyAnnounce", "✓ Showing due to companyId match");
                                }
                            }

                            // Check if announcement targets this specific user via targetUserId
                            if (snap.hasChild("targetUserId")) {
                                String targetUserId = snap.child("targetUserId").getValue(String.class);
                                Log.d("CompanyAnnounce", "Found targetUserId: " + targetUserId);
                                if (targetUserId != null && targetUserId.equals(currentCompanyId)) {
                                    shouldShowAnnouncement = true;
                                    Log.d("CompanyAnnounce", "✓ Showing due to targetUserId match");
                                }
                            }

                            // Check for targetType to handle announcements sent to all companies
                            if (snap.hasChild("targetType")) {
                                String targetType = snap.child("targetType").getValue(String.class);
                                Log.d("CompanyAnnounce", "Found targetType: " + targetType);
                                if ("company".equals(targetType) || "all_users".equals(targetType)) {
                                    shouldShowAnnouncement = true;
                                    Log.d("CompanyAnnounce", "✓ Showing due to targetType: " + targetType);
                                }
                            }

                            // Check if it's created by the current company (their own announcements)
                            if (snap.hasChild("createdBy")) {
                                String createdBy = snap.child("createdBy").getValue(String.class);
                                Log.d("CompanyAnnounce", "Found createdBy: " + createdBy);
                                if (currentCompanyId.equals(createdBy)) {
                                    shouldShowAnnouncement = true;
                                    Log.d("CompanyAnnounce", "✓ Showing due to createdBy match");
                                }
                            }

                            // Check for application notifications
                            if (snap.hasChild("type")) {
                                String type = snap.child("type").getValue(String.class);
                                Log.d("CompanyAnnounce", "Found type: " + type);
                                if ("application".equals(type)) {
                                    shouldShowAnnouncement = true;
                                    Log.d("CompanyAnnounce", "✓ Showing due to application type");
                                }
                            }

                            // If no specific targeting info, assume it's a general company announcement
                            if (!snap.hasChild("companyId") && !snap.hasChild("targetUserId") &&
                                    !snap.hasChild("targetType") && !snap.hasChild("createdBy") &&
                                    !snap.hasChild("type")) {
                                shouldShowAnnouncement = true;
                                Log.d("CompanyAnnounce", "✓ Showing - no specific targeting");
                            }

                            Log.d("CompanyAnnounce", "Final decision for " + id + ": " + shouldShowAnnouncement);

                            // Skip if this announcement shouldn't be shown to this company
                            if (!shouldShowAnnouncement) {
                                Log.d("CompanyAnnounce", "✗ Skipping announcement " + id);
                                continue;
                            }
                        }

                        String title = snap.child("title").getValue(String.class);
                        String body = snap.child("message").getValue(String.class);

                        // Improved timestamp handling
                        long timestampLong = getTimestampFromSnapshot(snap.child("timestamp"));

                        String date = formatTimestamp(timestampLong);
                        boolean isRead = readsSnapshot.hasChild(id);

                        Announcement announcement = new Announcement(id, title, body, date, isRead);
                        announcement.setTimestamp(timestampLong);

                        // Check if this is a warning announcement
                        if (snap.hasChild("category") && "disciplinary_action".equals(snap.child("category").getValue(String.class))) {
                            announcement.setCategory("warning");
                            announcement.setSeverity(snap.child("severity").getValue(String.class));
                            announcement.setPriority(snap.child("priority").getValue(String.class));
                        }

                        announcementList.add(announcement);
                        Log.d("CompanyAnnounce", "✓ Added announcement: " + title);
                    } catch (Exception e) {
                        Log.e("CompanyAnnounce", "Error processing announcement: " + snap.getKey(), e);
                    }
                }

                Log.d("CompanyAnnounce", "Total announcements added: " + announcementList.size());
                sortAnnouncementsByDate(false);
                adapter.notifyDataSetChanged();
                if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("CompanyAnnounce", "Failed to load announcements", error.toException());
                Toast.makeText(CompanyAnnounce.this, "Failed to load announcements", Toast.LENGTH_SHORT).show();
                if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }
        });
    }
    private long getTimestampFromSnapshot(DataSnapshot timestampSnap) {
        if (!timestampSnap.exists()) return 0;

        try {
            Object value = timestampSnap.getValue();
            if (value == null) return 0;

            if (value instanceof Long) {
                return (Long) value;
            } else if (value instanceof Double) {
                return ((Double) value).longValue();
            } else if (value instanceof String) {
                String timestampStr = (String) value;
                if (timestampStr.contains("T")) {
                    // ISO 8601 format
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                    Date date = sdf.parse(timestampStr);
                    return date != null ? date.getTime() : 0;
                } else {
                    // Try parsing as numeric string
                    return Long.parseLong(timestampStr);
                }
            }
        } catch (Exception e) {
            Log.e("CompanyAnnounce", "Error converting timestamp: " + timestampSnap.getValue(), e);
        }
        return 0;
    }

    private String formatTimestamp(Long timestamp) {
        if (timestamp == null) return "Unknown date";
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    private void sortAnnouncementsByDate(boolean ascending) {
        if (announcementList == null || announcementList.isEmpty()) return;

        announcementList.sort((a1, a2) -> {
            long t1 = a1.getTimestamp();
            long t2 = a2.getTimestamp();
            return ascending ? Long.compare(t1, t2) : Long.compare(t2, t1);
        });

        adapter.filterChip("All");
        adapter.notifyDataSetChanged();
    }
}