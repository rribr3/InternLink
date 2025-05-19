package com.example.internlink;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CvGenerated extends AppCompatActivity {

    private EditText firstName, lastName, email, phoneNumber, address, linkedin, profileDescription, university1, degree1,
            educationStartYear1, educationEndYear1, skills, jobTitle1, company1, jobLocation1, jobStartDate1, jobEndDate1,
            jobDescription1, projectName1, projectRole1, projectDescription1, certificationName1, certificationOrg1, certificationYear1;

    private Button btnGenerate;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cv_generated);

        initViews();

        btnGenerate.setOnClickListener(v -> {
            JSONObject profileJson = collectFormData();
            generateCVFromCohere(profileJson);
        });
    }

    private void initViews() {
        firstName = findViewById(R.id.firstName);
        lastName = findViewById(R.id.lastName);
        email = findViewById(R.id.email);
        phoneNumber = findViewById(R.id.phoneNumber);
        address = findViewById(R.id.address);
        linkedin = findViewById(R.id.linkedin);
        profileDescription = findViewById(R.id.profileDescription);
        university1 = findViewById(R.id.university1);
        degree1 = findViewById(R.id.degree1);
        educationStartYear1 = findViewById(R.id.educationStartYear1);
        educationEndYear1 = findViewById(R.id.educationEndYear1);
        skills = findViewById(R.id.skills);
        jobTitle1 = findViewById(R.id.jobTitle1);
        company1 = findViewById(R.id.company1);
        jobLocation1 = findViewById(R.id.jobLocation1);
        jobStartDate1 = findViewById(R.id.jobStartDate1);
        jobEndDate1 = findViewById(R.id.jobEndDate1);
        jobDescription1 = findViewById(R.id.jobDescription1);
        projectName1 = findViewById(R.id.projectName1);
        projectRole1 = findViewById(R.id.projectRole1);
        projectDescription1 = findViewById(R.id.projectDescription1);
        certificationName1 = findViewById(R.id.certificationName1);
        certificationOrg1 = findViewById(R.id.certificationOrg1);
        certificationYear1 = findViewById(R.id.certificationYear1);
        btnGenerate = findViewById(R.id.generateBtn);
    }

    private JSONObject collectFormData() {
        JSONObject json = new JSONObject();
        try {
            json.put("first_name", firstName.getText().toString());
            json.put("last_name", lastName.getText().toString());
            json.put("email", email.getText().toString());
            json.put("phone", phoneNumber.getText().toString());
            json.put("address", address.getText().toString());
            json.put("linkedin", linkedin.getText().toString());
            json.put("profile_description", profileDescription.getText().toString());
            json.put("university1", university1.getText().toString());
            json.put("degree1", degree1.getText().toString());
            json.put("educationStartYear1", educationStartYear1.getText().toString());
            json.put("educationEndYear1", educationEndYear1.getText().toString());
            json.put("skills", skills.getText().toString());
            json.put("jobTitle1", jobTitle1.getText().toString());
            json.put("company1", company1.getText().toString());
            json.put("jobLocation1", jobLocation1.getText().toString());
            json.put("jobStartDate1", jobStartDate1.getText().toString());
            json.put("jobEndDate1", jobEndDate1.getText().toString());
            json.put("jobDescription1", jobDescription1.getText().toString());
            json.put("projectName1", projectName1.getText().toString());
            json.put("projectRole1", projectRole1.getText().toString());
            json.put("projectDescription1", projectDescription1.getText().toString());
            json.put("certificationName1", certificationName1.getText().toString());
            json.put("certificationOrg1", certificationOrg1.getText().toString());
            json.put("certificationYear1", certificationYear1.getText().toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    private void generateCVFromCohere(JSONObject profileData) {
        progressDialog = ProgressDialog.show(this, "Generating CV", "Please wait...", true);

        OkHttpClient client = new OkHttpClient();

        RequestBody requestBody = RequestBody.create(
                MediaType.parse("application/json"),
                "{\"profile\":" + profileData.toString() + "}"
        );

        Request request = new Request.Builder()
                .url("http://10.10.94.69:3000/generate-cv")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(CvGenerated.this, "Failed to connect to server", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                runOnUiThread(() -> progressDialog.dismiss());

                if (response.isSuccessful()) {
                    try {
                        JSONObject responseJson = new JSONObject(response.body().string());
                        String generatedCvJson = responseJson.getString("cv");

                        runOnUiThread(() -> {
                            // 🔧 NEXT STEP: generate PDF from this string
                            generatePdfFromText(generatedCvJson);
                        });

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(CvGenerated.this, "Server error", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void generatePdfFromText(String cvText) {
        // 👇 Here you’ll implement Android PDF generation
        Toast.makeText(this, "PDF generation logic coming next", Toast.LENGTH_SHORT).show();
        // You can use PdfDocument or any third-party library like iText or PDFBox if needed.
    }
}
