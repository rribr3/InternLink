package com.example.internlink;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class RoleActivity extends AppCompatActivity {

    LinearLayout card1, card2;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_role);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        card1 = findViewById(R.id.card1);
        card2 = findViewById(R.id.card2);

        card1.setOnClickListener(v -> {
            Intent intent = new Intent(RoleActivity.this, LoginActivity.class);
            startActivity(intent);
        });
        card2.setOnClickListener(v -> {
            Intent intent = new Intent(RoleActivity.this, LoginActivity.class);
            startActivity(intent);
        });

    }
}