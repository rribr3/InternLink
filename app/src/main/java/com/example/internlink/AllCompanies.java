package com.example.internlink;

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

public class AllCompanies extends AppCompatActivity {

    private String studentId;
    private DatabaseReference applicationsRef;
    private DatabaseReference chatsRef;
    private DatabaseReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_all_companies);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        studentId = getIntent().getStringExtra("STUDENT_ID");

        applicationsRef = FirebaseDatabase.getInstance().getReference("applications");
        chatsRef = FirebaseDatabase.getInstance().getReference("chats");
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        loadAcceptedCompaniesWithoutChat();
    }

    private void loadAcceptedCompaniesWithoutChat() {
        applicationsRef.orderByChild("userId").equalTo(studentId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot appsSnapshot) {
                        Set<String> acceptedCompanyIds = new HashSet<>();

                        for (DataSnapshot app : appsSnapshot.getChildren()) {
                            String status = app.child("status").getValue(String.class);
                            String companyId = app.child("companyId").getValue(String.class);

                            if ("Accepted".equalsIgnoreCase(status) && companyId != null) {
                                acceptedCompanyIds.add(companyId);
                            }
                        }

                        loadChattedCompaniesThenFilter(acceptedCompanyIds);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void loadChattedCompaniesThenFilter(Set<String> acceptedCompanyIds) {
        chatsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot chatsSnapshot) {
                Set<String> chattedCompanyIds = new HashSet<>();
                for (DataSnapshot chat : chatsSnapshot.getChildren()) {
                    String user1 = chat.child("user1").getValue(String.class);
                    String user2 = chat.child("user2").getValue(String.class);

                    if ((studentId.equals(user1) || studentId.equals(user2))) {
                        String otherUser = studentId.equals(user1) ? user2 : user1;
                        chattedCompanyIds.add(otherUser);
                    }
                }

                Set<String> finalCompanyIds = new HashSet<>();
                for (String companyId : acceptedCompanyIds) {
                    if (!chattedCompanyIds.contains(companyId)) {
                        finalCompanyIds.add(companyId);
                    }
                }

                loadCompanyDetails(finalCompanyIds);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadCompanyDetails(Set<String> companyIds) {
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot usersSnapshot) {
                List<User> companyList = new ArrayList<>();
                for (DataSnapshot userSnap : usersSnapshot.getChildren()) {
                    String role = userSnap.child("role").getValue(String.class);
                    String uid = userSnap.getKey();

                    if ("company".equals(role) && companyIds.contains(uid)) {
                        User company = userSnap.getValue(User.class);
                        companyList.add(company);
                    }
                }

                RecyclerView recyclerView = findViewById(R.id.rv_all_companies);
                recyclerView.setLayoutManager(new LinearLayoutManager(AllCompanies.this));
                recyclerView.setAdapter(new CompanyAdapter(AllCompanies.this, companyList));

                findViewById(R.id.empty_state_layout).setVisibility(companyList.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
