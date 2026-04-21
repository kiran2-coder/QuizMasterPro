package com.quizmaster.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.quizmaster.app.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private boolean isNavigating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        binding.btnLogin.setOnClickListener(v -> loginUser());

        binding.tvRegister.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));

        binding.tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
    }

    private void showForgotPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset Password");
        builder.setMessage("Enter your registered email to receive a reset link.");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        input.setHint("email@example.com");

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(60, 20, 60, 20);
        container.addView(input, lp);
        builder.setView(container);

        builder.setPositiveButton("Send Link", (dialog, which) -> {
            String email = input.getText().toString().trim();
            if (!email.isEmpty()) {
                sendResetEmail(email);
            } else {
                Toast.makeText(this, "Please enter an email", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void sendResetEmail(String email) {
        mAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(LoginActivity.this, "✅ Reset link sent!", Toast.LENGTH_LONG).show())
                .addOnFailureListener(e ->
                        Toast.makeText(LoginActivity.this, "❌ Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loginUser() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnLogin.setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        checkUserRole();
                    } else {
                        resetUI();
                        Toast.makeText(this, "Login Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void checkUserRole() {
        if (isNavigating) return;
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

        if (userId == null) {
            resetUI();
            Toast.makeText(this, "User session error. Try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);

        // 🕒 1. Better Timeout: Notify the user if it's taking too long
        Handler timeoutHandler = new Handler(Looper.getMainLooper());
        Runnable timeoutRunnable = () -> {
            if (!isNavigating && !isFinishing()) {
                resetUI();
                Toast.makeText(this, "⌛ Connection Timeout. Please check your internet.", Toast.LENGTH_LONG).show();
                // Optional: Log out if you want to prevent unauthorized access
                // mAuth.signOut();
            }
        };
        timeoutHandler.postDelayed(timeoutRunnable, 6000); // Increased to 6s for slow networks

        // 🔍 2. Detailed Firestore Fetch
        db.collection("users").document(userId).get()
                .addOnSuccessListener(doc -> {
                    timeoutHandler.removeCallbacks(timeoutRunnable); // Stop the timeout timer
                    if (isFinishing() || isNavigating) return;

                    if (doc.exists()) {
                        String role = doc.getString("role");
                        String status = doc.getString("status");

                        if ("restricted".equalsIgnoreCase(status)) {
                            handleRestrictedUser();
                        } else if ("admin".equalsIgnoreCase(role)) {
                            navigateTo(AdminDashboardActivity.class);
                        } else if ("quizmaster".equalsIgnoreCase(role)) {
                            navigateTo(QuizMasterDashboardActivity.class);
                        } else {
                            navigateTo(UserDashboardActivity.class);
                        }
                    } else {
                        // Document missing: Likely a new user who hasn't completed profile
                        Log.d("AUTH", "No user document found for UID: " + userId);
                        navigateTo(UserDashboardActivity.class);
                    }
                })
                .addOnFailureListener(e -> {
                    timeoutHandler.removeCallbacks(timeoutRunnable); // Stop the timeout timer
                    resetUI();

                    // 🛑 THIS IS THE MOST IMPORTANT PART FOR DEBUGGING
                    Log.e("AUTH_DB_ERROR", "Firestore Error: " + e.getMessage());
                    Toast.makeText(this, "❌ Database Error: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void navigateTo(Class<?> destinationClass) {
        if (isNavigating) return;
        isNavigating = true;
        binding.progressBar.setVisibility(View.GONE);
        Intent intent = new Intent(LoginActivity.this, destinationClass);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void handleRestrictedUser() {
        mAuth.signOut();
        resetUI();
        Toast.makeText(this, "🚫 Account Restricted!", Toast.LENGTH_LONG).show();
    }

    private void resetUI() {
        binding.progressBar.setVisibility(View.GONE);
        binding.btnLogin.setEnabled(true);
        isNavigating = false;
    }
}