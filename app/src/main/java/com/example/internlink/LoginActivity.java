package com.example.internlink;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
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

        ImageView togglePassword = loginDialog.findViewById(R.id.toggle_password_login);
        EditText passwordEditTextlog = loginDialog.findViewById(R.id.password_login);

        togglePassword.setOnClickListener(v -> {
            if (passwordEditTextlog.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                passwordEditTextlog.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                togglePassword.setImageResource(R.drawable.ic_eye_open);
            } else {
                passwordEditTextlog.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                togglePassword.setImageResource(R.drawable.ic_eye_closed);
            }
            passwordEditTextlog.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            passwordEditTextlog.setSelection(passwordEditTextlog.getText().length()); // Move cursor to end
        });


        EditText emailEditText = loginDialog.findViewById(R.id.email_login);
        Button loginConfirm = loginDialog.findViewById(R.id.login_confirm_button);

        loginConfirm.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditTextlog.getText().toString().trim();

            FirebaseAuth auth = FirebaseAuth.getInstance();

            auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            String userId = auth.getCurrentUser().getUid();
                            DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

                            usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot snapshot) {
                                    if (snapshot.exists()) {
                                        String userRole = snapshot.child("role").getValue(String.class);
                                        String dbEmail = snapshot.child("email").getValue(String.class);

                                        if ("admin@internlink.com".equals(dbEmail)) {
                                            startActivity(new Intent(LoginActivity.this, AdminActivity.class));
                                        } else if ("student".equals(userRole)) {
                                            startActivity(new Intent(LoginActivity.this, StudentActivity.class));
                                        } else if ("company".equals(userRole)) {
                                            startActivity(new Intent(LoginActivity.this, CompanyActivity.class));
                                        } else {
                                            Toast.makeText(LoginActivity.this, "Unknown role", Toast.LENGTH_SHORT).show();
                                        }
                                    } else {
                                        Toast.makeText(LoginActivity.this, "User data not found", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError error) {
                                    Toast.makeText(LoginActivity.this, "Database error", Toast.LENGTH_SHORT).show();
                                }
                            });

                        } else {
                            Toast.makeText(LoginActivity.this, "Invalid credentials", Toast.LENGTH_SHORT).show();
                        }
                    });

        });



        loginDialog.show();
    }

    private void showSignupPopup() {
        Dialog signupDialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        signupDialog.setContentView(R.layout.signup_popup);
        signupDialog.getWindow().getAttributes().windowAnimations = R.style.DialogSlideUpAnimation;

        EditText nameEditText = signupDialog.findViewById(R.id.full_name_signup);
        EditText emailEditText = signupDialog.findViewById(R.id.email_signup);
        EditText passwordEditText = signupDialog.findViewById(R.id.password_signup);
        EditText confirmPasswordEditText = signupDialog.findViewById(R.id.confirm_password_signup);
        Button signupConfirm = signupDialog.findViewById(R.id.signup_confirm_button);
        Button lowerButton = signupDialog.findViewById(R.id.lowerButton);
        ImageView toggleSignupPassword1 = signupDialog.findViewById(R.id.toggle_password_signup1);
        ImageView toggleSignupPassword2 = signupDialog.findViewById(R.id.toggle_password_signup2);

        toggleSignupPassword1.setOnClickListener(v -> {
            if (passwordEditText.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                toggleSignupPassword1.setImageResource(R.drawable.ic_eye_open);
            } else {
                passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                toggleSignupPassword1.setImageResource(R.drawable.ic_eye_closed);
            }
            passwordEditText.setTypeface(android.graphics.Typeface.create("sans-serif-black", android.graphics.Typeface.NORMAL));
            passwordEditText.setSelection(passwordEditText.getText().length());
        });

        toggleSignupPassword2.setOnClickListener(v -> {
            if (confirmPasswordEditText.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                confirmPasswordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                toggleSignupPassword2.setImageResource(R.drawable.ic_eye_open);
            } else {
                confirmPasswordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                toggleSignupPassword2.setImageResource(R.drawable.ic_eye_closed);
            }
            confirmPasswordEditText.setTypeface(android.graphics.Typeface.create("sans-serif-black", android.graphics.Typeface.NORMAL));
            confirmPasswordEditText.setSelection(confirmPasswordEditText.getText().length());
        });

        signupConfirm.setOnClickListener(v -> {
            String name = nameEditText.getText().toString().trim();
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            String confirmPassword = confirmPasswordEditText.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

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
                                            View dialogView = signupDialog.findViewById(R.id.signup_root); // The root layout in signup_popup.xml
                                            Animation slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down);

                                            slideDown.setAnimationListener(new Animation.AnimationListener() {
                                                @Override
                                                public void onAnimationStart(Animation animation) {}

                                                @Override
                                                public void onAnimationEnd(Animation animation) {
                                                    signupDialog.dismiss();
                                                }

                                                @Override
                                                public void onAnimationRepeat(Animation animation) {}
                                            });

                                            dialogView.startAnimation(slideDown);

                                        } else {
                                            Toast.makeText(this, "Failed to save user data", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            Exception e = task.getException();
                            Toast.makeText(this, "Signup failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            e.printStackTrace(); // Logs to Logcat
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
