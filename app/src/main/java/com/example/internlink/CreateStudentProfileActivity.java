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

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONException;
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

public class CreateStudentProfileActivity extends AppCompatActivity {

    private String userId, name, email, password;
    private EditText editFullName, editEmail, editPhone, editBio, editUniversity, editDegree, editGradYear, editGPA, editPortfolio, editLinkedIn, editGitHub;
    private ImageView profilePhoto;
    private Button btnSubmit, btnUploadCV;
    private CheckBox checkAgree;
    private Uri logoUri = null;
    private Uri cvUri = null;
    private String uploadedCVUrl = null;
    private LinearLayout cvDisplayLayout;
    private TextView cvFileName;
    private ImageView deleteCVIcon;


    private static final String IMGBB_API_KEY = "93a9e7c9a933826963d704e128929b30";
    private static final String DROPBOX_ACCESS_TOKEN = "sl.u.AFuFPL7ei6qtpOj9ouxtuzP6J_v2WVYpv-ZbRRyTDkwKYNy6PXoRQh1rujT1Ze0pyBsA3AkE1Kezsq6lcZr8r7t2U0y3A7JzRQj8Ezc0OlxdGWHCxBvjLp9V_VU3m9y0rEMoRSAGPPKbfytpo3A10RzZCrt2VTsDLck-bAvdvR1a18n_-7DBoT0tGujvfeMYujTdmXNCEjNogakzReU01uhKd8q3xNrDTIcGmmfiQAPODb5dBYKXldKex79lm-4HNpce-_Dpyd4FaCOU23dFfGB1w0DjFD-JImGIdJtz0WEq0p36VJvGTZQL63Lixv523JHunN4QFqwQonmFWxp5fnV8eroRP8U70U4JWshKd_J-GxQWKTwU8EisbHHhY7soYqDSJBAzTekJ5tdD8MGXvJKTvvHd2B6NCx3Hids13kDnH2XzHImuRb8NiQWPZr3ujydLqTIWTRXfmu8Vh8QfV5FJmm_akUuX8uN6XkFdfE1ebj-FJhaYPyq8A8pJEmlWGKAu1AdPeUdA9oZeeHHGN4t_UPG-5CTKcSdLt_1d43JbMM_7Ffc0nCUuYa-LccYiywXBO7jp1TLuRFrv0yvgQS59faG-A_8cMQ1KK6Lfc4DF58u8-YhETjqmIUXTyJFoUcaqo6EMduCyR9oPNWt3mFFEtOce69kMCQ99y_4BVJHi6FROtS3S3kZG1nRo0p2ig76bbW5MTktX_EPnX8IkksHT1Dz8pjeyb6se9Ulcm9tzBE59wi6vtOsav0vgkuKWlWrTcEgGhXxUhFRgeGsisSuDp_wBMZS4yV7J9TykDIUi6Az0hBWqiUujYicV0bWCYvA1yIV7aKbtW3dE7rk-AQyT9T1TuqQHvDsYZ-Yf5d0nPOyqiq1TNX3azUbyNhmpXyRh3iEVuYWLi13h5QCEYcUQywXAhvIh3EklkgrLLnKjYXVRYs44y8GGlJqxz2EB8M8F73Jl6AbyqZ38w-Q-uDkAXZn_FXnvn37OV_RGOy-84CtlzX2C8DNXOQ-_IbAZ_KULRdQBXU92_r0aO3U-woDa5SZTiEej2qPMG7RF6FzUcIEB8VA5etxDu7y8dhxfRSgBuEnQHvChqjoD1J8caFPSNhKf2MqvNZ84Z1-t5P1GroTRsH4RdvbL3d3tmxNB_mnxz96q-KecPJYmqmfj_y3XuWd0DxvRmoW94iKNpjGinwkhNfUoxO96Mab5UQOECcLI2xDViJNSewnJG7l2lcStAmR-T4f_0IuG5gC22b8cODyrzLJB9rTbozV4b7eYP15DSjUA8f6ioWa5KJeG35tZVbX8WINeepkRs3QgQeSi1511i5OFig-JEfGNzxtZg9chnS71LdS6qaAEvXAVFQ-W_wAm64MxLxTh5fmTRoiIf_nd0jC59rSX0mwOHfr4xQKyv6RBNQjvi6mRJnxSp9Xp_5xdqtWI_mcjUcsMvWTa_Q";

    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    logoUri = uri;
                    profilePhoto.setImageURI(uri);
                } else {
                    Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<String> pdfPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    cvUri = uri;
                    showCVSelected(uri);
                } else {
                    Toast.makeText(this, "No file selected", Toast.LENGTH_SHORT).show();
                }
            }
    );


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_student_profile);

        userId = getIntent().getStringExtra("userId");
        name = getIntent().getStringExtra("name");
        email = getIntent().getStringExtra("email");
        password = getIntent().getStringExtra("password");

        initViews();
        prefillFields();

        profilePhoto.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        btnSubmit.setOnClickListener(v -> {
            if (validateInputs()) {
                if (cvUri != null && uploadedCVUrl == null) {
                    uploadPDFToDropbox(cvUri);
                } else {
                    if (logoUri != null) {
                        uploadToImgBB(logoUri);
                    } else {
                        submitProfile(null);
                    }
                }
            }
        });

        btnUploadCV.setOnClickListener(v -> {
            pdfPickerLauncher.launch("application/pdf");
        });

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
                        Toast.makeText(CreateStudentProfileActivity.this, "Upload failed", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    try {
                        String jsonData = response.body().string();
                        Log.d("GOFILE_RESPONSE", jsonData);
                        JSONObject jsonObject = new JSONObject(jsonData);
                        String imageUrl = jsonObject.getJSONObject("data").getString("url");

                        runOnUiThread(() -> {
                            dialog.dismiss();
                            submitProfile(imageUrl);
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            dialog.dismiss();
                            Toast.makeText(CreateStudentProfileActivity.this, "Error parsing response", Toast.LENGTH_SHORT).show();
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
    private void showCVSelected(Uri uri) {
        cvDisplayLayout.setVisibility(View.VISIBLE);
        cvFileName.setText(uri.getLastPathSegment());

        deleteCVIcon.setOnClickListener(v -> {
            cvUri = null;
            uploadedCVUrl = null;
            cvDisplayLayout.setVisibility(View.GONE);
            Toast.makeText(this, "CV removed", Toast.LENGTH_SHORT).show();
        });
    }


    private void initViews() {
        editFullName = findViewById(R.id.editFullName);
        editBio = findViewById(R.id.editBio);
        editUniversity = findViewById(R.id.editUniversity);
        editLinkedIn = findViewById(R.id.editLinkedIn);
        editDegree = findViewById(R.id.editDegree);
        editGradYear = findViewById(R.id.editGradYear);
        editGPA = findViewById(R.id.editGPA);
        editGitHub = findViewById(R.id.editGitHub);
        editEmail = findViewById(R.id.editEmail);
        editPhone = findViewById(R.id.editPhone);
        profilePhoto = findViewById(R.id.profilePhoto);
        btnUploadCV = findViewById(R.id.btnUploadCV);
        checkAgree = findViewById(R.id.checkAgree);
        cvDisplayLayout = findViewById(R.id.cvDisplayLayout);
        cvFileName = findViewById(R.id.cvFileName);
        deleteCVIcon = findViewById(R.id.deleteCVIcon);
        btnSubmit = findViewById(R.id.btnSubmit);
    }

    private void prefillFields() {
        if (name != null) editFullName.setText(name);
        if (email != null) editEmail.setText(email);
    }

    private boolean validateInputs() {
        return !(TextUtils.isEmpty(editFullName.getText()) || TextUtils.isEmpty(editEmail.getText()));
    }

    private void submitProfile(String logoUrl) {
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("Saving profile...");
        dialog.setCancelable(false);
        dialog.show();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users").child(userId);

        Map<String, Object> companyData = new HashMap<>();
        companyData.put("name", editFullName.getText().toString().trim());
        companyData.put("bio", editBio.getText().toString().trim());
        companyData.put("university", editUniversity.getText().toString().trim());
        companyData.put("linkedin", editLinkedIn.getText().toString().trim());
        companyData.put("degree", editDegree.getText().toString().trim());
        companyData.put("gradyear", editGradYear.getText().toString().trim());
        companyData.put("gpa", editGPA.getText().toString().trim());
        companyData.put("github", editGitHub.getText().toString().trim());
        companyData.put("email", editEmail.getText().toString().trim());
        companyData.put("phone", editPhone.getText().toString().trim());
        companyData.put("role", "student");
        if (logoUrl != null) companyData.put("logoUrl", logoUrl);
        if (uploadedCVUrl != null) companyData.put("cvUrl", uploadedCVUrl);

        ref.updateChildren(companyData).addOnCompleteListener(task -> {
            dialog.dismiss();
            if (task.isSuccessful()) {
                Toast.makeText(this, "Profile created successfully", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, StudentHomeActivity.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Failed to save profile", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void uploadPDFToDropbox(Uri uri) {
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("Uploading CV...");
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
                    .addHeader("Dropbox-API-Arg", "{\"path\": \"/cv_"+ userId +".pdf\", \"mode\": \"overwrite\", \"autorename\": true, \"mute\": false}")
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> {
                        dialog.dismiss();
                        Toast.makeText(CreateStudentProfileActivity.this, "Dropbox upload failed", Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    dialog.dismiss();
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "No body";
                        Log.e("DROPBOX_UPLOAD", "Upload failed: " + errorBody);
                        runOnUiThread(() ->
                                Toast.makeText(CreateStudentProfileActivity.this, "Upload error: " + errorBody, Toast.LENGTH_LONG).show()
                        );
                        return;
                    }


                    // Create a shared link for public access
                    createDropboxSharedLink("/cv_" + userId + ".pdf");
                }
            });
        } catch (Exception e) {
            dialog.dismiss();
            Toast.makeText(this, "Error reading file", Toast.LENGTH_SHORT).show();
        }
    }
    private void createDropboxSharedLink(String path) {
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
                    String errorBody = response.body() != null ? response.body().string() : "No body";
                    Log.e("DROPBOX_SHARE", "Error: " + response.code() + " - " + errorBody);
                    return;
                }

                try {
                    String res = response.body().string();
                    JSONObject json = new JSONObject(res);
                    String url = json.getString("url");

                    // Replace ?dl=0 with ?raw=1 for direct download
                    uploadedCVUrl = url.replace("?dl=0", "?raw=1");

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



}
