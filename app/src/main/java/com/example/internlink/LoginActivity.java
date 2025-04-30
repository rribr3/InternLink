package com.example.internlink;

import android.app.Dialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetDialog;

public class LoginActivity extends AppCompatActivity {

    ImageButton facebookButton;
    ImageButton googleButton;
    Button loginButton;
    Button signupButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        facebookButton = findViewById(R.id.facebookButton);
        googleButton = findViewById(R.id.googleButton);
        loginButton = findViewById(R.id.login_Button);
        signupButton = findViewById(R.id.signup);

        facebookButton.setOnClickListener(v ->
                Toast.makeText(this, "Facebook login", Toast.LENGTH_SHORT).show());

        googleButton.setOnClickListener(v ->
                Toast.makeText(this, "Google login", Toast.LENGTH_SHORT).show());

        loginButton.setOnClickListener(v -> showLoginPopup());

        signupButton.setOnClickListener(v -> showSignupPopup());
    }

    private void showLoginPopup() {
        BottomSheetDialog loginDialog = new BottomSheetDialog(this);
        loginDialog.setContentView(R.layout.login_popup);

        loginDialog.getBehavior().setPeekHeight(
                getResources().getDisplayMetrics().heightPixels / 2
        );

        loginDialog.show();
    }

    private void showSignupPopup() {
        Dialog signupDialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        signupDialog.setContentView(R.layout.signup_popup);
        signupDialog.show();
    }
}