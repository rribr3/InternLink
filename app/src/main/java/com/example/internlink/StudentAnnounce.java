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

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class StudentAnnounce extends AppCompatActivity implements AnnouncementAdapter.AnnouncementClickListener {

    private LinearLayout announcementContainer;
    private RecyclerView recyclerView;
    private TextInputEditText searchEditText;
    private ChipGroup chipGroup;
    private AnnouncementAdapter adapter;
    private MaterialToolbar toolbar;
    private List<Announcement> announcementList = new ArrayList<>();

    // Loading coordination variables
    private int loadingOperationsCount = 0;
    private final Object loadingLock = new Object();
    private SwipeRefreshLayout swipeRefreshLayout;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_student_announce);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        recyclerView = findViewById(R.id.announcement_recycler_view);
        searchEditText = findViewById(R.id.search_edit_text);
        chipGroup = findViewById(R.id.filter_chip_group);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);

        setupSwipeRefresh();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new AnnouncementAdapter(announcementList, new AnnouncementAdapter.AnnouncementClickListener() {
            @Override
            public void showAnnouncementPopup(String id, String title, String body, String date) {
                StudentAnnounce.this.showAnnouncementPopup(id, title, body, date);
            }
        });

        recyclerView.setAdapter(adapter);

        toolbar = findViewById(R.id.topAppBar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        loadAllAnnouncements();

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
                switch (chipText.toLowerCase()) {
                    case "earliest":
                        adapter.sortByDate(true);
                        break;
                    case "latest":
                        adapter.sortByDate(false);
                        break;
                    default:
                        adapter.filterChip(chipText);
                        break;
                }
            }
        });
    }

    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeResources(
                    R.color.blue_500,
                    R.color.green,
                    R.color.red,
                    R.color.yellow
            );
            swipeRefreshLayout.setOnRefreshListener(this::refreshAnnouncements);
        }
    }

    private void refreshAnnouncements() {
        synchronized (loadingLock) {
            loadingOperationsCount = 0;
        }

        announcementList.clear();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }

        loadAllAnnouncements();
    }

    private void checkAndFinalizeLoading() {
        synchronized (loadingLock) {
            loadingOperationsCount--;
            Log.d("StudentAnnounce", "Loading operation completed. Remaining: " + loadingOperationsCount);

            if (loadingOperationsCount <= 0) {
                runOnUiThread(() -> {
                    Log.d("StudentAnnounce", "All announcements loaded. Total count: " + announcementList.size());
                    sortAnnouncementsByDate(false);
                    adapter.filterChip("All");
                    if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
            }
        }
    }

    private void addAnnouncement(String announcementId, String title, String body, String date, boolean isRead, long timestamp) {
        Announcement announcement = new Announcement(announcementId, title, body, date, isRead);
        announcement.setTimestamp(timestamp);
        announcementList.add(announcement);
        adapter.notifyItemInserted(announcementList.size() - 1);
    }

    @Override
    public void showAnnouncementPopup(String announcementId, String title, String body, String date) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View popupView = LayoutInflater.from(this).inflate(R.layout.announcement_item, null);

        TextView titleView = popupView.findViewById(R.id.announcement_title);
        TextView bodyView = popupView.findViewById(R.id.announcement_body);
        TextView dateView = popupView.findViewById(R.id.announcement_date);
        ImageView closeIcon = popupView.findViewById(R.id.delete_icon);

        closeIcon.setVisibility(View.VISIBLE);

        Announcement announcement = null;
        for (Announcement a : announcementList) {
            if (a.getId().equals(announcementId)) {
                announcement = a;
                break;
            }
        }

        if (announcement != null && "warning".equals(announcement.getCategory())) {
            titleView.setTextColor(Color.RED);
        }

        titleView.setText(title);
        bodyView.setText(body);
        dateView.setText("Posted: " + date);

        builder.setView(popupView);
        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();

        closeIcon.setOnClickListener(v -> dialog.dismiss());
        markAnnouncementAsRead(announcementId);
    }

    private SpannableString createClickableSpannable(String body) {
        SpannableString spannable = new SpannableString(body);

        handleClickableLink(spannable, body, "[View Details]", () -> {
            Intent intent = new Intent(StudentAnnounce.this, StudentHomeActivity.class);
            startActivity(intent);
        });

        handleClickableLink(spannable, body, "[View Status]", () -> {
            Intent intent = new Intent(StudentAnnounce.this, StudentHomeActivity.class);
            startActivity(intent);
        });

        return spannable;
    }

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
                    Toast.makeText(StudentAnnounce.this, "Failed to mark as read", Toast.LENGTH_SHORT).show();
                });
    }

    // FIXED: Updated to properly load all student announcements
    private void loadAllAnnouncements() {
        announcementList.clear();
        adapter.notifyDataSetChanged();

        // Initialize loading counter for 5 async operations (was 3)
        synchronized (loadingLock) {
            loadingOperationsCount = 5;
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userReadsRef = FirebaseDatabase.getInstance().getReference("user_reads").child(userId);

        userReadsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot readsSnapshot) {
                // 1. Load general announcements
                loadAnnouncementsFromRef(FirebaseDatabase.getInstance().getReference("announcements"), readsSnapshot);

                // 2. Load all student-targeted announcements from announcements_by_role/student
                loadAllStudentAnnouncements(readsSnapshot, userId);

                // 3. Load warning announcements specifically for this user
                loadWarningAnnouncements(readsSnapshot, userId);

                // 4. Load specific student announcements (targeted to this student)
                loadSpecificStudentAnnouncements(readsSnapshot, userId);

                // 5. Load announcements from notifications (these might be student-specific)
                loadNotificationAnnouncements(readsSnapshot, userId);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(StudentAnnounce.this, "Failed to load data", Toast.LENGTH_SHORT).show();
                synchronized (loadingLock) {
                    loadingOperationsCount = 0;
                    runOnUiThread(() -> adapter.filterChip("All"));
                }
                if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }
        });
    }

    // NEW: Load all announcements from announcements_by_role/student
    private void loadAllStudentAnnouncements(DataSnapshot readsSnapshot, String userId) {
        DatabaseReference studentAnnouncementsRef = FirebaseDatabase.getInstance()
                .getReference("announcements_by_role")
                .child("student");

        studentAnnouncementsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Log.d("StudentAnnounce", "Loading student announcements. Count: " + snapshot.getChildrenCount());

                for (DataSnapshot snap : snapshot.getChildren()) {
                    try {
                        String id = snap.getKey();
                        String title = snap.child("title").getValue(String.class);
                        String message = snap.child("message").getValue(String.class);
                        String targetType = snap.child("targetType").getValue(String.class);
                        String targetUserId = snap.child("targetUserId").getValue(String.class);
                        String recipientId = snap.child("recipientId").getValue(String.class);

                        // Skip if this is targeted to a specific user and it's not this user
                        if ("specific_user".equals(targetType) && targetUserId != null && !targetUserId.equals(userId)) {
                            continue;
                        }

                        // Skip if this has a recipientId and it's not this user
                        if (recipientId != null && !recipientId.equals(userId)) {
                            continue;
                        }

                        long timestampLong = extractTimestamp(snap);
                        String date = formatTimestamp(timestampLong);
                        boolean isRead = readsSnapshot.hasChild(id);

                        Announcement announcement = new Announcement(id, title, message, date, isRead);
                        announcement.setTimestamp(timestampLong);

                        // Set category based on content
                        if (snap.hasChild("applicant_status")) {
                            announcement.setCategory("application_status");
                        } else if (snap.hasChild("category")) {
                            String category = snap.child("category").getValue(String.class);
                            announcement.setCategory(category);
                        }

                        announcementList.add(announcement);
                        Log.d("StudentAnnounce", "Added student announcement: " + title);

                    } catch (Exception e) {
                        Log.e("StudentAnnounce", "Error processing student announcement: " + snap.getKey(), e);
                    }
                }
                checkAndFinalizeLoading();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("StudentAnnounce", "Failed to load student announcements", error.toException());
                Toast.makeText(StudentAnnounce.this, "Failed to load student announcements", Toast.LENGTH_SHORT).show();
                checkAndFinalizeLoading();
            }
        });
    }

    // NEW: Load specific student announcements (those with recipientId)
    private void loadSpecificStudentAnnouncements(DataSnapshot readsSnapshot, String userId) {
        DatabaseReference studentSpecificRef = FirebaseDatabase.getInstance()
                .getReference("announcements_by_role")
                .child("student");

        studentSpecificRef.orderByChild("recipientId").equalTo(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        Log.d("StudentAnnounce", "Loading specific student announcements for user: " + userId + ". Count: " + snapshot.getChildrenCount());

                        for (DataSnapshot snap : snapshot.getChildren()) {
                            try {
                                String id = snap.getKey();

                                // Check if already added to avoid duplicates
                                boolean alreadyExists = false;
                                for (Announcement existing : announcementList) {
                                    if (existing.getId().equals(id)) {
                                        alreadyExists = true;
                                        break;
                                    }
                                }

                                if (alreadyExists) {
                                    continue;
                                }

                                String title = snap.child("title").getValue(String.class);
                                String message = snap.child("message").getValue(String.class);

                                long timestampLong = extractTimestamp(snap);
                                String date = formatTimestamp(timestampLong);
                                boolean isRead = readsSnapshot.hasChild(id);

                                Announcement announcement = new Announcement(id, title, message, date, isRead);
                                announcement.setTimestamp(timestampLong);

                                if (snap.hasChild("applicant_status")) {
                                    announcement.setCategory("application_status");
                                }

                                announcementList.add(announcement);
                                Log.d("StudentAnnounce", "Added specific student announcement: " + title);

                            } catch (Exception e) {
                                Log.e("StudentAnnounce", "Error processing specific student announcement: " + snap.getKey(), e);
                            }
                        }
                        checkAndFinalizeLoading();
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.e("StudentAnnounce", "Failed to load specific student announcements", error.toException());
                        checkAndFinalizeLoading();
                    }
                });
    }

    // NEW: Load announcements from notifications node
    private void loadNotificationAnnouncements(DataSnapshot readsSnapshot, String userId) {
        DatabaseReference notificationsRef = FirebaseDatabase.getInstance()
                .getReference("notifications")
                .child(userId);

        notificationsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Log.d("StudentAnnounce", "Loading notification announcements for user: " + userId + ". Count: " + snapshot.getChildrenCount());

                for (DataSnapshot snap : snapshot.getChildren()) {
                    try {
                        String type = snap.child("type").getValue(String.class);

                        // Only process announcement type notifications
                        if (!"announcement".equals(type)) {
                            continue;
                        }

                        String id = snap.getKey();

                        // Check if already added to avoid duplicates
                        boolean alreadyExists = false;
                        for (Announcement existing : announcementList) {
                            if (existing.getId().equals(id)) {
                                alreadyExists = true;
                                break;
                            }
                        }

                        if (alreadyExists) {
                            continue;
                        }

                        String title = snap.child("title").getValue(String.class);
                        String message = snap.child("message").getValue(String.class);

                        long timestampLong = 0;
                        Object timestampObj = snap.child("timestamp").getValue();
                        if (timestampObj instanceof Long) {
                            timestampLong = (Long) timestampObj;
                        }

                        String date = formatTimestamp(timestampLong);
                        boolean isRead = snap.child("isRead").getValue(Boolean.class) != null ?
                                snap.child("isRead").getValue(Boolean.class) : false;

                        Announcement announcement = new Announcement(id, title, message, date, isRead);
                        announcement.setTimestamp(timestampLong);
                        announcement.setCategory("notification");

                        announcementList.add(announcement);
                        Log.d("StudentAnnounce", "Added notification announcement: " + title);

                    } catch (Exception e) {
                        Log.e("StudentAnnounce", "Error processing notification announcement: " + snap.getKey(), e);
                    }
                }
                checkAndFinalizeLoading();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("StudentAnnounce", "Failed to load notification announcements", error.toException());
                checkAndFinalizeLoading();
            }
        });
    }

    // HELPER: Extract timestamp from various formats
    private long extractTimestamp(DataSnapshot snap) {
        long timestampLong = 0;
        Object timestampObj = snap.child("timestamp").getValue();

        if (timestampObj != null) {
            if (timestampObj instanceof Long) {
                timestampLong = (Long) timestampObj;
            } else if (timestampObj instanceof String) {
                try {
                    timestampLong = Long.parseLong((String) timestampObj);
                } catch (NumberFormatException e) {
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
                        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                        Date date = sdf.parse((String) timestampObj);
                        if (date != null) {
                            timestampLong = date.getTime();
                        }
                    } catch (ParseException pe) {
                        pe.printStackTrace();
                    }
                }
            }
        }

        return timestampLong;
    }

    private void loadWarningAnnouncements(DataSnapshot readsSnapshot, String userId) {
        DatabaseReference warningsRef = FirebaseDatabase.getInstance()
                .getReference("announcements_by_role")
                .child("student");

        warningsRef.orderByChild("targetUserId").equalTo(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot snap : snapshot.getChildren()) {
                    try {
                        String id = snap.getKey();
                        if (id == null) {
                            Log.w("StudentAnnounce", "Warning announcement has null id, skipping");
                            continue;
                        }

                        String category = snap.child("category").getValue(String.class);
                        if ("disciplinary_action".equals(category)) {
                            String title = snap.child("title").getValue(String.class);
                            String message = snap.child("message").getValue(String.class);

                            if (title == null || message == null) {
                                Log.w("StudentAnnounce", "Warning announcement " + id + " missing required fields, skipping");
                                continue;
                            }

                            long timestampLong = extractTimestamp(snap);
                            String severity = snap.child("severity").getValue(String.class);
                            String priority = snap.child("priority").getValue(String.class);
                            boolean isRead = readsSnapshot.hasChild(id);

                            Announcement announcement = new Announcement(id, title, message, formatTimestamp(timestampLong), isRead);
                            announcement.setTimestamp(timestampLong);
                            announcement.setCategory("warning");

                            if (severity != null) {
                                announcement.setSeverity(severity);
                            }
                            if (priority != null) {
                                announcement.setPriority(priority);
                            }

                            Log.d("StudentAnnounce", String.format("Adding warning announcement: id=%s, title=%s, timestamp=%d",
                                    id, title, timestampLong));

                            announcementList.add(announcement);
                        }
                    } catch (Exception e) {
                        Log.e("StudentAnnounce", "Error processing warning announcement: " + snap.getKey(), e);
                    }
                }
                checkAndFinalizeLoading();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                String errorMsg = "Failed to load warnings: " + error.getMessage();
                Log.e("StudentAnnounce", errorMsg, error.toException());
                Toast.makeText(StudentAnnounce.this, errorMsg, Toast.LENGTH_SHORT).show();
                checkAndFinalizeLoading();
            }
        });
    }

    private void loadAnnouncementsFromRef(DatabaseReference ref, DataSnapshot readsSnapshot) {
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot snap : snapshot.getChildren()) {
                    try {
                        String id = snap.getKey();
                        String title = snap.child("title").getValue(String.class);
                        String body = snap.child("message").getValue(String.class);

                        long timestampLong = extractTimestamp(snap);
                        String date = formatTimestamp(timestampLong);
                        boolean isRead = readsSnapshot.hasChild(id);

                        Announcement announcement = new Announcement(id, title, body, date, isRead);
                        announcement.setTimestamp(timestampLong);

                        if (snap.hasChild("category") && "disciplinary_action".equals(snap.child("category").getValue(String.class))) {
                            announcement.setCategory("warning");
                            announcement.setSeverity(snap.child("severity").getValue(String.class));
                            announcement.setPriority(snap.child("priority").getValue(String.class));
                        }

                        announcementList.add(announcement);
                    } catch (Exception e) {
                        Log.e("StudentAnnounce", "Error processing announcement: " + snap.getKey(), e);
                    }
                }

                checkAndFinalizeLoading();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(StudentAnnounce.this, "Failed to load announcements", Toast.LENGTH_SHORT).show();
                checkAndFinalizeLoading();
            }
        });
    }

    private String formatTimestamp(long timestamp) {
        if (timestamp <= 0) return "Unknown date";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.US);
            return sdf.format(new Date(timestamp));
        } catch (Exception e) {
            Log.e("StudentAnnounce", "Error formatting timestamp: " + timestamp, e);
            return "Unknown date";
        }
    }

    private void sortAnnouncementsByDate(boolean ascending) {
        if (announcementList == null || announcementList.isEmpty()) return;

        Collections.sort(announcementList, (a1, a2) -> {
            long t1 = a1 != null ? a1.getTimestamp() : 0;
            long t2 = a2 != null ? a2.getTimestamp() : 0;
            return ascending ? Long.compare(t1, t2) : Long.compare(t2, t1);
        });

        adapter.notifyDataSetChanged();
    }
}