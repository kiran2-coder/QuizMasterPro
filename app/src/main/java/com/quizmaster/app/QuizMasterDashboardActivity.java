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
import com.google.firebase.firestore.DocumentSnapshot; // 🔥 Added missing import
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.quizmaster.app.databinding.ActivityQuizMasterDashboardBinding;

import java.util.ArrayList;
import java.util.List;

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

        // ✅ Cleaned up: Removed the duplicate Toast listener
        binding.btnViewHighScores.setOnClickListener(v -> showCategoryPickerForScores());

        binding.fabLogout.setOnClickListener(v -> logout());
    }

    private void showCategoryPickerForScores() {
        // 1. Fetch Categories from Firestore
        db.collection("categories").get().addOnSuccessListener(queryDocumentSnapshots -> {
            List<String> categories = new ArrayList<>();

            // 🔥 Specified DocumentSnapshot type here
            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                String name = doc.getString("name");
                if (name != null) categories.add(name);
            }

            if (categories.isEmpty()) {
                Toast.makeText(this, "No categories found!", Toast.LENGTH_SHORT).show();
                return;
            }

            // 2. Show Category Picker Dialog
            String[] catArray = categories.toArray(new String[0]);
            new AlertDialog.Builder(this)
                    .setTitle("Select Category")
                    .setItems(catArray, (dialog, which) -> {
                        String selectedCategory = catArray[which];
                        fetchGlobalScoresForCategory(selectedCategory);
                    })
                    .show();
        }).addOnFailureListener(e -> Toast.makeText(this, "Error loading categories", Toast.LENGTH_SHORT).show());
    }

    private void fetchGlobalScoresForCategory(String categoryName) {
        // 1. Inflate the custom layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_leaderboard, null);
        RecyclerView rv = dialogView.findViewById(R.id.rvGlobalScores);
        ProgressBar loader = dialogView.findViewById(R.id.scoreLoader);
        TextView title = dialogView.findViewById(R.id.tvDialogTitle);

        title.setText("Leaderboard: " + categoryName);
        rv.setLayoutManager(new LinearLayoutManager(this));
        loader.setVisibility(View.VISIBLE);

        AlertDialog scoreDialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Close", null)
                .show();

        // 2. Query Firestore
        db.collection("scores")
                .whereEqualTo("category", categoryName)
                .orderBy("score", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    loader.setVisibility(View.GONE);
                    List<ScoreModel> scoreList = queryDocumentSnapshots.toObjects(ScoreModel.class);

                    if (scoreList.isEmpty()) {
                        Toast.makeText(this, "No scores found.", Toast.LENGTH_SHORT).show();
                        scoreDialog.dismiss();
                    } else {
                        // 🔥 Pass the list to your adapter
                        rv.setAdapter(new HighScoreAdapter(scoreList));
                    }
                })
                .addOnFailureListener(e -> {
                    loader.setVisibility(View.GONE);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void logout() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("🚪 Sign Out")
                .setMessage("Are you sure you want to logout? ")
                .setCancelable(false) // Prevents closing if user clicks outside the box
                .setPositiveButton("Logout", (dialog, which) -> {
                    // 1. Sign out from Firebase
                    FirebaseAuth.getInstance().signOut();

                    // 2. Redirect to Splash (or Login) Activity
                    Intent intent = new Intent(QuizMasterDashboardActivity.this, SplashActivity.class);

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