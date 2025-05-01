package com.example.internlink;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class DashboardActivity extends AppCompatActivity {

    private TextView totalStudentsText, totalCompaniesText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        totalStudentsText = findViewById(R.id.totalStudentsText);
        totalCompaniesText = findViewById(R.id.totalCompaniesText);

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
                dataSet.setColor(Color.rgb(136, 14, 79));
                dataSet.setValueTextColor(Color.DKGRAY);
                dataSet.setLineWidth(2f);

                LineData lineData = new LineData(dataSet);
                lineChart.setData(lineData);
                lineChart.invalidate(); // refresh

                // Optional: Set x-axis labels if needed
                // You can use IndexAxisValueFormatter to map indices to dates
            /*
            XAxis xAxis = lineChart.getXAxis();
            xAxis.setValueFormatter(new IndexAxisValueFormatter(dates));
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setGranularity(1f);
            xAxis.setGranularityEnabled(true);
            */
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DashboardActivity.this, "Failed to load chart data", Toast.LENGTH_SHORT).show();
            }
        });
    }

}

