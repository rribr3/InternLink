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
    private EditText editSkills;


    private static final String IMGBB_API_KEY = "93a9e7c9a933826963d704e128929b30";
    private static final String DROPBOX_ACCESS_TOKEN = "sl.u.AF0fzLQa6oq9hMVAGR2MwHrFm6u9USwb3fxWZyfMvmBa63qgEDXUORuy5pYaCPu7MrYtzTogMmaliwTgfG8bUv9eA8U9aIVXMFUhlKNdu7s7BdVEAA8tnSA02MsVakfRLJYTA6xaQlCTiPbiJlHwuOgVNPiUx1OEbqRctZxp3SvAmzxBGzirgMl8oTF1GMDbwum7x1dMOTVqTvPzr3ENqNPtLPnal0qgcu3VTuR8m-F_vKC2nlac40Ebl_E95wMwrz0PzfyO7QGDcfBhCzFNzic8EMLfzkKSL444dHOuneJQuEq4mhSzG_pjI7Z8QyJ8oECn-z5D3ZoULTv3m0CXhjixjfKweIdawQfxbE56jLihjSC6IjD43KONK2kUUOwXLYXmPasoBn8jR3WYxr3qSb9HgslXJVdq_-ApBhFThkiPd0gYJDL8cy8e5eiZKb5IRjASzj9lVX8uDFQBhiGAieqB0ILX4R_mUXqFxuXrvglJwl7S6TjtSp9b9mFutSdCNE12t0RVaUmDIN5OWyU2D6LM5p0HxsjfQwy6t5fWTAVFo8UEaD54LEfVucEMtrDI-rAiKrTghzToqHc1XpyNhOPMfxMz5i6NzCYsiEmTZqUHGrTosfZbNAOQa7Z4zD2uH4F2Fo2jIiPuFIbwsjhco8u4LqgHmH6u69RZBEjY61g5I1nfPE4JDowQbxm1R0YOqdpXzm9zVerayI8CjfGxOE183DoBM6t0T8yY0AdQm4G_ldzMShrZR7cRdNf-Psa3oqlTHjm8lM_CYeNkBC2Qd4HS39QhaR2jBB8_6tUns31wQRvBbFDVNXXgYeVHtVUovVL_7FVd7BJfT-suQiTHiIckRsVgUeUvfE0p7MkvJGfegLsZp_N4V6Y0ltz_JvMbWSkBF6c9MTSxisPzBmjmVIYUYGskGOPFfdLEhYecJbdidNR7Q_xzwgVPnB3oFWrxPRFLpve2ajWL75Zm0qYKk51pUiyqyDa0B-me9DYHDTAwrVguT9GSjBoOxqsWKjc-Yyv3nScUCP95YhPufudxz1rhdfx5aj9DczhFEYa12GmXeJoSatZFn8I_WJWPSfJpvOfc16ubkcQiqeu4SEuJgSb_8cK-ocSY2kLMbbseJey97N5z0tVGIOQ5FxMiA69w3BWfIlKHBrXOR5vGFRmO-xQr9cgXOgpu1k_0WHL8qjGhB9yujRUTnQ5MhJ2NLRsRnr6qPQAynP6Fvs7_8bc4FjkgCxe9aBskZslZYkvgYtw0T45Zxtpl76w74VQPdM93TI4TsNom3GjR6fMu9Ntu48rUJv0lU5nvO-3O36sGsNDHpICi26mniEPKVvMx8xdDg_sLpcf3bwrGlTpDF2Kr2CVmpOSXbtrwFZzNkOtlBylOgOs-gZJaYGuc7TOQUhVtGUI6Qx2KeIxX7FdpJofatKY8Iz95NOHlObmuJHOVy-0DHw";
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
        editSkills = findViewById(R.id.editSkills);
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
        companyData.put("skills", editSkills.getText().toString().trim());
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
