package com.example.internlink;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class CompanyAnnounce extends AppCompatActivity {

    private LinearLayout announcementContainer;
    private RecyclerView recyclerView;
    private TextInputEditText searchEditText;
    private ChipGroup chipGroup;
    private AnnouncementAdapter adapter;
    private MaterialToolbar toolbar;
    private List<Announcement> announcementList = new ArrayList<>();

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

        recyclerView = findViewById(R.id.announcement_recycler_view);
        searchEditText = findViewById(R.id.search_edit_text);
        chipGroup = findViewById(R.id.filter_chip_group);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new AnnouncementAdapter(this, announcementList);
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
                    sortAnnouncementsByDate(true);  // Sorting in ascending order (earliest first)
                } else if (chipText.equalsIgnoreCase("Latest")) {
                    sortAnnouncementsByDate(false);  // Sorting in descending order (latest first)
                } else {
                    adapter.filterChip(chipText);  // Handle other filters like Read/Unread
                }
            }
        });

    }

    private void addAnnouncement(String announcementId, String title, String body, String date, boolean isRead, long timestamp) {
        Announcement announcement = new Announcement(announcementId, title, body, date, isRead);
        announcement.setTimestamp(timestamp);
        announcementList.add(announcement);
        adapter.notifyItemInserted(announcementList.size() - 1);
    }

    void showAnnouncementPopup(String title, String body, String date) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View popupView = LayoutInflater.from(this).inflate(R.layout.announcement_item, null);
        popupView.setBackgroundColor(getResources().getColor(android.R.color.white));

        TextView titleView = popupView.findViewById(R.id.announcement_title);
        TextView bodyView = popupView.findViewById(R.id.announcement_body);
        TextView dateView = popupView.findViewById(R.id.announcement_date);
        ImageView closeIcon = popupView.findViewById(R.id.delete_icon);

        titleView.setText(title);
        bodyView.setText(body);
        dateView.setText("Posted: " + date);

        builder.setView(popupView);
        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();

        closeIcon.setOnClickListener(v -> dialog.dismiss());
    }

    private void loadAllAnnouncements() {
        announcementList.clear();
        adapter.notifyDataSetChanged();

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userReadsRef = FirebaseDatabase.getInstance().getReference("user_reads").child(userId);

        userReadsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot readsSnapshot) {
                loadAnnouncementsFromRef(FirebaseDatabase.getInstance().getReference("announcements"), readsSnapshot);
                loadAnnouncementsFromRef(FirebaseDatabase.getInstance().getReference("announcements_by_role").child("company"), readsSnapshot);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(CompanyAnnounce.this, "Failed to load data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadAnnouncementsFromRef(DatabaseReference ref, DataSnapshot readsSnapshot) {
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot snap : snapshot.getChildren()) {
                    String id = snap.getKey();
                    String title = snap.child("title").getValue(String.class);
                    String body = snap.child("message").getValue(String.class);
                    Long timestamp = snap.child("timestamp").getValue(Long.class);
                    String date = formatTimestamp(timestamp);
                    boolean isRead = readsSnapshot.hasChild(id);

                    addAnnouncement(id, title, body, date, isRead, timestamp != null ? timestamp : 0);
                }
                // Default sort: latest
                sortAnnouncementsByDate(false);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(CompanyAnnounce.this, "Failed to load data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String formatTimestamp(Long timestamp) {
        if (timestamp == null) return "Unknown date";
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    private void sortAnnouncementsByDate(boolean ascending) {
        if (announcementList == null || announcementList.isEmpty()) return;

        // Sort the announcements based on the timestamp
        announcementList.sort((a1, a2) -> {
            long t1 = a1.getTimestamp();
            long t2 = a2.getTimestamp();
            return ascending ? Long.compare(t1, t2) : Long.compare(t2, t1);
        });

        // Update the filtered list after sorting
        adapter.filterChip("All");  // To refresh the filtered list after sorting (you can also call `adapter.notifyDataSetChanged()` here directly if no filtering is needed)

        adapter.notifyDataSetChanged();  // Notify the adapter that the data has changed
    }

}
