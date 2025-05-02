package com.example.internlink;

import android.graphics.Color;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieEntry;

import java.util.ArrayList;
import java.util.List;

public class ReportsActivity extends AppCompatActivity {
    private AppCompatButton backButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_reports);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        backButton = findViewById(R.id.backButton);  // Initialize back button

        backButton.setOnClickListener(v -> finish());

        PieChart pieChart = findViewById(R.id.pieChart);
        BarChart barChart = findViewById(R.id.barChart);

        // ✅ Define and populate pieEntries
        List<PieEntry> pieEntries = new ArrayList<>();
        pieEntries.add(new PieEntry(40f, "Software"));
        pieEntries.add(new PieEntry(30f, "Marketing"));
        pieEntries.add(new PieEntry(20f, "Design"));
        pieEntries.add(new PieEntry(10f, "Finance"));

        // ✅ Define and populate barEntries
        List<BarEntry> barEntries = new ArrayList<>();
        barEntries.add(new BarEntry(0f, 120f)); // Project 1
        barEntries.add(new BarEntry(1f, 90f));  // Project 2
        barEntries.add(new BarEntry(2f, 70f));  // Project 3
        barEntries.add(new BarEntry(3f, 50f));  // Project 4

        // Setup PieChart
        PieDataSet pieDataSet = new PieDataSet(pieEntries, "Category");
        pieDataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        pieDataSet.setValueTextColor(Color.BLACK);
        pieDataSet.setValueTextSize(12f);
        PieData pieData = new PieData(pieDataSet);

        pieChart.setData(pieData);
        pieChart.getDescription().setEnabled(false);
        pieChart.animateY(1000, Easing.EaseInOutCubic);

        // Setup BarChart
        BarDataSet barDataSet = new BarDataSet(barEntries, "Applications");
        barDataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        barDataSet.setValueTextColor(Color.BLACK);
        barDataSet.setValueTextSize(12f);
        BarData barData = new BarData(barDataSet);

        barChart.setData(barData);
        barChart.getDescription().setEnabled(false);
        barChart.animateY(1000, Easing.EaseInOutBounce);
    }
}

