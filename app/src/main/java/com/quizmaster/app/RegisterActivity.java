package com.quizmaster.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.quizmaster.app.databinding.ActivityRegisterBinding;

public class RegisterActivity extends AppCompatActivity {
    private ActivityRegisterBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private boolean isProcessing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        binding.btnRegister.setOnClickListener(v -> registerUser());
        binding.tvLogin.setOnClickListener(v ->
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class)));
    }

    private void registerUser() {
        if (isProcessing) return;

        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        String name = binding.etName.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        isProcessing = true;
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnRegister.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String userId = mAuth.getCurrentUser().getUid();
                        saveUserData(userId, name, email);
                    } else {
                        resetUI();
                        Toast.makeText(this, "Registration Failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserData(String userId, String name, String email) {
        // 🕒 1. Timeout Logic
        Handler timeoutHandler = new Handler(Looper.getMainLooper());
        Runnable timeoutRunnable = () -> {
            if (!isFinishing()) {
                resetUI();
                Log.e("REG_TIMEOUT", "Firestore save timed out for: " + userId);
                Toast.makeText(this, "⌛ Database slow. Try logging in now.", Toast.LENGTH_LONG).show();
            }
        };
        timeoutHandler.postDelayed(timeoutRunnable, 6000);

        // 2. Prepare Model
        UserModel newUser = new UserModel(
                userId, name, email, "user", "active", name, System.currentTimeMillis()
        );

        // 3. Firestore Write
        db.collection("users").document(userId)
                .set(newUser)
                .addOnSuccessListener(aVoid -> {
                    timeoutHandler.removeCallbacks(timeoutRunnable);
                    if (isFinishing()) return;

                    Toast.makeText(this, "✅ Success! Please Login.", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    timeoutHandler.removeCallbacks(timeoutRunnable);
                    resetUI();
                    Log.e("REG_DB_ERROR", "Error: " + e.getMessage());
                    Toast.makeText(this, "❌ Profile Error: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void resetUI() {
        isProcessing = false;
        binding.progressBar.setVisibility(View.GONE);
        binding.btnRegister.setEnabled(true);
    }
}