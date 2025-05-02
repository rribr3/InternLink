package com.example.internlink;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class UserManagementActivity extends AppCompatActivity {

    private MaterialCardView cardStudents, cardCompanies;
    private DatabaseReference databaseRef;
    private AppCompatButton backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_management);

        backButton = findViewById(R.id.backButton);  // Initialize back button

        backButton.setOnClickListener(v -> finish());

        cardStudents = findViewById(R.id.cardStudents);
        cardCompanies = findViewById(R.id.cardCompanies);

        databaseRef = FirebaseDatabase.getInstance().getReference();

        // Replace "studentId1" and "companyId1" with real IDs from your database
        cardStudents.setOnClickListener(v -> showStudentPopup("studentId1"));
        cardCompanies.setOnClickListener(v -> showCompanyPopup("companyId1"));
    }

    private void showStudentPopup(String studentId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View popupView = LayoutInflater.from(this).inflate(R.layout.popup_student_info, null);
        builder.setView(popupView);

        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) TextView studentName = popupView.findViewById(R.id.studentName);
        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) TextView studentInfo = popupView.findViewById(R.id.studentInfo);
        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) Button approveBtn = popupView.findViewById(R.id.approveStudentBtn);
        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) Button rejectBtn = popupView.findViewById(R.id.rejectStudentBtn);

        AlertDialog dialog = builder.create();
        dialog.show();

        DatabaseReference ref = databaseRef.child("students").child(studentId);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                String name = snapshot.child("name").getValue(String.class);
                String education = snapshot.child("education").getValue(String.class);
                String projects = snapshot.child("projects").getValue(String.class);
                String credibility = snapshot.child("credibility").getValue(String.class);

                studentName.setText(name != null ? name : "Unknown");
                studentInfo.setText("Education: " + (education != null ? education : "N/A") +
                        "\nProjects: " + (projects != null ? projects : "N/A") +
                        "\nCredibility: " + (credibility != null ? credibility : "N/A"));
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(UserManagementActivity.this, "Failed to load student data", Toast.LENGTH_SHORT).show();
            }
        });

        approveBtn.setOnClickListener(v -> {
            ref.child("status").setValue("approved");
            Toast.makeText(this, "Student approved", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        rejectBtn.setOnClickListener(v -> {
            ref.child("status").setValue("rejected");
            Toast.makeText(this, "Student rejected", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
    }

    private void showCompanyPopup(String companyId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View popupView = LayoutInflater.from(this).inflate(R.layout.popup_company_info, null);
        builder.setView(popupView);

        TextView companyName = popupView.findViewById(R.id.companyName);
        TextView companyInfo = popupView.findViewById(R.id.companyInfo);
        Button approveBtn = popupView.findViewById(R.id.approveCompanyBtn);
        Button rejectBtn = popupView.findViewById(R.id.rejectCompanyBtn);

        AlertDialog dialog = builder.create();
        dialog.show();

        DatabaseReference ref = databaseRef.child("companies").child(companyId);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                String name = snapshot.child("name").getValue(String.class);
                String sector = snapshot.child("sector").getValue(String.class);
                String postedProjects = snapshot.child("postedProjects").getValue(String.class);

                companyName.setText(name != null ? name : "Unknown");
                companyInfo.setText("Sector: " + (sector != null ? sector : "N/A") +
                        "\nPosted Projects: " + (postedProjects != null ? postedProjects : "N/A"));
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(UserManagementActivity.this, "Failed to load company data", Toast.LENGTH_SHORT).show();
            }
        });

        approveBtn.setOnClickListener(v -> {
            ref.child("status").setValue("approved");
            Toast.makeText(this, "Company approved", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        rejectBtn.setOnClickListener(v -> {
            ref.child("status").setValue("rejected");
            Toast.makeText(this, "Company rejected", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        AppCompatButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Close this activity and return to the previous one
            }
        });
    }
}
