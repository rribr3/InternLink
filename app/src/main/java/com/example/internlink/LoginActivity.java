package com.example.internlink;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Base64;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executor;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;


public class LoginActivity extends AppCompatActivity {

    ImageButton facebookButton;
    ImageButton googleButton;
    Button loginButton;
    Button signupButton;
    Button backButton, btnAuth;
    private GestureDetector gestureDetector;
    private String role;
    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;
    private SharedPreferences biometricPrefs;
    private static final String BIOMETRIC_PREFS = "biometric_prefs";
    private static final String KEY_BIOMETRIC_ENABLED = "biometric_enabled";
    private static final String KEY_SAVED_EMAIL = "saved_email";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        role = getIntent().getStringExtra("role");

        // Initialize SharedPreferences for biometric data
        biometricPrefs = getSharedPreferences(BIOMETRIC_PREFS, MODE_PRIVATE);

        facebookButton = findViewById(R.id.facebookButton);
        googleButton = findViewById(R.id.googleButton);
        loginButton = findViewById(R.id.login_Button);
        signupButton = findViewById(R.id.signup);
        backButton = findViewById(R.id.backButton);
        btnAuth = findViewById(R.id.btnAuth);

        setupBiometricAuthentication();
        checkBiometricAvailability();

        facebookButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com"));
            startActivity(intent);
        });

        googleButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"));
            startActivity(intent);
        });

        loginButton.setOnClickListener(v -> showLoginPopup());
        signupButton.setOnClickListener(v -> showSignupPopup());

        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RoleActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

        gestureDetector = new GestureDetector(this, new SwipeGestureListener());
    }

    private void setupBiometricAuthentication() {
        executor = ContextCompat.getMainExecutor(this);
        biometricPrompt = new BiometricPrompt(LoginActivity.this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    Toast.makeText(getApplicationContext(), "Authentication Error: " + errString, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                Toast.makeText(getApplicationContext(), "Authentication Successful", Toast.LENGTH_SHORT).show();

                // Get saved email and proceed with automatic login
                String savedEmail = biometricPrefs.getString(KEY_SAVED_EMAIL, "");
                if (!savedEmail.isEmpty()) {
                    proceedWithBiometricLogin(savedEmail);
                } else {
                    Toast.makeText(LoginActivity.this, "No saved credentials found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(getApplicationContext(), "Authentication Failed", Toast.LENGTH_SHORT).show();
            }
        });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric Login")
                .setSubtitle("Use your fingerprint or face to login")
                .setNegativeButtonText("Use Password")
                .build();

        btnAuth.setOnClickListener(v -> {
            if (isBiometricEnabled()) {
                biometricPrompt.authenticate(promptInfo);
            } else {
                Toast.makeText(this, "Biometric login not set up. Please login with password first.", Toast.LENGTH_LONG).show();
                showLoginPopup();
            }
        });
    }

    private void checkBiometricAvailability() {
        BiometricManager biometricManager = BiometricManager.from(this);
        switch (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                // Show biometric button if biometric is already enabled
                if (isBiometricEnabled()) {
                    btnAuth.setVisibility(View.VISIBLE);
                }
                break;
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                btnAuth.setVisibility(View.GONE);
                Toast.makeText(this, "No biometric features available on this device", Toast.LENGTH_SHORT).show();
                break;
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                btnAuth.setVisibility(View.GONE);
                Toast.makeText(this, "Biometric features are currently unavailable", Toast.LENGTH_SHORT).show();
                break;
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                btnAuth.setVisibility(View.GONE);
                Toast.makeText(this, "No biometric credentials enrolled. Please set up fingerprint or face unlock in Settings", Toast.LENGTH_LONG).show();
                break;
        }
    }

    private boolean isBiometricEnabled() {
        return biometricPrefs.getBoolean(KEY_BIOMETRIC_ENABLED, false);
    }

    private void enableBiometricLogin(String email) {
        biometricPrefs.edit()
                .putBoolean(KEY_BIOMETRIC_ENABLED, true)
                .putString(KEY_SAVED_EMAIL, email)
                .apply();

        btnAuth.setVisibility(View.VISIBLE);
        Toast.makeText(this, "Biometric login enabled for future logins", Toast.LENGTH_SHORT).show();
    }

    private void proceedWithBiometricLogin(String email) {
        // Show loading dialog
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Logging in...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Check if user is already signed in with Firebase
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null && auth.getCurrentUser().getEmail().equals(email)) {
            // User is already authenticated, proceed to dashboard
            progressDialog.dismiss();
            proceedToDashboard(auth.getCurrentUser().getUid());
        } else {
            // For security, we should still require the user to enter password
            // Biometric should only be used as a convenience for filling email
            progressDialog.dismiss();
            showLoginPopupWithEmail(email);
        }
    }

    private void showLoginPopupWithEmail(String email) {
        BottomSheetDialog loginDialog = new BottomSheetDialog(this);
        loginDialog.setContentView(R.layout.login_popup);

        EditText emailEditText = loginDialog.findViewById(R.id.email_login);
        emailEditText.setText(email);
        emailEditText.setEnabled(false); // Disable editing since it's from biometric

        // Focus on password field
        EditText passwordEditTextlog = loginDialog.findViewById(R.id.password_login);
        passwordEditTextlog.requestFocus();

        // Rest of the login popup setup...
        setupLoginPopup(loginDialog);
        loginDialog.show();
    }

    private void showLoginPopup() {
        BottomSheetDialog loginDialog = new BottomSheetDialog(this);
        loginDialog.setContentView(R.layout.login_popup);
        setupLoginPopup(loginDialog);
        loginDialog.show();
    }

    private void setupLoginPopup(BottomSheetDialog loginDialog) {
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
                            // Ask user if they want to enable biometric login
                            if (!isBiometricEnabled() && canUseBiometric()) {
                                showBiometricSetupDialog(email);
                            }

                            loginDialog.dismiss();

                            // Update user activity
                            updateUserActivity();

                            // Proceed to dashboard
                            String userId = auth.getCurrentUser().getUid();
                            proceedToDashboard(userId);

                        } else {
                            Toast.makeText(LoginActivity.this, "Invalid credentials", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }

    private boolean canUseBiometric() {
        BiometricManager biometricManager = BiometricManager.from(this);
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS;
    }

    private void showBiometricSetupDialog(String email) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Enable Biometric Login")
                .setMessage("Would you like to use your fingerprint or face to login faster next time?")
                .setPositiveButton("Enable", (dialog, which) -> {
                    enableBiometricLogin(email);
                })
                .setNegativeButton("Not Now", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void updateUserActivity() {
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
    }

    private void proceedToDashboard(String userId) {
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
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, "User data not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(LoginActivity.this, "Database error", Toast.LENGTH_SHORT).show();
            }
        });
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
                                    // Clear biometric data
                                    biometricPrefs.edit().clear().apply();
                                    btnAuth.setVisibility(View.GONE);
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

    // Add method to disable biometric login (for logout or settings)
    public void disableBiometricLogin() {
        biometricPrefs.edit().clear().apply();
        btnAuth.setVisibility(View.GONE);
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

            ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("Creating account...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            FirebaseAuth auth = FirebaseAuth.getInstance();
            auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        progressDialog.dismiss();
                        if (task.isSuccessful()) {
                            String userId = auth.getCurrentUser().getUid();

                            if ("company".equals(role)) {
                                Intent intent = new Intent(LoginActivity.this, CreateCompanyProfileActivity.class);
                                intent.putExtra("userId", userId);
                                intent.putExtra("name", name);
                                intent.putExtra("email", email);
                                intent.putExtra("password", password);
                                startActivity(intent);
                            } else if ("student".equals(role)) {
                                Intent intent = new Intent(LoginActivity.this, CreateStudentProfileActivity.class);
                                intent.putExtra("userId", userId);
                                intent.putExtra("name", name);
                                intent.putExtra("email", email);
                                intent.putExtra("password", password);
                                startActivity(intent);
                            } else {
                                DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
                                usersRef.child(userId).child("name").setValue(name);
                                usersRef.child(userId).child("email").setValue(email);
                                usersRef.child(userId).child("role").setValue(role);
                                finish();
                            }

                            signupDialog.dismiss();
                        } else {
                            Exception e = task.getException();
                            Toast.makeText(this, "Signup failed: " + (e != null ? e.getMessage() : "Unknown error"), Toast.LENGTH_LONG).show();
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