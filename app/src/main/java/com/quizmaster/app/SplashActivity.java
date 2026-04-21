package com.quizmaster.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import com.airbnb.lottie.LottieAnimationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.android.gms.tasks.OnSuccessListener;
import com.quizmaster.app.databinding.ActivitySplashBinding;

public class SplashActivity extends AppCompatActivity {
    private ActivitySplashBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private boolean hasNavigated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 🔥 LOTTIE LOOP (BEAUTIFUL)
        binding.animationView.setSpeed(1.2f);
        binding.animationView.setRepeatCount(999);

        // 🔥 CHECK AUTH STATUS IMMEDIATELY
        checkAuthStatus();
    }

    private void checkAuthStatus() {
        if (mAuth.getCurrentUser() != null) {
            checkRoleAndNavigate();
        } else {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (!isFinishing() && !hasNavigated) {
                    hasNavigated = true;
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                }
            }, 2500);
        }
    }

    private void checkRoleAndNavigate() {
        if (hasNavigated) return;

        String uid = mAuth.getCurrentUser().getUid();
        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {  // ✅ CORRECT SYNTAX
                    @Override
                    public void onSuccess(DocumentSnapshot snapshot) {
                        if (!hasNavigated) {
                            navigateByRole(snapshot);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (!hasNavigated) {
                        hasNavigated = true;
                        startActivity(new Intent(this, UserDashboardActivity.class));
                        finish();
                    }
                });
    }

    private void navigateByRole(DocumentSnapshot snapshot) {
        if (hasNavigated) return;
        hasNavigated = true;

        if (!snapshot.exists()) {
            startActivity(new Intent(this, LoginActivity.class));
            finishAffinity();
            return;
        }

        String role = snapshot.getString("role");
        Intent intent;

        switch (role != null ? role : "user") {
            case "admin":
                intent = new Intent(this, AdminDashboardActivity.class);
                break;
            case "quizmaster":
                intent = new Intent(this, QuizMasterDashboardActivity.class);
                break;
            default:
                intent = new Intent(this, UserDashboardActivity.class);
                break;
        }

        startActivity(intent);
        finishAffinity();
    }
}
