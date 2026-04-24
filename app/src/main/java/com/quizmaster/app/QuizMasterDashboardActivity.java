package com.quizmaster.app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.quizmaster.app.databinding.ActivityQuizMasterDashboardBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuizMasterDashboardActivity extends AppCompatActivity {
    private ActivityQuizMasterDashboardBinding binding;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQuizMasterDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();

        // Navigate to Category Management
        binding.btnAddCategory.setOnClickListener(v ->
                startActivity(new Intent(this, CategoriesActivity.class)));

        // Show the highest score for each category across all users
        binding.btnViewHighScores.setOnClickListener(v -> fetchCategoryHighScores());

        // ✅ New: Navigate to Global Leaderboard
        binding.btnLeaderboard.setOnClickListener(v ->
                startActivity(new Intent(this, LeaderboardActivity.class)));

        binding.fabLogout.setOnClickListener(v -> logout());
    }

    private void fetchCategoryHighScores() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_leaderboard, null);
        RecyclerView rv = dialogView.findViewById(R.id.rvGlobalScores);
        ProgressBar loader = dialogView.findViewById(R.id.scoreLoader);
        TextView title = dialogView.findViewById(R.id.tvDialogTitle);

        title.setText("Category Top Records");
        rv.setLayoutManager(new LinearLayoutManager(this));

        List<ScoreModel> scoreList = new ArrayList<>();
        HighScoreAdapter adapter = new HighScoreAdapter(scoreList);
        rv.setAdapter(adapter);

        loader.setVisibility(View.VISIBLE);

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Close", null)
                .show();

        db.collection("scores")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    loader.setVisibility(View.GONE);
                    
                    Map<String, ScoreModel> categoryBestMap = new HashMap<>();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        ScoreModel score = doc.toObject(ScoreModel.class);
                        if (score != null && score.category != null) {
                            String category = score.category.toLowerCase().trim();
                            
                            if (!categoryBestMap.containsKey(category) || 
                                score.score > categoryBestMap.get(category).score) {
                                categoryBestMap.put(category, score);
                            }
                        }
                    }

                    scoreList.clear();
                    scoreList.addAll(categoryBestMap.values());
                    
                    Collections.sort(scoreList, (s1, s2) -> s1.category.compareToIgnoreCase(s2.category));

                    adapter.notifyDataSetChanged();

                    if (scoreList.isEmpty()) {
                        Toast.makeText(this, "No scores found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    loader.setVisibility(View.GONE);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("FIRESTORE_ERROR", e.getMessage());
                });
    }

    private void logout() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("🚪 Sign Out")
                .setMessage("Are you sure you want to logout? ")
                .setCancelable(false)
                .setPositiveButton("Logout", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(QuizMasterDashboardActivity.this, SplashActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }
}
