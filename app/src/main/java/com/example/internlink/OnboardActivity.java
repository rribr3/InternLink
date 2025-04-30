package com.example.internlink;

import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import java.util.Arrays;
import java.util.List;

public class OnboardActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private LinearLayout indicatorsContainer;
    private Button buttonNext;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_onboard);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button skipButton = findViewById(R.id.skipButton);
        skipButton.setOnClickListener(v -> {
            Intent intent = new Intent(OnboardActivity.this, RoleActivity.class);
            startActivity(intent);
            finish();
        });


        viewPager = findViewById(R.id.viewPager);
        indicatorsContainer = findViewById(R.id.indicatorLayout);
        buttonNext = findViewById(R.id.buttonNext);

        List<OnboardingItem> onboardingItems = Arrays.asList(
                new OnboardingItem(R.drawable.onboard1, "Connecting Talent with Opportunity", "Whether you're looking for an internship or offering one, we've got you covered."),
                new OnboardingItem(R.drawable.onboard2, "Bringing the Right People Together", "We help students discover the right internships and companies find the right candidates."),
                new OnboardingItem(R.drawable.onboard3, "Internship Platform", "Create profiles, post or apply to internships, and manage everything in one place.")
        );

        viewPager.setAdapter(new OnboardingAdapter(onboardingItems));
        setupIndicators(onboardingItems.size());
        setCurrentIndicator(0);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                setCurrentIndicator(position);
                buttonNext.setText(position == onboardingItems.size() - 1 ? "Get Started" : "Next");
            }
        });

        buttonNext.setOnClickListener(v -> {
            if (viewPager.getCurrentItem() + 1 < onboardingItems.size()) {
                viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
            } else {
                startActivity(new Intent(this, RoleActivity.class));
                finish();
            }
        });
    }
    private void setupIndicators(int count) {
        ImageView[] indicators = new ImageView[count];
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(8, 0, 8, 0);

        for (int i = 0; i < indicators.length; i++) {
            indicators[i] = new ImageView(this);
            indicators[i].setImageDrawable(ContextCompat.getDrawable(this, R.drawable.indicator_inactive));
            indicators[i].setLayoutParams(params);
            indicatorsContainer.addView(indicators[i]);
        }
    }

    private void setCurrentIndicator(int index) {
        int count = indicatorsContainer.getChildCount();
        for (int i = 0; i < count; i++) {
            ImageView imageView = (ImageView) indicatorsContainer.getChildAt(i);
            imageView.setImageDrawable(ContextCompat.getDrawable(
                    this,
                    i == index ? R.drawable.indicator_active : R.drawable.indicator_inactive
            ));
        }
    }
}