package com.quizmaster.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.quizmaster.app.databinding.ActivityUserDashboardBinding;
import java.util.List;

public class UserDashboardActivity extends AppCompatActivity {
    private ActivityUserDashboardBinding binding;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();

        // 🔥 Fetch and Display Real Name from Firestore
        loadUserProfile();

        // --- Button Click Listeners ---

        binding.btnStartQuiz.setOnClickListener(v ->
                startActivity(new Intent(this, QuizSelectionActivity.class)));

        binding.btnLeaderboard.setOnClickListener(v ->
                startActivity(new Intent(this, LeaderboardActivity.class)));

        // ✅ Fixed: Removed the Phase 5 Toast listener and kept only the Dialog listener
       // binding.btnHighScores.setOnClickListener(v -> showHighScoresDialog());

        binding.btnProfile.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));

        binding.fabLogout.setOnClickListener(v -> logout());
    }

    private void loadUserProfile() {
        String currentUserId = FirebaseAuth.getInstance().getUid();

        if (currentUserId != null) {
            db.collection("users").document(currentUserId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            // 1. Map the document to your UserModel
                            UserModel user = documentSnapshot.toObject(UserModel.class);

                            // 2. Safely check the 'name' field from the model
                            if (user != null && user.name != null) {
                                binding.tvWelcome.setText("Welcome " + user.name + "!");
                            } else {
                                // Fallback if 'name' is missing but document exists
                                binding.tvWelcome.setText("Welcome User!");
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        binding.tvWelcome.setText("Welcome Player!");
                    });
        }
    }

//    private void showHighScoresDialog() {
//        // 1. Setup RecyclerView for the Dialog
//        RecyclerView recyclerView = new RecyclerView(this);
//        recyclerView.setLayoutManager(new LinearLayoutManager(this));
//        // Simple padding for better UI inside the dialog
//        recyclerView.setPadding(30, 30, 30, 30);
//        recyclerView.setClipToPadding(false);
//
//        AlertDialog dialog = new AlertDialog.Builder(this)
//                .setTitle("🏆 Your Category Bests")
//                .setView(recyclerView)
//                .setPositiveButton("Close", null)
//                .create(); // Use .create() so we can show it after data loads or immediately
//
//        dialog.show();
//
//        // 2. Query Firestore based on your updated Rules
//        String currentUid = FirebaseAuth.getInstance().getUid();
//        if (currentUid == null) return;
//
//
//
//        db.collection("scores")
//                .whereEqualTo("userId", currentUid) // This filter is REQUIRED by your security rules
//                .get()
//                .addOnSuccessListener(queryDocumentSnapshots -> {
//                    List<ScoreModel> list = queryDocumentSnapshots.toObjects(ScoreModel.class);
//
//                    if (list.isEmpty()) {
//                        Toast.makeText(this, "No scores yet. Take a quiz!", Toast.LENGTH_SHORT).show();
//                        dialog.dismiss();
//                    } else {
//                        recyclerView.setAdapter(new HighScoreAdapter(list));
//                    }
//                })
//                .addOnFailureListener(e -> {
//                    Toast.makeText(this, "Failed to load: " + e.getMessage(), Toast.LENGTH_SHORT).show();
//                    dialog.dismiss();
//                });
//    }

    private void logout() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("🚪 Sign Out")
                .setMessage("Are you sure you want to logout? ")
                .setCancelable(false) // Prevents closing if user clicks outside the box
                .setPositiveButton("Logout", (dialog, which) -> {
                    // 1. Sign out from Firebase
                    FirebaseAuth.getInstance().signOut();

                    // 2. Redirect to Splash (or Login) Activity
                    Intent intent = new Intent(UserDashboardActivity.this, SplashActivity.class);

                    // 3. Clear the backstack so the user can't "Go Back" to the dashboard
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                    startActivity(intent);
                    finish(); // Close current activity
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss(); // Just close the dialog
                })
                .show();
    }
}