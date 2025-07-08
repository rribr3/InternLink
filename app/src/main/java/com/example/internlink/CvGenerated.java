package com.example.internlink;

import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;

import com.google.android.material.appbar.MaterialToolbar;

public class CvGenerated extends AppCompatActivity {


    private EditText firstName, lastName, email, phoneNumber, address, linkedin, profileDescription, university1, degree1,
            educationStartYear1, educationEndYear1, skills, jobTitle1, company1, jobLocation1, jobStartDate1, jobEndDate1,
            jobDescription1, projectName1, projectRole1, projectDescription1, certificationName1, certificationOrg1, certificationYear1, language1, language2;

    private Spinner proficiency1, proficiency2;

    private Button btnGenerate;
    private ProgressDialog progressDialog;
    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cv_generated);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set up navigation (back button) click listener
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

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
        language1 = findViewById(R.id.language1);
        language2 = findViewById(R.id.language2);
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
            json.put("language1", ((EditText) findViewById(R.id.language1)).getText().toString());
            json.put("proficiency1", ((Spinner) findViewById(R.id.proficiency1)).getSelectedItem().toString());
            json.put("language2", ((EditText) findViewById(R.id.language2)).getText().toString());
            json.put("proficiency2", ((Spinner) findViewById(R.id.proficiency2)).getSelectedItem().toString());
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

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();


        RequestBody requestBody = RequestBody.create(
                MediaType.parse("application/json"),
                "{\"profile\":" + profileData.toString() + "}"
        );

        Request request = new Request.Builder()
                .url("https://4ba97b69c6b6.ngrok-free.app/generate-cv")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(CvGenerated.this, "Failed to connect: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
        PdfDocument document = new PdfDocument();
        Paint paint = new Paint();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create(); // A4 size
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        int x = 40;
        int y = 60;

        paint.setAntiAlias(true);
        paint.setColor(android.graphics.Color.BLACK);
        paint.setTextSize(14f);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));

        // Break long text into lines that fit the width
        int maxWidth = pageInfo.getPageWidth() - 80;
        String[] lines = cvText.split("\n");

        for (String line : lines) {
            if (line.trim().isEmpty()) {
                y += 20; // Add space between sections
                continue;
            }

            // Bold headings
            if (line.trim().toUpperCase().equals(line.trim()) && line.length() < 40) {
                paint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
                paint.setTextSize(16f);
            } else {
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
                paint.setTextSize(14f);
            }

            // Wrap text
            List<String> wrappedLines = breakTextIntoLines(line, paint, maxWidth);
            for (String wrappedLine : wrappedLines) {
                canvas.drawText(wrappedLine, x, y, paint);
                y += 20;

                if (y > pageInfo.getPageHeight() - 60) {
                    document.finishPage(page);
                    pageInfo = new PdfDocument.PageInfo.Builder(595, 842, document.getPages().size() + 1).create();
                    page = document.startPage(pageInfo);
                    canvas = page.getCanvas();
                    y = 60;
                }
            }
        }

        document.finishPage(page);

        // Save to file
        try {
            File pdfDir = new File(getExternalFilesDir(null), "pdfs");
            if (!pdfDir.exists()) pdfDir.mkdirs();

            File file = new File(pdfDir, "GeneratedCV.pdf");
            FileOutputStream out = new FileOutputStream(file);
            document.writeTo(out);
            document.close();
            out.close();

            Toast.makeText(this, "CV PDF saved at " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();

            // Optionally open the PDF
            Uri pdfUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(pdfUri, "application/pdf");
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Intent chooser = Intent.createChooser(intent, "Open PDF");
            startActivity(chooser);


        } catch (Exception e) {
            Toast.makeText(this, "Error generating PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    private List<String> breakTextIntoLines(String text, Paint paint, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder lineBuilder = new StringBuilder();

        for (String word : words) {
            if (paint.measureText(lineBuilder + word + " ") < maxWidth) {
                lineBuilder.append(word).append(" ");
            } else {
                lines.add(lineBuilder.toString().trim());
                lineBuilder = new StringBuilder(word).append(" ");
            }
        }
        if (!lineBuilder.toString().isEmpty()) {
            lines.add(lineBuilder.toString().trim());
        }
        return lines;
    }


    private void openPdf(File file) {
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/pdf");
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No PDF viewer found", Toast.LENGTH_SHORT).show();
        }
    }


}