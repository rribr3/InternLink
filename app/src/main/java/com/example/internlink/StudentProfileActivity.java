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
    private static final String DROPBOX_ACCESS_TOKEN = "sl.u.AFuJmkxv7Ly7RgEOJw1_vCjOkAMvgv32vXu6W_n3LBzsNhzJdGrJNqappNkVvSGxT6wD6kG5nT29PYOAuknN6FJVFVliW6Yx2JfyTDP3zru8xRUNPUNRj-jVSUAvilJG17JWbXjKjVqc2-bXrUaQayYlww2GFAXWFYeQMR2URtUahzzmuBokdXzkhcPvPbbC-REd_VxAdci0Bvi_sR1iSUnCzCEuH2RiMY6YWbUbMkqZ-9Jzl7yogRWyPP_YNsgFm28tPLLyrpbjl3ByXeWwhdhJhodHj4in1uoJfe7z1tSdxaKOJE-timK2L9yHbv-ITw53ZW-uectkq7zF7Db4f6SZRE16yl0Zbjwfat4IuhAfL7KmJ8ZEqblXXHgNV-4n9GQYjRk7b-d0XlXCx-jHpelOB-BqlBEqAXL4gEKvNV9pba9AoGIGX4gM5WW_BkasdFM6bp1YXo_sE8ZgHW9mCVvMSf5oZOVwjEjbpLQO8PFlh1NYkCzKiNsmaHzXQorncYghzAdCIxfhZO7vRWLsKL_c2uHuIN-FYiGVeTUNPH7Ow3tVyV3l7x1togK6fF930_wWMcmP_PVMMEED0yz8b3Jbtexv_KcLBQhIc5SzyPdii2RaNSPhVYk-XcrKX3K9Vmflz1k-mIL7k62aebkuypKgP3ufXSML04S2dTwMd1-BwmZqXwxrefb6AVoJO6jgJXRf6QepT7l0J7rfAWxkGapzao6rkQzIMTNh_jN7Tme107W35cWFPUvuOZF76GgeuvphyRxHbhoeFOuYLvx-slX0kMs6Lv8A9DS5ZGrHFkhcAt6256bc3Ra26rbaHjSmMxO4KjaAWpJRYYvQeukMlRibthSV4FfsNErJXVSbBOj2nR8dghfOi8p7PpjqDSoqJ7edjoreB9KPEp6Y1iJNjkJbL5L7HUmMWmTtgT9TFJ5kiCNCRX7koVo-y64io5pUMY2Zx05S88IeQtycn9gKlF7vdZ19Ad8g9IpZXo6Z2gaxYBQ-woIlxqEw6Fp5IuYcqi5tk7jZBK2RqTHGRWx6DjgrVd4r6SiD0uKyyK5grfDdTi2jAeB0htbCpAQSAtGL8g6Tmmw-MMO0u7RlfWzQou9jopMbfYYKAa8u0SFWkTt426ckoKGCHiYNNJvMgtSJfnG7oJkowx8cXSBpm1LmMbAutIVFRDoW28Co4f6ycrozfGw2cTSa5u96VXI3zcubQw1FNVmYZ3-LVf1d_uUQFotimhAjKPC9aX7dm-nieT8063bvB6SSAoIYBga37somX4JSnArg1o34RhfspCA2ZfkP2O0KSvs_akICpAb-Vxm794JzRQcqDC0CQsKYiQhu8B-HkU3bc2gGvTohCWU4G3Z3kb4rrveOnEa_LURWhEIxig1v5QpXE3WWA5xFLMlOoVacpun0ChT75FXqw3eq_mbLLPonOtZrgpQFL6TZiVtjEnrCtbtH2VRzluS6TVK0dpE";

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
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(cvUrl));
                startActivity(browserIntent);

            } else {
                Toast.makeText(this, "No CV uploaded", Toast.LENGTH_SHORT).show();
            }
        });



    }
    private void saveStudentData(String cvUrl) {
        Map<String, Object> updates = new HashMap<>();
        if (cvUrl != null) updates.put("cvUrl", cvUrl);

        studentRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Saved cv successfully", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show());
    }

    private void uploadPdfToDropbox(Uri pdfUri) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Uploading CV...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        try {
            InputStream inputStream = getContentResolver().openInputStream(pdfUri);
            byte[] pdfData = getBytes(inputStream);

            OkHttpClient client = new OkHttpClient();

            RequestBody body = RequestBody.create(MediaType.parse("application/octet-stream"), pdfData);
            Request request = new Request.Builder()
                    .url("https://content.dropboxapi.com/2/files/upload")
                    .addHeader("Authorization", "Bearer " + DROPBOX_ACCESS_TOKEN)
                    .addHeader("Dropbox-API-Arg", "{\"path\": \"/cv_" + STUDENT_ID + ".pdf\",\"mode\": \"overwrite\"}")
                    .addHeader("Content-Type", "application/octet-stream")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(StudentProfileActivity.this, "CV upload failed", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful()) {
                        // Step 2: Create a shared link
                        String createLinkJson = "{\"path\": \"/cv_" + STUDENT_ID + ".pdf\", \"settings\": {\"requested_visibility\": \"public\"}}";

                        RequestBody linkBody = RequestBody.create(
                                MediaType.parse("application/json"), createLinkJson);

                        Request linkRequest = new Request.Builder()
                                .url("https://api.dropboxapi.com/2/sharing/create_shared_link_with_settings")
                                .addHeader("Authorization", "Bearer " + DROPBOX_ACCESS_TOKEN)
                                .addHeader("Content-Type", "application/json")
                                .post(linkBody)
                                .build();

                        client.newCall(linkRequest).enqueue(new Callback() {
                            @Override
                            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                                runOnUiThread(() -> {
                                    progressDialog.dismiss();
                                    Toast.makeText(StudentProfileActivity.this, "Failed to generate Dropbox link", Toast.LENGTH_SHORT).show();
                                });
                            }

                            @Override
                            public void onResponse(@NonNull Call call, @NonNull Response linkResponse) throws IOException {
                                String json = linkResponse.body().string();
                                try {
                                    JSONObject obj = new JSONObject(json);
                                    String url;

                                    // NEW structure (Dropbox API v2 response)
                                    if (obj.has("url")) {
                                        url = obj.getString("url");
                                    } else if (obj.has("result")) {
                                        url = obj.getJSONObject("result").getString("url");
                                    } else {
                                        throw new Exception("No url field found");
                                    }

                                    url = url.replace("?dl=0", "?raw=1");

                                    String finalUrl = url;
                                    runOnUiThread(() -> {
                                        progressDialog.dismiss();
                                        saveStudentData(finalUrl);
                                    });
                                } catch (Exception e) {
                                    runOnUiThread(() -> {
                                        progressDialog.dismiss();
                                        Toast.makeText(StudentProfileActivity.this, "Link parse error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                                }
                            }

                        });
                    } else {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(StudentProfileActivity.this, "CV upload failed", Toast.LENGTH_SHORT).show();
                        });
                    }
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

        // Extract Dropbox path from URL
        String dropboxPath = extractDropboxPathFromUrl(dropboxUrl);
        if (dropboxPath == null) {
            progressDialog.dismiss();
            Toast.makeText(this, "Invalid Dropbox path", Toast.LENGTH_SHORT).show();
            return;
        }

        OkHttpClient client = new OkHttpClient();
        String accessToken = "sl.u.AFtqXbDKKz-x3kbcl5CsMY4h7R_NAuB1i9wBdD_fFlVSVL0w5nv8rJqYCCWvXjZCNBQ0Elu1pKfiNe6ei_wJ_5foFt-Wven4kXD3E5QdP67-Ag3u-36FozLKuZFnQeHqfyVQ8UhhbWw5Eb4o3mJt79-Y9u01uHVYDVT6niLJlyurdMMrqqa-C_PHqv8QGSN5GvebzPchUr11HPf4EObi06-7acbs1HXWBY8FTmLOG8NQ8OycvAbAQzlugB1JyTZc2raBGSRt5-lGevzT4smdA0fnHC2g_2hLcywgw7b5c9U7777-m2VBHk7CW7nstltwo6o7RFyBq7Q9eO8mgZYLoPJgyin2N7_PpiwHZ-Nb8sRdp4ldJhUeiCJ3zOwYVdU_QBKswK7Pfgn1ozb3WyOK7crkP5DKmG3kncOuNapqyh_Lyy1X5E4xp5R9G3bV8E309uJmxVsifw2Wm2mkU5ZVUH-2beMSrFeaiWqduhqxJOZOd19Sa95xQaIrtBA5ey80b7Sv2Jvc-e0QjhUZD-Bsq0xawpI6NPcFV7JLMbreMPin1AGUxQ7TRjf2vBOOPDcb7KZ-H8Rotl5o2ZoWFiYZf_-EqShmHEkxc74jL-1FRo2rL0xKaxj8R_G5KN3fnF9u0lB69nxb7mzcz2zjheFca4G7pc6GNcFkZoyDWrSG0ODHVRhLjh0NjcVRyxgrqXyQ_ALpeQcSJo6hg9FSNF7srGsKLwnAjsu1XRe3wKo1ggH8EtXw51N3fDV4QeAcTs9po22nYO68uhHGx2fRA-e8Lpqk_qydx2CHJio8MfwDOrxMb5xgxFzkQN0t1s2KYraZUDeNcFCXjLCS1KYFuGpQpFCr74m4xKsS4rh3e_qtMmcFceG35RyzTFV2AeoqkthwjsFzg2DU6hSg4lV9YJFkK36VVEHxLz4YWg815VEW8c9bedlhpqi7rGT1e4F2qkzRLwHe8nwcBryNFJ3IoXuy70AO9YLwTQkow-KUigu-kqvi3YeVbX8eq4lI_myBrMuOyYAfDzkp6I66X7Kt1WVsOkwEOHPskpixSXDQsgO_sQ9H2TzgFDxnnWAVKHLrUa8G6Udk6G_rmq9SIWYxGpOHlmZZMntDiauVrxlIiIkAkNZcML1naUXEDl5YVTWIzZTrsOTtrkCjX65Dd11rLAIbY1sZAuSQdxThT9wn8HyhzuGpXtT487ffDtDJd0qPz6xmWHVtOitGCKazAIs1u5Uz3KF9VCCAEW9H5mU2AxAPNBNfrs96rWVnTq755P-13UwcKkimSRwh5aU8yU0Tv55-GpogaYXNu5-t941iOU5tGSKlWKMDeVlKyglIPRw95vIAnVJ7tKIiUxr37w519eMQ-HRuUy-duGphDR5NJ717RnMpu1l-Du8ZnkFX9YQS_KOeiOGUaAG-KvqN6a855xyTfIaBH2TvqtqjpyJvl8eYXPtf1w"; // <-- Replace with your token

        RequestBody body = RequestBody.create(
                MediaType.parse("application/json"),
                "{\"path\": \"" + dropboxPath + "\"}"
        );

        Request request = new Request.Builder()
                .url("https://api.dropboxapi.com/2/files/delete_v2")
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(StudentProfileActivity.this, "Failed to delete CV", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    // Remove from Firebase
                    studentRef.child("cvUrl").removeValue();

                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(StudentProfileActivity.this, "CV deleted", Toast.LENGTH_SHORT).show();
                        cvDisplayLayout.setVisibility(View.GONE);
                        cvUrl = null;
                    });
                } else {
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(StudentProfileActivity.this, "Dropbox deletion failed", Toast.LENGTH_SHORT).show();
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


        studentRef = FirebaseDatabase.getInstance().getReference("users").child(STUDENT_ID);
    }

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
                        Uri uri = Uri.parse(cvUrl);
                        String fileName = uri.getLastPathSegment();
                        cvFileName.setText(fileName != null ? fileName : "CV.pdf");
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
