package com.example.internlink;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class CompanyProfileActivity extends AppCompatActivity {

    private TextView companyName, industry, location, description, mission, vision, email, phone, address;
    private ChipGroup expertiseChipGroup;
    private LinearLayout socialLinksContainer;

    private final String COMPANY_ID = "-OPyd6GlRTe3-fuwvrvV";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_company_profile); // Replace with your actual layout

        initViews();
        loadCompanyData();
    }

    private void initViews() {
        companyName = findViewById(R.id.companyName);
        industry = findViewById(R.id.industry);
        location = findViewById(R.id.location);
        description = findViewById(R.id.description);
        mission = findViewById(R.id.mission);
        vision = findViewById(R.id.vision);
        email = findViewById(R.id.email);
        phone = findViewById(R.id.phone);
        address = findViewById(R.id.address);
        expertiseChipGroup = findViewById(R.id.expertiseChipGroup);
        socialLinksContainer = findViewById(R.id.socialLinksContainer);
    }

    private void loadCompanyData() {
        DatabaseReference companyRef = FirebaseDatabase.getInstance().getReference("companies").child(COMPANY_ID);

        companyRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(CompanyProfileActivity.this, "Company not found", Toast.LENGTH_SHORT).show();
                    return;
                }

                setSafeText(companyName, snapshot.child("name"));
                setSafeText(industry, snapshot.child("industry"));
                setSafeText(location, snapshot.child("location"));
                setSafeText(description, snapshot.child("about"));
                setSafeText(mission, snapshot.child("mission"));
                setSafeText(vision, snapshot.child("vision"));

                // Contact
                setSafeText(email, snapshot.child("contact").child("email"));
                setSafeText(phone, snapshot.child("contact").child("phone"));
                setSafeText(address, snapshot.child("contact").child("address"));

                // Expertise
                expertiseChipGroup.removeAllViews();
                for (DataSnapshot chip : snapshot.child("expertise").getChildren()) {
                    String skill = chip.getValue(String.class);
                    if (skill != null) {
                        Chip newChip = new Chip(CompanyProfileActivity.this);
                        newChip.setText(skill);
                        newChip.setChipBackgroundColorResource(R.color.chip_background);
                        newChip.setTextColor(getResources().getColor(R.color.black));
                        newChip.setClickable(false);
                        newChip.setCheckable(false);
                        expertiseChipGroup.addView(newChip);
                    }
                }

                // Social Links
                socialLinksContainer.removeAllViews();
                for (DataSnapshot social : snapshot.child("social").getChildren()) {
                    String platform = social.getKey();
                    String link = social.getValue(String.class);
                    if (link != null && !link.isEmpty()) {
                        TextView socialLink = new TextView(CompanyProfileActivity.this);
                        socialLink.setText(platform + ": " + link);
                        socialLink.setTextColor(getResources().getColor(R.color.blue_dark));
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                        );
                        params.setMargins(16, 8, 16, 8);
                        socialLink.setLayoutParams(params);
                        socialLinksContainer.addView(socialLink);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CompanyProfileActivity.this, "Failed to load data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setSafeText(TextView textView, DataSnapshot snapshot) {
        try {
            if (snapshot.getValue() instanceof String) {
                textView.setText(snapshot.getValue(String.class));
            } else if (snapshot.getValue() instanceof java.util.Map) {
                StringBuilder sb = new StringBuilder();
                for (DataSnapshot child : snapshot.getChildren()) {
                    sb.append(child.getKey()).append(": ").append(child.getValue(String.class)).append("\n");
                }
                textView.setText(sb.toString().trim());
            } else {
                textView.setText("Not available");
            }
        } catch (Exception e) {
            textView.setText("Error loading");
        }
    }
}