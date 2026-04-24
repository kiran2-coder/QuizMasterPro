package com.quizmaster.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.quizmaster.app.databinding.ActivityUserDashboardBinding;
import java.util.ArrayList;
import java.util.Collections;
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

        // ✅ Now implemented: Show user's own high scores in each category
        binding.btnHighScores.setOnClickListener(v -> showMyHighScoresDialog());

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
                            UserModel user = documentSnapshot.toObject(UserModel.class);
                            if (user != null && user.name != null) {
                                binding.tvWelcome.setText("Welcome " + user.name + "!");
                            } else {
                                binding.tvWelcome.setText("Welcome User!");
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        binding.tvWelcome.setText("Welcome Player!");
                    });
        }
    }

    private void showMyHighScoresDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_leaderboard, null);
        RecyclerView rv = dialogView.findViewById(R.id.rvGlobalScores);
        ProgressBar loader = dialogView.findViewById(R.id.scoreLoader);
        TextView title = dialogView.findViewById(R.id.tvDialogTitle);

        title.setText("My Personal Best Scores");
        rv.setLayoutManager(new LinearLayoutManager(this));

        List<ScoreModel> scoreList = new ArrayList<>();
        HighScoreAdapter adapter = new HighScoreAdapter(scoreList);
        rv.setAdapter(adapter);

        loader.setVisibility(View.VISIBLE);

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Close", null)
                .show();

        String currentUid = FirebaseAuth.getInstance().getUid();
        if (currentUid == null) return;

        // Fetch scores for current user. 
        // NOTE: We removed .orderBy() from the query to avoid needing a composite index in Firestore.
        // We sort the results manually in code instead.
        db.collection("scores")
                .whereEqualTo("userId", currentUid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    loader.setVisibility(View.GONE);
                    scoreList.clear();
                    List<ScoreModel> results = queryDocumentSnapshots.toObjects(ScoreModel.class);
                    
                    // Sort by percentage descending manually
                    Collections.sort(results, (s1, s2) -> Integer.compare(s2.percentage, s1.percentage));
                    
                    scoreList.addAll(results);
                    adapter.notifyDataSetChanged();

                    if (scoreList.isEmpty()) {
                        Toast.makeText(this, "No scores yet. Take a quiz!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    loader.setVisibility(View.GONE);
                    Toast.makeText(this, "Error loading scores: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void logout() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("🚪 Sign Out")
                .setMessage("Are you sure you want to logout? ")
                .setCancelable(false)
                .setPositiveButton("Logout", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(UserDashboardActivity.this, SplashActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }
}
