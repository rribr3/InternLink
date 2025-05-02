package com.example.internlink;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

public class DashboardActivity extends AppCompatActivity {

    private TextView totalStudentsText, totalCompaniesText;
    private AppCompatButton backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        totalStudentsText = findViewById(R.id.totalStudentsText);
        totalCompaniesText = findViewById(R.id.totalCompaniesText);
        backButton = findViewById(R.id.backButton);  // Initialize back button

        backButton.setOnClickListener(v -> finish()); // Close this activity on click

        countTotalStudents();
        countTotalCompanies();

        LineChart lineChart = findViewById(R.id.userActivityChart);
        Description description = lineChart.getDescription();
        description.setText("Activity Count by Date");
        loadUserActivityFromFirebase(lineChart);
    }

    private void countTotalStudents() {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

        Query studentQuery = usersRef.orderByChild("role").equalTo("student");

        studentQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int count = (int) snapshot.getChildrenCount();
                totalStudentsText.setText(String.valueOf(count));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DashboardActivity.this, "Failed to load student count", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void countTotalCompanies() {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

        Query companyQuery = usersRef.orderByChild("role").equalTo("company");

        companyQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int count = (int) snapshot.getChildrenCount();
                totalCompaniesText.setText(String.valueOf(count));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DashboardActivity.this, "Failed to load company count", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadUserActivityFromFirebase(LineChart lineChart) {
        DatabaseReference activityRef = FirebaseDatabase.getInstance().getReference("user_activity");

        activityRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Entry> entries = new ArrayList<>();
                List<String> dates = new ArrayList<>();
                int index = 0;

                for (DataSnapshot daySnapshot : snapshot.getChildren()) {
                    String date = daySnapshot.getKey();
                    Long count = daySnapshot.child("count").getValue(Long.class);

                    if (count != null) {
                        entries.add(new Entry(index, count.floatValue()));
                        dates.add(date);
                        index++;
                    }
                }

                LineDataSet dataSet = new LineDataSet(entries, "User Activity");
                dataSet.setColor(android.graphics.Color.rgb(136, 14, 79));
                dataSet.setValueTextColor(android.graphics.Color.DKGRAY);
                dataSet.setLineWidth(2f);

                LineData lineData = new LineData(dataSet);
                lineChart.setData(lineData);
                lineChart.invalidate();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DashboardActivity.this, "Failed to load chart data", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
