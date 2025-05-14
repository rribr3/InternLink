package com.example.internlink;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class CreateStudentProfileActivity extends AppCompatActivity {

    private String userId, name, email, password;

    private EditText editFullName, editEmail, editPhone, editBio, editUniversity, editDegree, editGradYear, editGPA, editPortfolio, editLinkedIn, editGitHub;
    private ImageView profilePhoto;
    private MaterialButton btnSubmit;
    private Button btnUploadCV;
    private CheckBox checkAgree;
    private Uri logoUri = null;

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
    private Uri cvUri = null;
    private String uploadedCVUrl = null;
    private final ActivityResultLauncher<String> pdfPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    cvUri = uri;
                    Toast.makeText(this, "PDF selected: " + uri.getLastPathSegment(), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "No file selected", Toast.LENGTH_SHORT).show();
                }
            });


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
            if (!validateInputs()) return;

            if (cvUri != null) {
                uploadCVAndSubmit();
            } else {
                submitProfile(null); // No CV uploaded
            }
        });

        btnUploadCV.setOnClickListener(v -> {
            pdfPickerLauncher.launch("application/pdf");
        });

    }
    private void uploadCVAndSubmit() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Uploading CV...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        String fileName = "CVs/" + userId + "_cv.pdf";
        StorageReference storageRef = FirebaseStorage.getInstance().getReference().child(fileName);

        storageRef.putFile(cvUri).addOnSuccessListener(taskSnapshot ->
                storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    uploadedCVUrl = uri.toString();
                    progressDialog.dismiss();
                    submitProfile(uploadedCVUrl);
                }).addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Failed to get CV URL", Toast.LENGTH_SHORT).show();
                })
        ).addOnFailureListener(e -> {
            progressDialog.dismiss();
            Toast.makeText(this, "CV upload failed", Toast.LENGTH_SHORT).show();
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
        editPortfolio = findViewById(R.id.editPortfolio);
        editGitHub = findViewById(R.id.editGitHub);
        editEmail = findViewById(R.id.editEmail);
        editPhone = findViewById(R.id.editPhone);
        profilePhoto = findViewById(R.id.profilePhoto);
        btnUploadCV = findViewById(R.id.btnUploadCV);
        checkAgree = findViewById(R.id.checkAgree);
        btnSubmit = findViewById(R.id.btnSubmit);
    }

    private void prefillFields() {
        if (name != null) editFullName.setText(name);
        if (email != null) editEmail.setText(email);
    }

    private boolean validateInputs() {
        return !(TextUtils.isEmpty(editFullName.getText()) ||
                //TextUtils.isEmpty(editIndustry.getText()) ||
                //TextUtils.isEmpty(editLocation.getText()) ||
                TextUtils.isEmpty(editEmail.getText()));
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
        companyData.put("portfolio", editPortfolio.getText().toString().trim());
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
                Intent intent = new Intent(this, CompanyHomeActivity.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Failed to save profile", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
