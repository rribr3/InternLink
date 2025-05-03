package com.example.internlink;

import android.app.Dialog;
import android.app.ProgressDialog;
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
import android.widget.TextView;
import android.widget.Toast;
import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


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
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

        gestureDetector = new GestureDetector(this, new SwipeGestureListener());
    }

    private void showLoginPopup() {
        BottomSheetDialog loginDialog = new BottomSheetDialog(this);
        loginDialog.setContentView(R.layout.login_popup);
        TextView forgotPasswordText = loginDialog.findViewById(R.id.forgot_password_text);

        forgotPasswordText.setOnClickListener(v -> {
            EditText emailEditText = loginDialog.findViewById(R.id.email_login);
            String email = emailEditText.getText().toString().trim();

            if (email.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please enter your email first", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this, "Password reset email sent", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(LoginActivity.this, "Failed to send reset email", Toast.LENGTH_SHORT).show();
                        }
                    });
        });

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
            passwordEditTextlog.setSelection(passwordEditTextlog.getText().length());
        });

        EditText emailEditText = loginDialog.findViewById(R.id.email_login);
        Button loginConfirm = loginDialog.findViewById(R.id.login_confirm_button);

        loginConfirm.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditTextlog.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show();
                return;
            }

            loginConfirm.setEnabled(false);
            ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("Logging in...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            FirebaseAuth auth = FirebaseAuth.getInstance();

            auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        progressDialog.dismiss();
                        loginConfirm.setEnabled(true);

                        if (task.isSuccessful()) {
                            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("user_activity").child(today).child("count");

                            ref.runTransaction(new Transaction.Handler() {
                                @NonNull
                                @Override
                                public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                                    Long currentCount = currentData.getValue(Long.class);
                                    if (currentCount == null) {
                                        currentData.setValue(1);
                                    } else {
                                        currentData.setValue(currentCount + 1);
                                    }
                                    return Transaction.success(currentData);
                                }

                                @Override
                                public void onComplete(DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {}
                            });

                            String userId = auth.getCurrentUser().getUid();
                            DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

                            usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot snapshot) {
                                    if (snapshot.exists()) {
                                        String status = snapshot.child("status").getValue(String.class);
                                        if ("deactivated".equalsIgnoreCase(status)) {
                                            showReactivationDialog(userId);
                                            return;
                                        }


                                        String userRole = snapshot.child("role").getValue(String.class);

                                        if ("admin".equals(userRole)) {
                                            startActivity(new Intent(LoginActivity.this, AdminActivity.class));
                                        } else if ("student".equals(userRole)) {
                                            startActivity(new Intent(LoginActivity.this, StudentHomeActivity.class));
                                        } else if ("company".equals(userRole)) {
                                            startActivity(new Intent(LoginActivity.this, CompanyHomeActivity.class));
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
    private void showReactivationDialog(String userId) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Account Deactivated")
                .setMessage("Your account is currently deactivated. Would you like to reactivate it?")
                .setPositiveButton("Reactivate", (dialog, which) -> {
                    FirebaseDatabase.getInstance().getReference("users")
                            .child(userId)
                            .child("status")
                            .setValue("active")
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(this, "Account reactivated. Please log in again.", Toast.LENGTH_SHORT).show();
                                    FirebaseAuth.getInstance().signOut();
                                } else {
                                    Toast.makeText(this, "Failed to reactivate. Try again later.", Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    dialog.dismiss();
                })
                .setCancelable(false)
                .show();
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
                            User user = new User(name, email, role);
                            usersRef.child(userId).setValue(user);
                            usersRef.child(userId).child("active").setValue(true);

                            usersRef.child(userId).setValue(user)
                                    .addOnCompleteListener(task1 -> {
                                        if (task1.isSuccessful()) {
                                            Toast.makeText(this, "Signup successful", Toast.LENGTH_SHORT).show();
                                            View dialogView = signupDialog.findViewById(R.id.signup_root);
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
                            e.printStackTrace();
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

            if (Math.abs(diffX) > Math.abs(diffY) &&
                    Math.abs(diffX) > SWIPE_THRESHOLD &&
                    Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                if (diffX > 0) {
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
