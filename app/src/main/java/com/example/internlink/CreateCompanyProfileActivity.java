package com.example.internlink;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CreateCompanyProfileActivity extends AppCompatActivity {

    private String userId, name, email, password;

    private EditText editCompanyName, editIndustry, editLocation, editLinkedIn, editTwitter,
            editWebsite, editDescription, editMission, editVision,
            editEmail, editPhone, editAddress;
    private ImageView uploadLogo;
    private ChipGroup chipGroupExpertise;
    private MaterialButton btnSubmit;

    private Uri logoUri = null;
    private final OkHttpClient httpClient = new OkHttpClient();

    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    logoUri = uri;
                    uploadLogo.setImageURI(uri);
                } else {
                    Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_company_profile);

        userId = getIntent().getStringExtra("userId");
        name = getIntent().getStringExtra("name");
        email = getIntent().getStringExtra("email");
        password = getIntent().getStringExtra("password");

        initViews();
        prefillFields();

        uploadLogo.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        btnSubmit.setOnClickListener(v -> {
            if (validateInputs()) {
                if (logoUri != null) {
                    uploadLogoToImgur();
                } else {
                    submitProfile(null);
                }
            }
        });
    }

    private void initViews() {
        editCompanyName = findViewById(R.id.editCompanyName);
        editIndustry = findViewById(R.id.editIndustry);
        editLocation = findViewById(R.id.editLocation);
        editLinkedIn = findViewById(R.id.editLinkedIn);
        editTwitter = findViewById(R.id.editTwitter);
        editWebsite = findViewById(R.id.editWebsite);
        editDescription = findViewById(R.id.editDescription);
        editMission = findViewById(R.id.editMission);
        editVision = findViewById(R.id.editVision);
        editEmail = findViewById(R.id.editEmail);
        editPhone = findViewById(R.id.editPhone);
        editAddress = findViewById(R.id.editAddress);
        chipGroupExpertise = findViewById(R.id.chipGroupExpertise);
        uploadLogo = findViewById(R.id.uploadLogo);
        btnSubmit = findViewById(R.id.btnSubmit);
    }

    private void prefillFields() {
        if (name != null) editCompanyName.setText(name);
        if (email != null) editEmail.setText(email);
    }

    private boolean validateInputs() {
        return !(TextUtils.isEmpty(editCompanyName.getText()) ||
                TextUtils.isEmpty(editIndustry.getText()) ||
                TextUtils.isEmpty(editLocation.getText()) ||
                TextUtils.isEmpty(editEmail.getText()));
    }

    private void uploadLogoToImgur() {
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("Uploading logo to Imgur...");
        dialog.setCancelable(false);
        dialog.show();

        try {
            InputStream inputStream = getContentResolver().openInputStream(logoUri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            String base64Image = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);

            RequestBody requestBody = RequestBody.create(
                    MediaType.parse("application/x-www-form-urlencoded"),
                    "image=" + base64Image);

            Request request = new Request.Builder()
                    .url("https://api.imgur.com/3/image")
                    .post(requestBody)
                    .addHeader("Authorization", "Client-ID YOUR_IMGUR_CLIENT_ID") // Replace with your real Client-ID
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> {
                        dialog.dismiss();
                        Toast.makeText(CreateCompanyProfileActivity.this, "Failed to upload logo", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) {
                    runOnUiThread(() -> dialog.dismiss());
                    try {
                        String responseBody = response.body().string();
                        JSONObject json = new JSONObject(responseBody);
                        String imageUrl = json.getJSONObject("data").getString("link");
                        runOnUiThread(() -> submitProfile(imageUrl));
                    } catch (Exception e) {
                        runOnUiThread(() -> Toast.makeText(CreateCompanyProfileActivity.this, "Error parsing Imgur response", Toast.LENGTH_SHORT).show());
                    }
                }
            });
        } catch (Exception e) {
            dialog.dismiss();
            Toast.makeText(this, "Error reading image", Toast.LENGTH_SHORT).show();
        }
    }

    private void submitProfile(String logoUrl) {
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("Saving profile...");
        dialog.setCancelable(false);
        dialog.show();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users").child(userId);

        Map<String, Object> companyData = new HashMap<>();
        companyData.put("name", editCompanyName.getText().toString().trim());
        companyData.put("industry", editIndustry.getText().toString().trim());
        companyData.put("location", editLocation.getText().toString().trim());
        companyData.put("linkedin", editLinkedIn.getText().toString().trim());
        companyData.put("twitter", editTwitter.getText().toString().trim());
        companyData.put("website", editWebsite.getText().toString().trim());
        companyData.put("description", editDescription.getText().toString().trim());
        companyData.put("mission", editMission.getText().toString().trim());
        companyData.put("vision", editVision.getText().toString().trim());
        companyData.put("email", editEmail.getText().toString().trim());
        companyData.put("phone", editPhone.getText().toString().trim());
        companyData.put("address", editAddress.getText().toString().trim());
        companyData.put("role", "company");
        if (logoUrl != null) companyData.put("logoUrl", logoUrl);

        StringBuilder expertiseBuilder = new StringBuilder();
        for (int i = 0; i < chipGroupExpertise.getChildCount(); i++) {
            Chip chip = (Chip) chipGroupExpertise.getChildAt(i);
            if (chip.isChecked()) {
                expertiseBuilder.append(chip.getText()).append(", ");
            }
        }
        String expertise = expertiseBuilder.toString().trim();
        if (expertise.endsWith(",")) {
            expertise = expertise.substring(0, expertise.length() - 1);
        }
        companyData.put("expertise", expertise);

        ref.updateChildren(companyData).addOnCompleteListener(task -> {
            dialog.dismiss();
            if (task.isSuccessful()) {
                Toast.makeText(this, "Profile created successfully", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, CompanyHomeActivity.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Failed to save profile", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
