package com.quizmaster.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
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

        // 🔥 NO TOOLBAR BACK BUTTON - DISABLED
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);  // HIDE back arrow
            getSupportActionBar().setDisplayShowHomeEnabled(false);
            getSupportActionBar().setTitle("🏆 TOP 10 LEADERBOARD");
        }

        setupRecyclerView();
        loadTopScores();

        // 🔥 ONLY THIS BUTTON WORKS → UserDashboardActivity
        binding.btnBack.setOnClickListener(v -> {
            startActivity(new Intent(this, UserDashboardActivity.class));
            finish();
        });
    }

    private void setupRecyclerView() {
        binding.rvLeaderboard.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LeaderboardAdapter(topScores);
        binding.rvLeaderboard.setAdapter(adapter);
    }



    private void loadTopScores() {
        binding.tvLoading.setText("Loading top scores...");

        db.collection("leaderboards")
                .orderBy("score", Query.Direction.DESCENDING)
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
                    binding.tvLoading.setVisibility(android.view.View.GONE);
                })
                .addOnFailureListener(e -> {
                    binding.tvLoading.setText("No scores yet");
                });
    }

    // 🔥 DISABLE TOOLBAR BACK - EMPTY METHOD
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);  // NO back arrow handling
    }
}
