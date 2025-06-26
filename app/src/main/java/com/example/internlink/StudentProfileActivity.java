package com.example.internlink;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
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

public class StudentProfileActivity extends AppCompatActivity {

    private EditText studentName, university, degree, bio, gradyear, skills, email, phone, linkedin, twitter, github, gpa;
    Button generateCvBtn;
    private LinearLayout cvDisplayLayout;
    private TextView cvFileName;
    private ImageView btnDeleteCv;
    private String cvUrl;
    private Uri cvUri = null;
    private Button uploadBtn, viewBtn;
    private Button btnSave;
    private String STUDENT_ID;
    private DatabaseReference studentRef;
    private MaterialToolbar topAppBar;
    private ImageView studentPfp;
    private Uri selectedLogoUri = null;
    private static final String IMGBB_API_KEY = "93a9e7c9a933826963d704e128929b30";
    private static final String DROPBOX_ACCESS_TOKEN = "sl.u.AFz_DGqRs2K9mOVIJNUOvWOvjQ_XUO55XJjX5Wc9ZTO2WbB8oi2PW0NnuZNQfizj5dhW1nGr3dnLnwabT7UPXosvZ5aBzm5XtSTO3SMB4nWixmoi2WD9UhgPtCfEXGYY631oyI7knERyfQTWlC2GC07elI56zTChR1iKSZH6bHV-C3-8AtKzdHT1q80a9uODuWETskXaZ-0V5nszaRd8aVEZJl-YRa0Aql6ngeuo4dKU3HZsehr-l35OwNg-BJtF5dGr3w9BMlDsEfOT9DWa_VBmEL1CBqWPQ0rqniwyizuip486VxiMJ7rn2FzxIr_b1Besfij5q9iUDkHRU_dXdjk8K-aYBx1lQdh26_zS25eQXL4CkKghHekEh809C8xdbPQqxhcjotWB_KXFrZ9DY2ItK12o4ecClmhAdjorI9OnMahHiqzbC4wvNneukLqGQzVRCJu_OZc0vTjW0hMhgoDxDHqwOp0HHZ_zx2bIQpCG44LzcgfzJnMeO8spckXaIAZnQC07-Luu_bHHnNphuTst_dF-A1LkYAi6qP77vwAlCcHyAp-uj4lblN6695j0RofCoUPV-LSGCvlAR9Uvpehb6mppkRWNwCy-aUk1SS8UW-0ddz0Z8GGgAEAA022Z0m2nU8xqcsjoNubpyq03L5F-2zr6lh0hrWEqQ_-I_8JucZj5VSO_AbvIYQLc8-RfN5Yo4zQp5ZMp0B54XoNRuj_odDllPaQ7CgJeCJvotvz6xIfdxFNOlkoN67ITL3zjuQQbriZovSNoGlOb1Qxyr46tJcHtE346eACxU2K86ju4QzTE2KGI0OmOwhFq3vXDq-rDKhAMFrFs0nZvpoVuXAXr3LKP1G-hTmBjDX1_uzsgq_FV7FamIbdx0pG7hByxD3t82s-onCiDCy22_hsRekc0O0jiRBIpSMpOIFAjLpff1vlU_5W2b6VXc_zKgOGfNgp30p36_tEq_1wySHybZ5RDYTD172q_1X5d5UwDNnKWzoVgS91cjfkqXEs-pYeYHDEB2ki9vVRRGLrlFPGxvohFX4uDzKn3aj43D98cQPiErqlNBLcmtRrD4d5NRCnYRqGmUWe8k4OQ1brs447skFSvbqFVSqnb9M9Wt4uGQgQVIL1M8MfajWUPkUOo_44woY8gFq1r3j_fYc4q5_GOwt7kNsqgyBm2fxB7-8-6_FttdWwkv1nw6UiaA5ZJsQlGxPYFHlD1vNb3xgcO4P0z1gS2xp5Yx-QB6U4lGdQt9yDKcK0Fw7gIs25bLJ6M8BY2UymXh0npcpLxN2hRdbgHHWbzFWqM50Wsv_JGu-WFQf5qIuXlTj-da50_mbYxZ0E8QazydJI5LdxzA-URMqcjPUWPNxJS3bYsNwHcbrXQca5YoIelqW7tGLjYR87h1JxKkf-BaDwOLGUvb_I-o0IMG3RmJcl2tr1aVUSjCiUqf4LeqUphdmYR1C-ASSui5iG3K-A";
    private final ActivityResultLauncher<String> pdfPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    cvUri = uri;
                    String fileName = uri.getLastPathSegment();
                    cvFileName.setText(fileName != null ? fileName : "CV.pdf");
                    cvDisplayLayout.setVisibility(View.VISIBLE);

