package com.quizmaster.app;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.quizmaster.app.databinding.ActivityLeaderboardBinding;
import java.util.ArrayList;
import java.util.List;

public class LeaderboardActivity extends AppCompatActivity {
    private ActivityLeaderboardBinding binding;
    private FirebaseFirestore db;
    private List<ScoreModel> topScores;
    private LeaderboardAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLeaderboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        topScores = new ArrayList<>();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setDisplayShowHomeEnabled(false);
            getSupportActionBar().setTitle("🏆 GLOBAL LEADERBOARD");
        }

        setupRecyclerView();
        loadTopScores();

        // 🔥 FIXED: Back button now checks user role before navigating
        binding.btnBack.setOnClickListener(v -> handleBackNavigation());
    }

    private void handleBackNavigation() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            finish();
            return;
        }

        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String role = documentSnapshot.getString("role");
                    if ("quizmaster".equalsIgnoreCase(role)) {
                        // Go back to QuizMaster Dashboard
                        finish(); 
                    } else if ("admin".equalsIgnoreCase(role)) {
                        // Go back to Admin Dashboard
                        finish();
                    } else {
                        // Default for users
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    // Fallback to simply closing the activity
                    finish();
                });
    }

    private void setupRecyclerView() {
        binding.rvLeaderboard.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LeaderboardAdapter(topScores);
        binding.rvLeaderboard.setAdapter(adapter);
    }

    private void loadTopScores() {
        binding.tvLoading.setVisibility(View.VISIBLE);
        binding.tvLoading.setText("Fetching Top 10 by Percentage...");

        db.collection("leaderboards")
                .orderBy("percentage", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    topScores.clear();
                    for (DocumentSnapshot doc : querySnapshot) {
                        ScoreModel score = doc.toObject(ScoreModel.class);
                        if (score != null) {
                            topScores.add(score);
                        }
                    }
                    adapter.notifyDataSetChanged();
                    binding.tvLoading.setVisibility(View.GONE);

                    if (topScores.isEmpty()) {
                        binding.tvLoading.setVisibility(View.VISIBLE);
                        binding.tvLoading.setText("No scores found yet. Be the first!");
                    }
                })
                .addOnFailureListener(e -> {
                    binding.tvLoading.setVisibility(View.VISIBLE);
                    binding.tvLoading.setText("Failed to load leaderboard.");
                });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }
}
