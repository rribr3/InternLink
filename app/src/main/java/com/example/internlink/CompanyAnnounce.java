package com.example.internlink;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
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

public class CompanyAnnounce extends AppCompatActivity {

    private LinearLayout announcementContainer;

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

        announcementContainer = findViewById(R.id.announcement_container);

        loadAllAnnouncements();
    }

    private void addAnnouncement(String title, String body, String date) {
        // Inflate announcement layout from XML
        View announcementView = LayoutInflater.from(this).inflate(R.layout.announcement_item, announcementContainer, false);

        // Set text fields
        ((TextView) announcementView.findViewById(R.id.announcement_title)).setText(title);
        ((TextView) announcementView.findViewById(R.id.announcement_body)).setText(body);
        ((TextView) announcementView.findViewById(R.id.announcement_date)).setText("Posted: " + date);


        // Add at the top (index 0)
        announcementContainer.addView(announcementView, 0);
    }
    private void loadAllAnnouncements() {
        DatabaseReference globalRef = FirebaseDatabase.getInstance().getReference("announcements");
        DatabaseReference companyRef = FirebaseDatabase.getInstance().getReference("announcements_by_role").child("company");

        // Load global announcements
        globalRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot snap : snapshot.getChildren()) {
                    String title = snap.child("title").getValue(String.class);
                    String body = snap.child("message").getValue(String.class);
                    Long timestamp = snap.child("timestamp").getValue(Long.class);
                    String date = formatTimestamp(timestamp);
                    addAnnouncement(title, body, date);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Handle error if needed
            }
        });

        // Load company-specific announcements
        companyRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot snap : snapshot.getChildren()) {
                    String title = snap.child("title").getValue(String.class);
                    String body = snap.child("message").getValue(String.class);
                    Long timestamp = snap.child("timestamp").getValue(Long.class);
                    String date = formatTimestamp(timestamp);
                    addAnnouncement(title, body, date);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Handle error if needed
            }
        });
    }

    private String formatTimestamp(Long timestamp) {
        if (timestamp == null) return "Unknown date";
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }


}