                    // Upload to Dropbox and Firebase
                    uploadPdfToDropbox(uri);  // This handles both upload and saving to Firebase
                } else {
                    Toast.makeText(this, "No PDF selected", Toast.LENGTH_SHORT).show();
                }
            }
    );

    private final ActivityResultLauncher<String> logoPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedLogoUri = uri;
                    studentPfp.setImageURI(uri);
                } else {
                    Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
                }
            });

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_profile);

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            STUDENT_ID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }



        initViews();
        loadCompanyData();

        btnSave.setOnClickListener(v -> {
            Map<String, Object> updates = new HashMap<>();
            updates.put("name", studentName.getText().toString().trim());
            updates.put("bio", bio.getText().toString().trim());
            updates.put("degree", degree.getText().toString().trim());
            updates.put("github", github.getText().toString().trim());
            updates.put("gpa", gpa.getText().toString().trim());
            updates.put("gradyear", gradyear.getText().toString().trim());
            updates.put("email", email.getText().toString().trim());
            updates.put("phone", phone.getText().toString().trim());
            updates.put("university", university.getText().toString().trim());
            updates.put("linkedin", linkedin.getText().toString().trim());
            updates.put("twitter", twitter.getText().toString().trim());
            updates.put("skills", skills.getText().toString().trim());

            if (selectedLogoUri != null) {
                uploadToImgBB(selectedLogoUri); // handle photo upload separately
            } else {
                studentRef.updateChildren(updates)
                        .addOnSuccessListener(aVoid -> Toast.makeText(StudentProfileActivity.this, "Changes saved", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(StudentProfileActivity.this, "Failed to save changes", Toast.LENGTH_SHORT).show());
            }
        });



        studentPfp.setOnClickListener(v -> logoPickerLauncher.launch("image/*"));

        topAppBar.setNavigationOnClickListener(v -> {
            Intent intent = new Intent(StudentProfileActivity.this, StudentHomeActivity.class);
            intent.putExtra("studentId", STUDENT_ID);
            startActivity(intent);
            finish();
        });
        btnDeleteCv.setOnClickListener(v -> {
            if (cvUrl != null && !cvUrl.isEmpty()) {
                deleteCvFromDropbox(cvUrl);
            }
        });
        uploadBtn.setOnClickListener(v -> {
            pdfPickerLauncher.launch("application/pdf");
        });
        viewBtn.setOnClickListener(v -> {
            if (cvUrl != null && !cvUrl.isEmpty()) {
                // Log the URL for debugging
                Log.d("StudentProfile", "Opening PDF URL: " + cvUrl);

                // Verify the URL is accessible
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url(cvUrl)
                        .head()  // Only check headers, don't download content
                        .build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        runOnUiThread(() -> {
                            Toast.makeText(StudentProfileActivity.this,
                                    "Cannot access PDF: Link may have expired", Toast.LENGTH_LONG).show();
                        });
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        if (response.isSuccessful()) {
                            runOnUiThread(() -> {
                                Intent intent = new Intent(StudentProfileActivity.this, PdfViewerActivity.class);
                                intent.putExtra("pdf_url", cvUrl);
                                startActivity(intent);
                            });
                        } else {
                            runOnUiThread(() -> {
                                Toast.makeText(StudentProfileActivity.this,
                                        "PDF link is no longer valid. Please re-upload.", Toast.LENGTH_LONG).show();
                            });
                        }
                    }
                });
            } else {
                Toast.makeText(StudentProfileActivity.this, "No CV uploaded", Toast.LENGTH_SHORT).show();
            }
        });
        generateCvBtn.setOnClickListener(v -> {
                Intent intent = new Intent(StudentProfileActivity.this, CvGenerated.class);
                startActivity(intent);
        });




    }
    // Update your saveStudentData method to also store the file path
    private void saveStudentDataWithPath(String tempLink, String filePath) {
        Map<String, Object> updates = new HashMap<>();
        if (tempLink != null) {
            updates.put("cvUrl", tempLink);
            updates.put("cvPath", filePath);  // Store the actual file path for deletion
            this.cvUrl = tempLink;
            cvFileName.setText("CV.pdf");  // Simple display name
            cvDisplayLayout.setVisibility(View.VISIBLE);
        }

        studentRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Saved CV successfully", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show());
    }

    // Keep the old method for compatibility, but add path storage
    private void saveStudentData(String tempLink) {
        String defaultPath = "/cv_" + STUDENT_ID + ".pdf";
        saveStudentDataWithPath(tempLink, defaultPath);
    }


    private void uploadPdfToDropbox(Uri pdfUri) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Uploading CV...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        try {
            InputStream inputStream = getContentResolver().openInputStream(pdfUri);
            if (inputStream == null) {
                progressDialog.dismiss();
                Toast.makeText(this, "Failed to read selected PDF", Toast.LENGTH_SHORT).show();
                return;
            }

            byte[] pdfData = getBytes(inputStream);

            OkHttpClient client = new OkHttpClient();

            // Use consistent file naming - remove timestamp
            String fileName = "/cv_" + STUDENT_ID + ".pdf";

            // Step 1: Upload the PDF to Dropbox
            JSONObject dropboxArg = new JSONObject();
            dropboxArg.put("path", fileName);  // Use the consistent filename
            dropboxArg.put("mode", "overwrite");  // This will overwrite existing files
            dropboxArg.put("autorename", false);   // Don't auto-rename
            dropboxArg.put("mute", false);
            dropboxArg.put("strict_conflict", false);

            RequestBody uploadBody = RequestBody.create(MediaType.parse("application/octet-stream"), pdfData);
            Request uploadRequest = new Request.Builder()
                    .url("https://content.dropboxapi.com/2/files/upload")
                    .addHeader("Authorization", "Bearer " + DROPBOX_ACCESS_TOKEN)
                    .addHeader("Dropbox-API-Arg", dropboxArg.toString())
                    .addHeader("Content-Type", "application/octet-stream")
                    .post(uploadBody)
                    .build();

            client.newCall(uploadRequest).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(StudentProfileActivity.this, "CV upload failed", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                        Log.e("DROPBOX_UPLOAD", "Upload error: " + errorBody);
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(StudentProfileActivity.this, "Upload failed", Toast.LENGTH_SHORT).show();
                        });
                        return;
                    }

                    // Log the upload response to verify the path
                    String uploadResponse = response.body().string();
                    Log.d("DROPBOX_UPLOAD", "Upload successful: " + uploadResponse);

                    // Step 2: Generate a temporary direct link using the same filename
                    JSONObject tempLinkBody = new JSONObject();
                    try {
                        tempLinkBody.put("path", fileName);  // Use the same consistent filename
                    } catch (Exception ex) {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(StudentProfileActivity.this, "Failed to build temp link request", Toast.LENGTH_SHORT).show();
                        });
                        return;
                    }

                    Request linkRequest = new Request.Builder()
                            .url("https://api.dropboxapi.com/2/files/get_temporary_link")
                            .addHeader("Authorization", "Bearer " + DROPBOX_ACCESS_TOKEN)
                            .addHeader("Content-Type", "application/json")
                            .post(RequestBody.create(MediaType.parse("application/json"), tempLinkBody.toString()))
                            .build();

                    client.newCall(linkRequest).enqueue(new Callback() {
                        @Override
                        public void onFailure(@NonNull Call call, @NonNull IOException e) {
                            runOnUiThread(() -> {
                                progressDialog.dismiss();
                                Toast.makeText(StudentProfileActivity.this, "Failed to get link", Toast.LENGTH_SHORT).show();
                            });
                        }

                        @Override
                        public void onResponse(@NonNull Call call, @NonNull Response linkResponse) throws IOException {
                            if (!linkResponse.isSuccessful()) {
                                String error = linkResponse.body() != null ? linkResponse.body().string() : "Unknown error";
                                Log.e("DROPBOX_LINK", "Link generation failed: " + error);
                                runOnUiThread(() -> {
                                    progressDialog.dismiss();
                                    Toast.makeText(StudentProfileActivity.this, "Link error", Toast.LENGTH_SHORT).show();
                                });
                                return;
                            }

                            try {
                                JSONObject json = new JSONObject(linkResponse.body().string());
                                String tempLink = json.getString("link");

                                runOnUiThread(() -> {
                                    progressDialog.dismiss();
                                    // Store both the link and the actual file path for deletion
                                    saveStudentDataWithPath(tempLink, fileName);
                                });

                            } catch (Exception e) {
                                runOnUiThread(() -> {
                                    progressDialog.dismiss();
                                    Toast.makeText(StudentProfileActivity.this, "Failed to parse link", Toast.LENGTH_SHORT).show();
                                });
                            }
                        }
                    });
                }
            });

        } catch (Exception e) {
            progressDialog.dismiss();
            Toast.makeText(this, "Error reading PDF", Toast.LENGTH_SHORT).show();
        }
    }



    private void deleteCvFromDropbox(String dropboxUrl) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Deleting CV...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // First, try to get the stored file path from Firebase
        studentRef.child("cvPath").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String filePath;
                if (snapshot.exists()) {
                    filePath = snapshot.getValue(String.class);
                    Log.d("DROPBOX_DELETE", "Using stored path: " + filePath);
                } else {
                    // Fallback to default naming convention
                    filePath = "/cv_" + STUDENT_ID + ".pdf";
                    Log.d("DROPBOX_DELETE", "Using fallback path: " + filePath);
                }

                performDeletion(filePath, progressDialog);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Use fallback path if Firebase read fails
                String fallbackPath = "/cv_" + STUDENT_ID + ".pdf";
                Log.d("DROPBOX_DELETE", "Firebase read failed, using fallback: " + fallbackPath);
                performDeletion(fallbackPath, progressDialog);
            }
        });
    }
    private void performDeletion(String filePath, ProgressDialog progressDialog) {
        OkHttpClient client = new OkHttpClient();

        JSONObject requestJson = new JSONObject();
        try {
            requestJson.put("path", filePath);
        } catch (JSONException e) {
            progressDialog.dismiss();
            Toast.makeText(this, "Error creating delete request", Toast.LENGTH_SHORT).show();
            return;
        }

        RequestBody body = RequestBody.create(
                MediaType.parse("application/json"),
                requestJson.toString()
        );

        Request request = new Request.Builder()
                .url("https://api.dropboxapi.com/2/files/delete_v2")
                .addHeader("Authorization", "Bearer " + DROPBOX_ACCESS_TOKEN)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("DROPBOX_DELETE", "Network failure: " + e.getMessage());
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(StudentProfileActivity.this, "Network error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "No response body";
                Log.d("DROPBOX_DELETE", "Response code: " + response.code() + ", Body: " + responseBody);

                if (response.isSuccessful()) {
                    // Remove from Firebase
                    Map<String, Object> removeUpdates = new HashMap<>();
                    removeUpdates.put("cvUrl", null);
                    removeUpdates.put("cvPath", null);
                    studentRef.updateChildren(removeUpdates);

                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(StudentProfileActivity.this, "CV deleted successfully", Toast.LENGTH_SHORT).show();
                        cvDisplayLayout.setVisibility(View.GONE);
                        cvUrl = null;
                    });
                } else {
                    String userMessage = "Deletion failed";
                    if (response.code() == 409) {
                        // File not found - clean up Firebase anyway
                        Map<String, Object> removeUpdates = new HashMap<>();
                        removeUpdates.put("cvUrl", null);
                        removeUpdates.put("cvPath", null);
                        studentRef.updateChildren(removeUpdates);

                        runOnUiThread(() -> {
                            cvDisplayLayout.setVisibility(View.GONE);
                            cvUrl = null;
                        });
                        userMessage = "File already deleted, cleaned up references";
                    } else if (response.code() == 401) {
                        userMessage = "Access token expired";
                    }

                    final String finalMessage = userMessage;
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(StudentProfileActivity.this, finalMessage, Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }


    private String extractDropboxPathFromUrl(String url) {
        try {
            Uri uri = Uri.parse(url);
            String path = uri.getPath(); // e.g., /s/abc123/CV.pdf
            if (path == null) return null;

            // Convert shared link path to actual file path, depending on how you store it
            // If you uploaded to "/cv_files/CV.pdf" in Dropbox:
            return "/cv_" + STUDENT_ID + ".pdf"; // or extract actual path based on logic
        } catch (Exception e) {
            return null;
        }
    }



    private void initViews() {
        studentName = findViewById(R.id.editFullName);
        university = findViewById(R.id.editUniversity);
        degree = findViewById(R.id.editDegree);
        bio = findViewById(R.id.editBio);
        gradyear = findViewById(R.id.editGraduationYear);
        skills = findViewById(R.id.editSkills);
        email = findViewById(R.id.editEmail);
        phone = findViewById(R.id.editPhone);
        linkedin = findViewById(R.id.editLinkedIn);
        twitter = findViewById(R.id.editTwitter);
        github = findViewById(R.id.editWebsite);
        gpa = findViewById(R.id.editGPA);
        btnSave = findViewById(R.id.btnSaveChanges);
        topAppBar = findViewById(R.id.topAppBar);
        studentPfp = findViewById(R.id.profilePhoto);
        cvDisplayLayout = findViewById(R.id.cvDisplayLayout);
        cvFileName = findViewById(R.id.cvFileName);
        btnDeleteCv = findViewById(R.id.btnDeleteCv);
        uploadBtn = findViewById(R.id.uploadBtn);
        viewBtn = findViewById(R.id.viewBtn);
        generateCvBtn = findViewById(R.id.generateCvBtn);



        studentRef = FirebaseDatabase.getInstance().getReference("users").child(STUDENT_ID);
    }



    // Update your loadCompanyData method to load the stored path as well
    private void loadCompanyData() {
        studentRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(StudentProfileActivity.this, "Student not found", Toast.LENGTH_SHORT).show();
                    return;
                }

                setSafeText(studentName, snapshot.child("name"));
                setSafeText(university, snapshot.child("university"));
                setSafeText(degree, snapshot.child("degree"));
                setSafeText(skills, snapshot.child("skills"));
                setSafeText(bio, snapshot.child("bio"));
                setSafeText(gradyear, snapshot.child("gradyear"));
                setSafeText(email, snapshot.child("email"));
                setSafeText(phone, snapshot.child("phone"));
                setSafeText(linkedin, snapshot.child("linkedin"));
                setSafeText(twitter, snapshot.child("twitter"));
                setSafeText(github, snapshot.child("github"));
                setSafeText(gpa, snapshot.child("gpa"));

                if (snapshot.hasChild("logoUrl")) {
                    String logoUrl = snapshot.child("logoUrl").getValue(String.class);
                    if (logoUrl != null && !logoUrl.isEmpty()) {
                        Glide.with(StudentProfileActivity.this)
                                .load(logoUrl.trim())
                                .placeholder(R.drawable.ic_profile)
                                .error(R.drawable.ic_profile)
                                .into(studentPfp);
                    }
                }

                if (snapshot.hasChild("cvUrl")) {
                    cvUrl = snapshot.child("cvUrl").getValue(String.class);
                    if (cvUrl != null && !cvUrl.isEmpty()) {
                        cvFileName.setText("CV.pdf");
                        cvDisplayLayout.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(StudentProfileActivity.this, "Failed to load data.", Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(StudentProfileActivity.this, "Upload failed", Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(StudentProfileActivity.this, "Error parsing response", Toast.LENGTH_SHORT).show();
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
        updates.put("name", studentName.getText().toString().trim());
        updates.put("bio", bio.getText().toString().trim());
        updates.put("degree", degree.getText().toString().trim());
        updates.put("github", github.getText().toString().trim());
        updates.put("gpa", gpa.getText().toString().trim());
        updates.put("gradyear", gradyear.getText().toString().trim());
        updates.put("email", email.getText().toString().trim());
        updates.put("phone", phone.getText().toString().trim());
        updates.put("university", university.getText().toString().trim());
        updates.put("linkedin", linkedin.getText().toString().trim());
        updates.put("twitter", twitter.getText().toString().trim());
        updates.put("skills", skills.getText().toString().trim());

        if (logoUrl != null) updates.put("logoUrl", logoUrl);

        studentRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Profile & image updated", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show());
    }

}