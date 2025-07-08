package com.example.internlink;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.internlink.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CreateCompanyProfileActivity extends AppCompatActivity {

    private String userId, name, email, password;
    private static final String IMGBB_API_KEY = "93a9e7c9a933826963d704e128929b30";
    private static final String DROPBOX_ACCESS_TOKEN = "sl.u.AF1D0l2S-kNVMA90KpvTjEUOSpURzPju4-0-OEU-lqEH-x47pyX00SfPhr9Wz-09yNl9n_RSOcJ5piwARwnIhZ-xITgpKDmi5fuC39Fo46SBuOWTS2Y9wQYpaBunjSoTVmwHQYVBTsbcwM7eGhtN5aIG4vsAoJmiVFSAMQE-IdxwyYe3FQm7td5TnK_knl4ImCm7jKuTxRKye27XuQHwTVV2IXrVqJ85Y7eSCTjq-F-BvZMAv6cT9abS0BtOfXUmEUbLmsH4VWplOknr2qhBrTj3ADEgATXICXnbBL1OfplnbEp33urh78fiek_H4nBp4nAiR5shJx8brJQkd4OJdLJN4KtPpYaKofm6mZxF5pbUBp58PlBFlylsklcoBsnO_Ldmx77cb8KrNN9Kq2xnqfiVJkPseVG6myORh_BAoRPRhFZ2Xa55OkqeTE6IJrnYwMEjs1G4dS7uTQXTqF_x3GXu8fcZcASV-rcWQegztRvmGDjwbJ7fete2QUr_b53HdlAkoOusYpKkMW5wAfnMsfP-q0_tFA5ZSvmZPln0I5aU_GsbcvUdN0UAdpe4MC4VfDWyGqm4o-2NMgihgb5CxJWbkjG2MrG5DzEjKVVUzK3TUReRcNsE_4MIbbjzjM8kFREwb_evnkMMx4-nl-wN_v956whfd425uZo5gzDQg9aEuhXAj2yLQnjME4va0kmAWDmSZtwnDCpM7lbW-hLctqIC1C7eUd_VoCZjK3y_70nWgJs1I5mE5yGjqPA_qWTzDUjs1HCxyJoQGhkodhHzq6TVKdqAouKufdc6SJWvzSHU77aVBD5IsoNqde2m5xwLgQUjruD8fzXEseyV8uDMSoGb_r_xtDaFO94cY2j_npWqp6uRQ_6K_WxuKZ5Pyhzb2XtJF56HngAnp0widABdMeyRAzhhjKIVZ8esvBoEF5NKlC3Gn6bWTGpCmTaBvbfMDdHClOk3TVRlzlIvJLNjrLR-_6OfspPd_hVnR_92V1l5zRVatwkVQT35x4Fgc8NEXTAuFTLkuAde1AoXrEGosUc8bj1igDPey2bySfb2iZtgT8wyuJ34jFckS0S_Q5lY_Gmsg4dSIT76rcQj9IG4BxlrcrDyw4i9axtdZEKnkibdc6RT49CMnkHXeQvTf9rI_LnN2cWa4OYfO7nsT9mTjU1jci1gxStiSQgajOJAniXDB6dqGfE-Dyt2ySGqiw_U3beGwTz_jF9ddHU4u82tvQw7q46CHA7GzpUkd1xHEToinfUuBprYgpCs4CWbws_0CPRQddcd0xYW4NYxANo13sbv2al9ESN9tXntG_vhnIvMj6qga3L7_VT6GmpsNxRX7AFs722C_w1C1ejtONTy8IydlQCOKhliWuNRSziy0oTOJblhnPfHIikew0mnOFILoV2mgqY4jRziRUNMeQtOvF1JK6AbQQ2wlGzyq_CRvFQZG0M0P-PZi5pVeg45-LgmdQM";
    private CheckBox checkAgree;

    private EditText editCompanyName, editIndustry, editLocation, editLinkedIn, editTwitter,
            editWebsite, editDescription, editMission, editVision,
            editEmail, editPhone, editAddress;
    private ImageView uploadLogo;
    private Button btnSubmit;
    private Uri logoUri = null;

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
    private Uri legalDocsUri = null;
    private String uploadedLegalDocsUrl = null;
    private LinearLayout legalDocsDisplayLayout;
    private TextView legalDocsFileName;
    private ImageView deleteLegalDocsIcon;
    private Button btnUploadLegalDocs;

    // Add the PDF picker launcher
    private final ActivityResultLauncher<String> pdfPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    legalDocsUri = uri;
                    showLegalDocsSelected(uri);
                } else {
                    Toast.makeText(this, "No file selected", Toast.LENGTH_SHORT).show();
                }
            }
    );

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
        btnUploadLegalDocs.setOnClickListener(v -> {
            pdfPickerLauncher.launch("application/pdf");
        });

        btnSubmit.setOnClickListener(v -> {
            if (validateInputs()) {
                if (legalDocsUri == null) {
                    Toast.makeText(this, "Please upload legal documents", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (legalDocsUri != null && uploadedLegalDocsUrl == null) {
                    uploadLegalDocsToDropbox(legalDocsUri);
                } else if (logoUri != null) {
                    uploadToImgBB(logoUri);
                } else {
                    submitProfile(null);
                }
            }
        });
    }
    private void uploadLegalDocsToDropbox(Uri uri) {
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("Uploading documents...");
        dialog.setCancelable(false);
        dialog.show();

        OkHttpClient client = new OkHttpClient();

        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            byte[] pdfBytes = getBytes(inputStream);

            RequestBody requestBody = RequestBody.create(MediaType.parse("application/octet-stream"), pdfBytes);

            Request request = new Request.Builder()
                    .url("https://content.dropboxapi.com/2/files/upload")
                    .addHeader("Authorization", "Bearer " + DROPBOX_ACCESS_TOKEN)
                    .addHeader("Content-Type", "application/octet-stream")
                    .addHeader("Dropbox-API-Arg", "{\"path\": \"/legal_docs_"+ userId +".pdf\", \"mode\": \"overwrite\", \"autorename\": true, \"mute\": false}")
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> {
                        dialog.dismiss();
                        Toast.makeText(CreateCompanyProfileActivity.this, "Upload failed", Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        runOnUiThread(() -> {
                            dialog.dismiss();
                            Toast.makeText(CreateCompanyProfileActivity.this, "Upload failed", Toast.LENGTH_LONG).show();
                        });
                        return;
                    }
                    createDropboxSharedLinkForLegalDocs("/legal_docs_" + userId + ".pdf");
                    dialog.dismiss();
                }
            });
        } catch (Exception e) {
            dialog.dismiss();
            Toast.makeText(this, "Error reading file", Toast.LENGTH_SHORT).show();
        }
    }

    // Add method to create shared link
    private void createDropboxSharedLinkForLegalDocs(String path) {
        OkHttpClient client = new OkHttpClient();

        RequestBody body = RequestBody.create(
                MediaType.parse("application/json"),
                "{\"path\": \"" + path + "\", \"settings\": {\"requested_visibility\": \"public\"}}"
        );

        Request request = new Request.Builder()
                .url("https://api.dropboxapi.com/2/sharing/create_shared_link_with_settings")
                .addHeader("Authorization", "Bearer " + DROPBOX_ACCESS_TOKEN)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("DROPBOX_SHARE", "Failed to create shared link");
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    return;
                }

                try {
                    String res = response.body().string();
                    JSONObject json = new JSONObject(res);
                    String url = json.getString("url");
                    uploadedLegalDocsUrl = url.replace("?dl=0", "?raw=1");

                    runOnUiThread(() -> {
                        if (logoUri != null) {
                            uploadToImgBB(logoUri);
                        } else {
                            submitProfile(null);
                        }
                    });

                } catch (Exception e) {
                    Log.e("DROPBOX_SHARE", "Parse error: " + e.getMessage());
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
        uploadLogo = findViewById(R.id.uploadLogo);
        btnSubmit = findViewById(R.id.btnSubmit);
        checkAgree = findViewById(R.id.checkAgree);
        legalDocsDisplayLayout = findViewById(R.id.legalDocsDisplayLayout);
        legalDocsFileName = findViewById(R.id.legalDocsFileName);
        deleteLegalDocsIcon = findViewById(R.id.deleteLegalDocsIcon);
        btnUploadLegalDocs = findViewById(R.id.btnUploadLegalDocs);
    }
    private void showLegalDocsSelected(Uri uri) {
        legalDocsDisplayLayout.setVisibility(View.VISIBLE);
        legalDocsFileName.setText(uri.getLastPathSegment());

        deleteLegalDocsIcon.setOnClickListener(v -> {
            legalDocsUri = null;
            uploadedLegalDocsUrl = null;
            legalDocsDisplayLayout.setVisibility(View.GONE);
            Toast.makeText(this, "Documents removed", Toast.LENGTH_SHORT).show();
        });
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

    private void uploadToImgBB(Uri uri) {
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("Uploading image...");
        dialog.setCancelable(false);
        dialog.show();

        try {
            InputStream iStream = getContentResolver().openInputStream(uri);
            byte[] inputData = getBytes(iStream);
            String base64Image = Base64.encodeToString(inputData, Base64.DEFAULT);

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
                        Toast.makeText(CreateCompanyProfileActivity.this, "Upload failed", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String jsonData = response.body().string();
                    try {
                        JSONObject jsonObject = new JSONObject(jsonData);
                        String imageUrl = jsonObject.getJSONObject("data").getString("url");

                        runOnUiThread(() -> {
                            dialog.dismiss();
                            submitProfile(imageUrl);
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            dialog.dismiss();
                            Toast.makeText(CreateCompanyProfileActivity.this, "Error parsing response", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            });
        } catch (Exception e) {
            dialog.dismiss();
            Toast.makeText(this, "Error reading image", Toast.LENGTH_SHORT).show();
        }
    }

    public byte[] getBytes(InputStream inputStream) throws Exception {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    private void submitProfile(String logoUrl) {
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("Saving profile...");
        dialog.setCancelable(false);
        dialog.show();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users").child(userId);
        DatabaseReference adminAnnouncementsRef = FirebaseDatabase.getInstance().getReference("announcements_by_role").child("admin");

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
        if (uploadedLegalDocsUrl != null) companyData.put("legalDocsUrl", uploadedLegalDocsUrl);

        ref.updateChildren(companyData).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Create admin announcement for legal documents verification
                String announcementId = adminAnnouncementsRef.push().getKey();
                if (announcementId != null) {
                    Map<String, Object> announcement = new HashMap<>();
                    announcement.put("id", announcementId);
                    announcement.put("title", "New Company Registration");
                    announcement.put("message", "A new company \"" + editCompanyName.getText().toString().trim() +
                            "\" has registered and needs legal documents verification.");
                    announcement.put("type", "company_registration");
                    announcement.put("companyId", userId);
                    announcement.put("createdBy", userId);
                    announcement.put("timestamp", ServerValue.TIMESTAMP);
                    announcement.put("isRead", false);
                    announcement.put("date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            .format(new Date()));

                    // Add the legal docs URL to the announcement
                    if (uploadedLegalDocsUrl != null) {
                        announcement.put("legalDocsUrl", uploadedLegalDocsUrl);
                    }

                    adminAnnouncementsRef.child(announcementId).setValue(announcement)
                            .addOnCompleteListener(announcementTask -> {
                                dialog.dismiss();
                                if (announcementTask.isSuccessful()) {
                                    Toast.makeText(CreateCompanyProfileActivity.this,
                                            "Profile created successfully", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(CreateCompanyProfileActivity.this,
                                            CompanyHomeActivity.class);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    Toast.makeText(CreateCompanyProfileActivity.this,
                                            "Profile created but failed to notify admin", Toast.LENGTH_SHORT).show();
                                }
                            });
                } else {
                    dialog.dismiss();
                    Toast.makeText(CreateCompanyProfileActivity.this,
                            "Profile created successfully", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(CreateCompanyProfileActivity.this,
                            CompanyHomeActivity.class);
                    startActivity(intent);
                    finish();
                }
            } else {
                dialog.dismiss();
                Toast.makeText(CreateCompanyProfileActivity.this,
                        "Failed to save profile", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
