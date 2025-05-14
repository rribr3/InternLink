package com.example.internlink;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class CompanyProfileActivity extends AppCompatActivity {

    private EditText companyName, industry, location, description, mission, vision, email, phone, address, linkedin, twitter, website;
    private LinearLayout socialLinksContainer;
    private Button btnSave;
    private String COMPANY_ID;
    private DatabaseReference companyRef;
    private MaterialToolbar topAppBar;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_company_profile);

        COMPANY_ID = getIntent().getStringExtra("companyId");

        initViews();
        loadCompanyData();

        btnSave.setOnClickListener(v -> saveCompanyData());

        // 🔙 Handle navigation icon click (back arrow)
        topAppBar.setNavigationOnClickListener(v -> {
            Intent intent = new Intent(CompanyProfileActivity.this, CompanyHomeActivity.class);
            intent.putExtra("companyId", COMPANY_ID);
            startActivity(intent);
            finish();
        });
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
        linkedin = findViewById(R.id.linkedin);
        twitter = findViewById(R.id.twitter);
        website = findViewById(R.id.website);
        socialLinksContainer = findViewById(R.id.socialLinksContainer);
        btnSave = findViewById(R.id.btnSave);
        topAppBar = findViewById(R.id.topAppBar); // Toolbar reference

        companyRef = FirebaseDatabase.getInstance().getReference("users").child(COMPANY_ID);
    }

    private void loadCompanyData() {
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
                setSafeText(description, snapshot.child("description"));
                setSafeText(mission, snapshot.child("mission"));
                setSafeText(vision, snapshot.child("vision"));
                setSafeText(email, snapshot.child("email"));
                setSafeText(phone, snapshot.child("phone"));
                setSafeText(address, snapshot.child("address"));
                setSafeText(linkedin, snapshot.child("linkedin"));
                setSafeText(twitter, snapshot.child("twitter"));
                setSafeText(website, snapshot.child("website"));

                socialLinksContainer.removeAllViews();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CompanyProfileActivity.this, "Failed to load data.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setSafeText(EditText editText, DataSnapshot snapshot) {
        String value = snapshot.getValue(String.class);
        if (value != null) {
            editText.setText(value);
        }
    }

    private void saveCompanyData() {
        String updatedName = companyName.getText().toString().trim();
        String updatedIndustry = industry.getText().toString().trim();
        String updatedLocation = location.getText().toString().trim();
        String updatedDescription = description.getText().toString().trim();
        String updatedMission = mission.getText().toString().trim();
        String updatedVision = vision.getText().toString().trim();
        String updatedEmail = email.getText().toString().trim();
        String updatedPhone = phone.getText().toString().trim();
        String updatedAddress = address.getText().toString().trim();
        String updatedLinkedin = linkedin.getText().toString().trim();
        String updatedTwitter = twitter.getText().toString().trim();
        String updatedWebsite = website.getText().toString().trim();

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", updatedName);
        updates.put("industry", updatedIndustry);
        updates.put("location", updatedLocation);
        updates.put("description", updatedDescription);
        updates.put("mission", updatedMission);
        updates.put("vision", updatedVision);
        updates.put("email", updatedEmail);
        updates.put("phone", updatedPhone);
        updates.put("address", updatedAddress);
        updates.put("linkedin", updatedLinkedin);
        updates.put("twitter", updatedTwitter);
        updates.put("website", updatedWebsite);

        companyRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> Toast.makeText(CompanyProfileActivity.this, "Changes saved successfully", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(CompanyProfileActivity.this, "Failed to save changes", Toast.LENGTH_SHORT).show());
    }
}
