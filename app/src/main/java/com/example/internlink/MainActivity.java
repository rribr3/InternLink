package com.example.internlink;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION_MS = 2500;
    private static final String TAG = "SplashScreen";

    private ImageView logo;
    private TextView appName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        playAnimations();
        goToNextScreenAfterDelay();
    }

    private void initViews() {
        logo = findViewById(R.id.logo);
        appName = findViewById(R.id.appName);

        // Set initial visibility (optional, can be done in XML)
        logo.setAlpha(1f); // Changed from 0f to 1f for testing
        appName.setAlpha(1f); // Changed from 0f to 1f for testing
    }

    private void playAnimations() {
        try {
            Animation logoAnimation = AnimationUtils.loadAnimation(this, R.anim.zoom_fade);
            Animation textAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade);

            if (logoAnimation != null) logo.startAnimation(logoAnimation);
            if (textAnimation != null) appName.startAnimation(textAnimation);

        } catch (Exception e) {
            Log.e(TAG, "Animation failed, using fallback.", e);

            // Fallback animations
            logo.animate()
                    .alpha(1f)
                    .scaleX(0.5f)
                    .scaleY(0.8f)
                    .setDuration(1000)
                    .start();

            appName.animate()
                    .alpha(1f)
                    .translationY(-30f)
                    .setDuration(1500)
                    .setStartDelay(200)
                    .start();
        }
    }

    private void goToNextScreenAfterDelay() {
        new Handler().postDelayed(() -> {
            startActivity(new Intent(MainActivity.this, OnboardActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, SPLASH_DURATION_MS);
    }

    @Override
    protected void onDestroy() {
        if (logo != null) logo.clearAnimation();
        if (appName != null) appName.clearAnimation();
        super.onDestroy();
    }
}