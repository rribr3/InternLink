package com.example.internlink;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AllStudents extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_all_students);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        String companyId = getIntent().getStringExtra("COMPANY_ID");
        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) RecyclerView recyclerView = findViewById(R.id.rv_all_students);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        DatabaseReference applicationsRef = FirebaseDatabase.getInstance().getReference("applications");
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

        applicationsRef.orderByChild("companyId").equalTo(companyId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot appsSnapshot) {
                        Set<String> acceptedStudentIds = new HashSet<>();
                        for (DataSnapshot app : appsSnapshot.getChildren()) {
                            String status = app.child("status").getValue(String.class);
                            String studentId = app.child("userId").getValue(String.class);
                            if ("Accepted".equalsIgnoreCase(status) && studentId != null) {
                                acceptedStudentIds.add(studentId);
                            }
                        }

                        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot usersSnapshot) {
                                List<User> studentList = new ArrayList<>();
                                for (DataSnapshot userSnap : usersSnapshot.getChildren()) {
                                    String uid = userSnap.getKey();
                                    String role = userSnap.child("role").getValue(String.class);
                                    if ("student".equals(role) && acceptedStudentIds.contains(uid)) {
                                        User student = userSnap.getValue(User.class);
                                        if (student != null) {
                                            student.setUid(uid); // <-- Set UID manually from Firebase key
                                            studentList.add(student);
                                        }
                                    }
                                }

                                recyclerView.setAdapter(new StudentAdapter(AllStudents.this, studentList));
                                findViewById(R.id.empty_state_layout).setVisibility(studentList.isEmpty() ? View.VISIBLE : View.GONE);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {}
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });

    }
}