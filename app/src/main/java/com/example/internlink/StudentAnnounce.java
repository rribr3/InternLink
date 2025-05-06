package com.example.internlink;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class StudentAnnounce extends AppCompatActivity {

    private LinearLayout announcementContainer;


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

        announcementContainer = findViewById(R.id.announcement_container);

        loadAllAnnouncements();
    }

    private void addAnnouncement(String announcementId, String title, String body, String date, boolean isRead) {
        // Inflate announcement layout from XML
        View announcementView = LayoutInflater.from(this).inflate(R.layout.announcement_item, announcementContainer, false);

        // Set text fields
        ((TextView) announcementView.findViewById(R.id.announcement_title)).setText(title);
        ((TextView) announcementView.findViewById(R.id.announcement_body)).setText(body);
        ((TextView) announcementView.findViewById(R.id.announcement_date)).setText("Posted: " + date);

        if (!isRead) {
            announcementView.setBackgroundColor(Color.parseColor("#E6E6FA"));
        } else {
            announcementView.setBackgroundColor(getResources().getColor(android.R.color.white)); // White for read
        }


        announcementView.setOnClickListener(v -> {
            // Show popup
            showAnnouncementPopup(title, body, date);

            // Mark as read
            announcementView.setBackgroundColor(getResources().getColor(android.R.color.white)); // Turn white
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            FirebaseDatabase.getInstance().getReference("user_reads")
                    .child(userId)
                    .child(announcementId)
                    .setValue(true);
        });

        // Add at the top (index 0)
        announcementContainer.addView(announcementView, 0);
    }

    private void showAnnouncementPopup(String title, String body, String date) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View popupView = LayoutInflater.from(this).inflate(R.layout.announcement_item, null);

        popupView.setBackgroundColor(getResources().getColor(android.R.color.white));

        TextView titleView = popupView.findViewById(R.id.announcement_title);
        TextView bodyView = popupView.findViewById(R.id.announcement_body);
        TextView dateView = popupView.findViewById(R.id.announcement_date);
        ImageView closeIcon = popupView.findViewById(R.id.delete_icon); // Use this as close button

        titleView.setText(title);
        bodyView.setText(body);
        dateView.setText("Posted: " + date);

        builder.setView(popupView);
        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent); // Optional: transparent background
        dialog.show();

        closeIcon.setOnClickListener(v -> dialog.dismiss());
    }

    private void loadAllAnnouncements() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userReadsRef = FirebaseDatabase.getInstance().getReference("user_reads").child(userId);

        userReadsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot readsSnapshot) {
                loadAnnouncementsFromRef(FirebaseDatabase.getInstance().getReference("announcements"), readsSnapshot);
                loadAnnouncementsFromRef(FirebaseDatabase.getInstance().getReference("announcements_by_role").child("student"), readsSnapshot);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(StudentAnnounce.this, "Failed to load data", Toast.LENGTH_SHORT).show();
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
                    addAnnouncement(id, title, body, date, isRead);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(StudentAnnounce.this, "Failed to load data", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private String formatTimestamp(Long timestamp) {
        if (timestamp == null) return "Unknown date";
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }


}
