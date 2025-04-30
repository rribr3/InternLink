package com.example.internlink;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

public class LoginActivity extends AppCompatActivity {

    ImageButton facebookButton;
    ImageButton googleButton;
    Button loginButton;
    Button signupButton;
    Button backButton;
    private GestureDetector gestureDetector;
    private String role;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        role = getIntent().getStringExtra("role");

        facebookButton = findViewById(R.id.facebookButton);
        googleButton = findViewById(R.id.googleButton);
        loginButton = findViewById(R.id.login_Button);
        signupButton = findViewById(R.id.signup);
        backButton = findViewById(R.id.backButton);

        facebookButton.setOnClickListener(v ->
                Toast.makeText(this, "Facebook login", Toast.LENGTH_SHORT).show());

        googleButton.setOnClickListener(v ->
                Toast.makeText(this, "Google login", Toast.LENGTH_SHORT).show());

        loginButton.setOnClickListener(v -> showLoginPopup());

        signupButton.setOnClickListener(v -> showSignupPopup());

        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RoleActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right); // RoleActivity slides in from left
        });


        gestureDetector = new GestureDetector(this, new SwipeGestureListener());
    }

    private void showLoginPopup() {
        BottomSheetDialog loginDialog = new BottomSheetDialog(this);
        loginDialog.setContentView(R.layout.login_popup);

        loginDialog.getBehavior().setPeekHeight(
                getResources().getDisplayMetrics().heightPixels / 2
        );

        EditText emailEditText = loginDialog.findViewById(R.id.email_login);
        EditText passwordEditText = loginDialog.findViewById(R.id.password_login);
        Button loginConfirm = loginDialog.findViewById(R.id.login_confirm_button);

        loginConfirm.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

            usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    boolean found = false;
                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                        String dbEmail = userSnapshot.child("email").getValue(String.class);
                        String dbPassword = userSnapshot.child("password").getValue(String.class);
                        String role = userSnapshot.child("role").getValue(String.class);

                        if (email.equals(dbEmail) && password.equals(dbPassword)) {
                            found = true;
                            if ("admin@internlink.com".equals(dbEmail)) {
                                startActivity(new Intent(LoginActivity.this, AdminActivity.class));
                            } else if ("student".equals(role)) {
                                startActivity(new Intent(LoginActivity.this, StudentActivity.class));
                            } else if ("company".equals(role)) {
                                startActivity(new Intent(LoginActivity.this, CompanyActivity.class));
                            } else {
                                Toast.makeText(LoginActivity.this, "Unknown role", Toast.LENGTH_SHORT).show();
                            }
                            break;
                        }
                    }

                    if (!found) {
                        Toast.makeText(LoginActivity.this, "Invalid credentials", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    Toast.makeText(LoginActivity.this, "Database error", Toast.LENGTH_SHORT).show();
                }
            });
        });



        loginDialog.show();
    }

    private void showSignupPopup() {
        Dialog signupDialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        signupDialog.setContentView(R.layout.signup_popup);

        EditText nameEditText = signupDialog.findViewById(R.id.full_name_signup);
        EditText emailEditText = signupDialog.findViewById(R.id.email_signup);
        EditText passwordEditText = signupDialog.findViewById(R.id.password_signup);
        Button signupConfirm = signupDialog.findViewById(R.id.signup_confirm_button);
        Button lowerButton = signupDialog.findViewById(R.id.lowerButton);

        signupConfirm.setOnClickListener(v -> {
            String name = nameEditText.getText().toString().trim();
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            FirebaseAuth auth = FirebaseAuth.getInstance();
            DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

            auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            String userId = auth.getCurrentUser().getUid();

                            // Create a new User object with the necessary data
                            User user = new User(name, email, role); // Assuming 'User' is your model class

                            // Save the user details in Realtime Database
                            usersRef.child(userId).setValue(user)
                                    .addOnCompleteListener(task1 -> {
                                        if (task1.isSuccessful()) {
                                            Toast.makeText(this, "Signup successful", Toast.LENGTH_SHORT).show();
                                            signupDialog.dismiss();
                                        } else {
                                            Toast.makeText(this, "Failed to save user data", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            Toast.makeText(this, "Signup failed", Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        lowerButton.setOnClickListener(v -> signupDialog.dismiss());

        signupDialog.show();
    }



    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }

    private class SwipeGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            float diffX = e2.getX() - e1.getX();
            float diffY = e2.getY() - e1.getY();

            if (Math.abs(diffX) > Math.abs(diffY) && Math.abs(diffX) > SWIPE_THRESHOLD &&
                    Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                if (diffX > 0) {
                    // Swipe right
                    Intent intent = new Intent(LoginActivity.this, RoleActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                    return true;
                }
            }
            return false;
        }
    }

}
