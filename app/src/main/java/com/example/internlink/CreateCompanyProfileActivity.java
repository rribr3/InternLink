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

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
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

public class CreateCompanyProfileActivity extends AppCompatActivity {

    private String userId, name, email, password;
    private static final String IMGBB_API_KEY = "93a9e7c9a933826963d704e128929b30";
    private static final String DROPBOX_ACCESS_TOKEN = "sl.u.AFxwei-bdH12l4QAQSN2wi9wx7yUHACctqI35BnLi3yb9mHMqEIwqpUGS5v1UPECqZF1LYyVyOEojkX631ocMI54g1o3qTOC_a6FjGuQBSLaWhPrq63xqTCH9bK1MsGoa7oEYuKlS34YzlQct0eWxKZtttVrqNPHywM4w0GcoXoDlln9MtFI4DkhgetFzgNqD3qqOGCn9w9Xhq0CYVjSk9RAuyHwnbeIwHWevLT8Zpl4rhaBCuuxT4P9DvOMo-gsmjniuKxvMNA73Ame0Pru1VM1FbELACe1WaGn7c08fVasEgN2hjscfnGF9JKhTcJKwReXHA9BFyZ61tnuP8NEy7QmkdrjLrlKrgSAW8LgS-0ZkxjCtA_mY1-BxLvTMu00H--n-cM07nhh5Nm1Q6GciXeSLs1kdxCr7rloOiWfX-gJ24h8ZFAcnKfOOFuZ0ghFm5FRqbkMxN43b_-cJq0DgrpfEwxC2foT-4nyHm0AZ9cp64F46kDFLP4inyb29k4y8MKl1YRyyM3qLN0emtzT3STTF6rVXrljfPSIY70JPxtAN-T9--8TAPQMWfjlUDOsQvhSq8ujL--ThZTyQ4N66OqbPBtwF4bC2-3foE13pK-g1iOumfP5IjcEVmtnHXJlnrmPUVo7PhXnQrK0o2A93nCUiboWDhgnrrbuJS2G_o-dZV9tx-6Tf797p9XrfNE19sNsQNm1HHY_h-xr-jlw6YM65PNqMRHhPzmsv5ax3Nt7C4-qcfpEv8NadolxevJ626ZLXIfVGmH8nGFGo9n2FC3fXEe42MudmFtYBOVC8f5KhP7XKyfbXIVi3Pzg11AgYUifmm_AILmz6DMHQKqmade0TDBx2o6fSfVL0CgBAMnEtOfmgrJfM4DiC1qvDg_9FIAPMC1EpDX5MVRSTu_40Mn2_OmyKLD6Lwly_BOJSE_NpPw949nxszzvxLv9JNmQC85yJJCL3qGljSCvb8JbllZQ_jnQtkhbA2b_wz_3_KG3rbHI9_tHd6_s7jd4OvsY3bwGUNtqBhhyXLbHTpLZIp1yBUMzgQHrUEZjsVg6Wy1T9GlykPgtjsTsQM8HvMoDywssAkM9M4k9X0QMMLdyPiqcfkZFVMaO9-pN1EQQaMZmVyRX05zftjFwyHq9gpVBfqMyCWipSznnqJJH0yaYUpUYn9T26lxtjOuPLRyYSQ2r7Lyh6wMF6YJd_PBI24glPGoMBeC0eGAv6kKFCu2vgSmzUEjvNmJczpy02aqTwdkJ6_F47LEb3cvAeeEVPeHAKF2lJ3a4KLhvS_seMfZYDq8qTrx-amE6J2CpiR6cqxT0PaObnY6d9NNxc_8mmn0hN9dcywIYJTS7aBNtxHJ_XSFHHFGCYHnIbUypOSxgjNWTxW1o1o461qxrnuEu4KdSI1C2wpCXoouiUTpg4MYlSpwE8RatJAjrcbLJLL5ZVXSgtA";
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
