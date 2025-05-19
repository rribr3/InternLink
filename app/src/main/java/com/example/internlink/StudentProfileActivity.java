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
    private static final String DROPBOX_ACCESS_TOKEN = "sl.u.AFtRqxwJ5W4zlY2rH1YInIDJ1FDVtdMMZrhBUZTJK71WrkPNqIja4r9jVUsXrIizLZQgPqNV3kh6_1YWSLeMhhUD5Xc8DWFpQE-TEAo3FJAPc52U6fWExzUVPRqP0fV7EPtL8ywK1PS747S0JrHYrWWwAHRO3SzjpIslgtmkXrJ6E0lFsyxlK6LOtKcINn-KdMa5gIlyjih4Bn3SaHfn3usKrIu0HIXCiEhiFy3TyqShuHCCCWEfUV5f2t5NshRML_zIOJq2KRe1NEmc8r6McjxbHiiGbcFvi0Rij9EJWbvHcN3HyV22Bg5vYANuRrDlk6OCIXqyoKFblXgIoiGIgF24Ky0m0x87mR-BYVNQZXTH8TzhFoUtmw-EMnhIBP2Ua3ldPFKCn7LG2JS7w0X6Lrsojp6s1NfyE720c1joFLeXzg9INAP-dv3k5soLVuGwIY8a3XifRBzNy5DyqF97bQW6NeP0vnRhcJgdVuY7nDwPTZu1sgasZtkiBFaAeDXMsbPs8ds0mp6tIUVSR4yhzFF5_b4kMMmq59KN5pc1Yde3gkowX7WthP7QT2JWAn32mM2taqhGMeylskXhscOH5EIAYRpoEPKyWmeeicWdQ9d1cNbtkJNF3jIQGFyVsKOwMz3fqyux4pfLe2Yw7nuNntfA2qIypHupga9DJwe9jc5FakyrUGzCTsY-bgRygrAH2eOqgviq078KGt6CRp4ugnHYzJb-PtWYHwO3iCfGsx-vjzL-pwZJeGAHUyWnLoSDPScGCFvVicx0HkEhE93sgLepMtoT6NAHy58sgS-UT5BnQDCFe0p8Xpo7IswxldDNIlyD6hGMKPj4Qe0pRaregamKtwGTHFBmv0nPqdlv8H3Qs6JAkv3_0jGxhVjCVEqxJIj0ovC36ShjSa-KQJz6MDK5ZeL6eDSvvgiDjVV2ee4SVbtgQl-A6PukHOo4PhwH3XDYoj3iWSUhtVQD9bEcPvi2qVosB8ybv1Ixs3voAuWdnxCFJcCoeasArwETVAQuY97vd2xXCKNyfW-BbkKfl657xC0q1qQQPeTIk1cSlPkunjFiUzCs83wKzNUnGg33DgmrsYN64Q9O2sDjjeRCYlutg7V7fGm5GH10v7kyAKbjsf89RoGqnkPvf8j8DCpYc8XgJJJG_mwNTLS366RalNpNnlQia34QqzT7JxxcrNLxBqn-eYpZHpWLHFqxAqoY-F4UpaLiCwRcssT1ocztp_J_23pnc8j6VdRJRFDJMiji2V1c8biLBUnjDzI5okJiO9o9zNqmCCjHGEcNrX5u3iPwvOzlztYYZvLfee0C0WXmKXhF-5pFGci7aq4s8Pi27smZ2u0j9Kc2XaUJRStstTXMg1ZyfleGJUpkeNpQH872P5cVUgVjEx8kmT8uU6ESDE_5UjU4-adNpemEKmranhsCcTLxN8IUm_TuakLoVixvW5CmCwAG7UIXIiFYngRuYQw";
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