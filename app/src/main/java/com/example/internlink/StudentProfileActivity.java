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
    private static final String DROPBOX_ACCESS_TOKEN = "sl.u.AFwCy7V8cHUsYrguu9QzH4ltfyhyTn2bQ6wSzTOc6sF-ay1pTEKWxXvc5vb7hPkoC6boFkqomrr8TrUkKPPAGJ_oJ3Kj1ouxHmiZDEqVxgp8gxyu_N2UtXehXMhFhRNMWAQhKQkyKiE-r55x_rPBCPtgrPC5m83jX4ApPr7-srWbibVvKyuSE4VWyrMOJZOkWBdX8dA4zdvdHbUOqrzJblbwKNyHZQ35pZ4oJqVN3VNsFw0FB3dT-Yn4Ds2mahP5Ab7r1XpQcF2XBKE0j4Q61AGtc7NA4iaBlH2IUdwSb1K4AQXuvJ2wsZnIcL-fBK29CLaMYbm3am2J7xKt6QGKIdIV7ovUBPBkBgjaBwvSff87krs7wRjdPDTS2e-Y7gAYCAsll_S72CiVvmnG1N-57arudjaqIZ60Z1NmcbBjpTGIuN6IBsF9LH8uepuGhtvFEzId2hMGWze21tDYqmrS73aaKtux_bK7QAfd5F5VL_Zwn754XJxp1hQe1gXc4jbO3HKAjDrxCbeB5YWYitdFo57gBq21lRnbQ6cVEZJmrzSMrTmi7O_PzStHfV5Y9LLqGQk_djneQeIndh21LhwGAvHhq5AkZ44xqLuWpmQAS39gnuF-36P-pYfxy13HtZJKg0CeT9ltCep1DcRaw3a7MN3obuY3PKUYa_jfivvw5z9PX8O1qln_-lekyojpWXCV-JSG1f_GrXqSknYAIV145tOSp4QptG1_4dx0yfa8Hrl_SqHKL6wPZSiFt1Zz64iacziNia-71EcBd5Rbob2Mpjhwa31b4G4DvikS_u8qSvK0cnd2_hzJQDOfCWdi771QVRY61tleZT26_kBxXoAN-nMy4ssgQeuU9_E4hRWVZo8ysYblvgkP7JV5RU87RGOubutR1qrYtXg649_pIkp19OnnhgMWkxthA-fR0FngRCIkRmnvkJzuujdV5H419hXMrLHOVso-fr87NPw0PdeHSEtlkCaiz88au1uEz9WY4V26C2SxiElMZMPOnJxjlHdc_iI-ni9O239keG0LXGmGQg0UTmAytriDkHjfYbHJCULaJ72RWq24gQ5SFKwJn-D99dhBR35GqizR6Jxff3l_ub0cgg-4Pk3XjKM312JSAsl9cZ48pXutJvjfthvHXZKMLIWzYll-_5WlyAbW2IqzfpYnRisoYcIyr-SSFtz4IsskL9-WTn6Fw1caoGUQoXLAAHEbw8NxYBAV41-HR7lfiKqfkSjIMzxbS-l04oevW7fxeUtnylHZ85rTKRuL3zG84xHB9zEc12gnXFJ2k5DcQSFI_jIRlyofk_fy_UpazbJOA7M-vwIqOBQSOwqa3faTG8KGDotzfITScXSzQ3Ddzqngdj1GWXi7djJFVBY510PNZ8-kj1D3ZWe42XVMk2Eu8081JY13_Xm7jenT_4f8Eop80al5kupl6aI8BBAl7Ay2TsMLMflwE3DnF10AStoy668";

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
                Intent intent = new Intent(StudentProfileActivity.this, PdfViewerActivity.class);
                intent.putExtra("pdf_url", cvUrl);
                startActivity(intent);
            } else {
                Toast.makeText(this, "No CV uploaded", Toast.LENGTH_SHORT).show();
            }
        });
        generateCvBtn.setOnClickListener(v -> {
                Intent intent = new Intent(StudentProfileActivity.this, CvGenerated.class);
                startActivity(intent);
        });




    }
    private void saveStudentData(String tempLink) {
        Map<String, Object> updates = new HashMap<>();
        if (tempLink != null) {
            updates.put("cvUrl", tempLink);
            this.cvUrl = tempLink; // ✅ This line is essential
            cvFileName.setText(Uri.parse(tempLink).getLastPathSegment());
            cvDisplayLayout.setVisibility(View.VISIBLE);
        }

        studentRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Saved CV successfully", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show());
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

            // Step 1: Upload the PDF to Dropbox
            JSONObject dropboxArg = new JSONObject();
            dropboxArg.put("path", "/cv_" + STUDENT_ID + ".pdf");
            dropboxArg.put("mode", "overwrite");
            dropboxArg.put("autorename", false);
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

                    // Step 2: Generate a temporary direct link
                    JSONObject tempLinkBody = new JSONObject();
                    try {
                        tempLinkBody.put("path", "/cv_" + STUDENT_ID + ".pdf");
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
                                    saveStudentData(tempLink); // Save direct access link
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

        // We now always know the file path: "/cv_<STUDENT_ID>.pdf"
        String dropboxPath = "/cv_" + STUDENT_ID + ".pdf";

        OkHttpClient client = new OkHttpClient();

        RequestBody body = RequestBody.create(
                MediaType.parse("application/json"),
                "{\"path\": \"" + dropboxPath + "\"}"
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
                        Toast.makeText(StudentProfileActivity.this, "CV deleted successfully", Toast.LENGTH_SHORT).show();
                        cvDisplayLayout.setVisibility(View.GONE);
                        cvUrl = null;
                    });
                } else {
                    String errorMsg = response.body() != null ? response.body().string() : "Unknown error";
                    Log.e("DROPBOX_DELETE", "Error: " + errorMsg);
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
        generateCvBtn = findViewById(R.id.generateCvBtn);



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