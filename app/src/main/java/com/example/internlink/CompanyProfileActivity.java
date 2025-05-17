package com.example.internlink;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CompanyProfileActivity extends AppCompatActivity {

    private EditText companyName, industry, location, description, mission, vision, email, phone, address, linkedin, twitter, website;
    private Button btnSave;
    private String COMPANY_ID;
    private DatabaseReference companyRef;
    private MaterialToolbar topAppBar;
    private ImageView companyLogo;
    private Uri selectedLogoUri = null;
    private static final String IMGBB_API_KEY = "93a9e7c9a933826963d704e128929b30";

    private final ActivityResultLauncher<String> logoPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedLogoUri = uri;
                    companyLogo.setImageURI(uri);
                } else {
                    Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
                }
            });

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_company_profile);

        COMPANY_ID = getIntent().getStringExtra("companyId");

        initViews();
        loadCompanyData();

        btnSave.setOnClickListener(v -> {
            if (selectedLogoUri != null) {
                uploadToImgBB(selectedLogoUri);
            } else {
                saveCompanyData(null);
            }
        });

        companyLogo.setOnClickListener(v -> logoPickerLauncher.launch("image/*"));

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
        btnSave = findViewById(R.id.btnSave);
        topAppBar = findViewById(R.id.topAppBar);
        companyLogo = findViewById(R.id.companyLogo);

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

                if (snapshot.hasChild("logoUrl")) {
                    String logoUrl = snapshot.child("logoUrl").getValue(String.class);
                    if (logoUrl != null && !logoUrl.isEmpty()) {
                        Glide.with(CompanyProfileActivity.this)
                                .load(logoUrl.trim())
                                .placeholder(R.drawable.ic_company)
                                .error(R.drawable.ic_company)
                                .into(companyLogo);
                    }
                }
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

    private void uploadToImgBB(Uri uri) {
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("Uploading image...");
        dialog.setCancelable(false);
        dialog.show();

        try {
            InputStream iStream = getContentResolver().openInputStream(uri);
            byte[] inputData = getBytes(iStream);
            String base64Image = Base64.encodeToString(inputData, Base64.NO_WRAP);

            OkHttpClient client = new OkHttpClient();

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("key", IMGBB_API_KEY)
                    .addFormDataPart("image", base64Image)
                    .build();

            Request request = new Request.Builder()
                    .url("https://api.imgbb.com/1/upload")
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> {
                        dialog.dismiss();
                        Toast.makeText(CompanyProfileActivity.this, "Upload failed", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String jsonData = response.body().string();
                    try {
                        JSONObject jsonObject = new JSONObject(jsonData);
                        String imageUrl = jsonObject.getJSONObject("data").getString("display_url");

                        runOnUiThread(() -> {
                            dialog.dismiss();
                            saveCompanyData(imageUrl);
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            dialog.dismiss();
                            Toast.makeText(CompanyProfileActivity.this, "Error parsing response", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            });
        } catch (Exception e) {
            dialog.dismiss();
            Toast.makeText(this, "Error reading image", Toast.LENGTH_SHORT).show();
        }
    }

    private byte[] getBytes(InputStream inputStream) throws Exception {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    private void saveCompanyData(String logoUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", companyName.getText().toString().trim());
        updates.put("industry", industry.getText().toString().trim());
        updates.put("location", location.getText().toString().trim());
        updates.put("description", description.getText().toString().trim());
        updates.put("mission", mission.getText().toString().trim());
        updates.put("vision", vision.getText().toString().trim());
        updates.put("email", email.getText().toString().trim());
        updates.put("phone", phone.getText().toString().trim());
        updates.put("address", address.getText().toString().trim());
        updates.put("linkedin", linkedin.getText().toString().trim());
        updates.put("twitter", twitter.getText().toString().trim());
        updates.put("website", website.getText().toString().trim());
        if (logoUrl != null) updates.put("logoUrl", logoUrl);

        companyRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> Toast.makeText(CompanyProfileActivity.this, "Changes saved successfully", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(CompanyProfileActivity.this, "Failed to save changes", Toast.LENGTH_SHORT).show());
    }
}
