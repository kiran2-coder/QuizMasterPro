package com.quizmaster.app;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.quizmaster.app.databinding.ActivityQuizBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import androidx.activity.OnBackPressedCallback;
import java.util.Map;
import java.util.UUID;
import com.google.firebase.firestore.SetOptions;

public class QuizActivity extends AppCompatActivity {
    private ActivityQuizBinding binding;
    private FirebaseFirestore db;
    private List<QuestionModel> questions;
    private int currentQuestionIndex = 0;
    private int score = 0;
    private CountDownTimer quizTimer;
    private long totalQuizTimeMs;
    private boolean isQuizActive = true;
    private String categoryId;

    // List to manage buttons collectively for styling resets
    private List<MaterialButton> optionButtons;
    private String currentCorrectText = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQuizBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 🔥 MODERN BACK PRESS HANDLING
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showExitConfirmationDialog();
            }
        });


        // 1. Get Data from Intent
        categoryId = getIntent().getStringExtra("categoryId");
        String categoryName = getIntent().getStringExtra("categoryName");
        int timeLimitMinutes = getIntent().getIntExtra("timeLimit", 15);

        if (categoryId == null) {
            Toast.makeText(this, "Invalid category!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 2. Setup Toolbar with Category Name
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            // Display "Science (15min)" or similar
            getSupportActionBar().setTitle(categoryName != null ? categoryName : "Quiz");

            binding.toolbar.setNavigationOnClickListener(v -> showExitConfirmationDialog());
        }

        // 3. Initialize UI & Buttons
        optionButtons = new ArrayList<>();
        optionButtons.add(binding.btnOption1);
        optionButtons.add(binding.btnOption2);
        optionButtons.add(binding.btnOption3);
        optionButtons.add(binding.btnOption4);

        db = FirebaseFirestore.getInstance();
        questions = new ArrayList<>();
        totalQuizTimeMs = timeLimitMinutes * 60L * 1000L;

        binding.tvQuestion.setText("🔄 Loading questions...");
        binding.tvTimer.setText(String.format("%02d:00", timeLimitMinutes));
        binding.tvProgress.setText("0/0");

        loadQuestions(categoryId);
        setupClickListeners();
    }

    private void loadQuestions(String categoryId) {
        // No need for "Loading" text updates here if it's instant,
        // but good to have as a fallback for slow networks.
        db.collection("categories").document(categoryId)
                .collection("questions")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    questions.clear();
                    for (DocumentSnapshot doc : querySnapshot) {
                        try {
                            QuestionModel question = doc.toObject(QuestionModel.class);
                            if (question != null && question.question != null) {
                                question.id = doc.getId();
                                questions.add(question);
                            }
                        } catch (Exception e) {
                            Log.e("QUIZ", "Parse error: " + e.getMessage());
                        }
                    }

                    if (questions.isEmpty()) {
                        binding.tvQuestion.setText("😞 No questions found!");
                        return;
                    }

                    Collections.shuffle(questions);

                    // 🔥 INSTANT START: No Handlers, no delays.
                    startQuiz();

                })
                .addOnFailureListener(e -> {
                    binding.tvQuestion.setText("❌ Load failed");
                    Log.e("QUIZ", "Error: " + e.getMessage());
                });
    }
    private void startQuiz() {
        startQuizTimer();
        showQuestion(0);
    }
    private void showExitConfirmationDialog() {
        // Use the AppCompat version for consistent styling
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Quit Quiz?");
        builder.setMessage("Your progress will be lost. Are you sure you want to exit?");
        builder.setCancelable(false);

        builder.setPositiveButton("Yes, Exit", (dialog, which) -> {
            if (quizTimer != null) quizTimer.cancel();
            finish();
        });

        builder.setNegativeButton("No, Stay", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    private void startQuizTimer() {
        quizTimer = new CountDownTimer(totalQuizTimeMs, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long minutes = millisUntilFinished / 1000 / 60;
                long seconds = (millisUntilFinished / 1000) % 60;
                binding.tvTimer.setText(String.format("%02d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                isQuizActive = false;
                showResults();
            }
        }.start();
    }

    private void showQuestion(int index) {
        if (index >= questions.size()) {
            quizTimer.cancel();
            showResults();
            return;
        }

        currentQuestionIndex = index;
        isQuizActive = true;
        QuestionModel question = questions.get(index);

        binding.tvQuestion.setText(question.question);
        binding.tvProgress.setText((index + 1) + "/" + questions.size());

        // --- MAPPING LOGIC START ---
        // Extract the text that matches the letter in the DB
        switch (question.correctAnswer.toUpperCase().trim()) {
            case "A": currentCorrectText = question.optionA; break;
            case "B": currentCorrectText = question.optionB; break;
            case "C": currentCorrectText = question.optionC; break;
            case "D": currentCorrectText = question.optionD; break;
            default: currentCorrectText = ""; break;
        }
        // --- MAPPING LOGIC END ---

        List<String> options = new ArrayList<>();
        options.add(question.optionA);
        options.add(question.optionB);
        options.add(question.optionC);
        options.add(question.optionD);
        Collections.shuffle(options);

        // Populate buttons as you already do...
        for (int i = 0; i < optionButtons.size(); i++) {
            MaterialButton btn = optionButtons.get(i);
            btn.setText(options.get(i));
            btn.setVisibility(View.VISIBLE);
            btn.setEnabled(true);
            // Reset colors
            btn.setStrokeColorResource(R.color.border_color);
            btn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, android.R.color.transparent)));
            btn.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        }
    }

    private void setupClickListeners() {
        for (MaterialButton btn : optionButtons) {
            btn.setOnClickListener(v -> checkAnswer(btn));
        }
    }

    private void checkAnswer(MaterialButton selectedBtn) {
        if (!isQuizActive) return;

        isQuizActive = false;
        for (MaterialButton btn : optionButtons) btn.setEnabled(false);

        String selectedText = selectedBtn.getText().toString();

        // Direct comparison with the mapped text
        boolean isCorrect = selectedText.equals(currentCorrectText);

        if (isCorrect) {
            score++;
            selectedBtn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.success)));
            selectedBtn.setTextColor(ContextCompat.getColor(this, R.color.white));
        } else {
            selectedBtn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.error)));
            selectedBtn.setTextColor(ContextCompat.getColor(this, R.color.white));

            // Highlight the right one by looking for the correct text
            for (MaterialButton btn : optionButtons) {
                if (btn.getText().toString().equals(currentCorrectText)) {
                    btn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.success)));
                    btn.setTextColor(ContextCompat.getColor(this, R.color.white));
                }
            }
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            showQuestion(currentQuestionIndex + 1);
        }, 1500);
    }

    private void showResults() {
        if (quizTimer != null) quizTimer.cancel();
        isQuizActive = false;
        int percentage = questions.isEmpty() ? 0 : (int) ((score * 100.0) / questions.size());

        binding.tvQuestion.setText("Quiz Complete! 🎉\nScore: " + score + "/" + questions.size() + " (" + percentage + "%)");


        saveScoreToLeaderboard(categoryId, score, percentage);

        hideAllButtons();
        showResultButtons();
    }

    private void showResultButtons() {
        binding.btnOption1.setText("🏆 Leaderboard");
        binding.btnOption1.setVisibility(View.VISIBLE);
        binding.btnOption1.setEnabled(true);
        // Navigation styling
        binding.btnOption1.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary)));
        binding.btnOption1.setTextColor(ContextCompat.getColor(this, R.color.white));
        binding.btnOption1.setOnClickListener(v -> startActivity(new Intent(this, LeaderboardActivity.class)));

        binding.btnOption2.setText("🏠 Home");
        binding.btnOption2.setVisibility(View.VISIBLE);
        binding.btnOption2.setEnabled(true);
        binding.btnOption2.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.secondary_teal)));
        binding.btnOption2.setTextColor(ContextCompat.getColor(this, R.color.white));

        // 🔥 REDIRECT TO USER DASHBOARD
        binding.btnOption2.setOnClickListener(v -> {
            Intent intent = new Intent(QuizActivity.this, UserDashboardActivity.class);
            // Clear the backstack so the user can't "Go Back" into the finished quiz
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish(); // Close the QuizActivity
        });
    }

    private void saveScoreToLeaderboard(String categoryId, int score, int percentage) {
        String currentUserId = FirebaseAuth.getInstance().getUid();
        String categoryName = getIntent().getStringExtra("categoryName"); // Get the human-readable name

        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String realName = documentSnapshot.getString("name");
                    if (realName == null) realName = "Anonymous";

                    // Create the data object using your ScoreModel
                    // We use the categoryName (e.g., "History") for the display,
                    // but categoryId for logic if needed.
                    ScoreModel scoreModel = new ScoreModel(
                            currentUserId,
                            realName,
                            categoryName != null ? categoryName : "General",
                            categoryId,
                            score,
                            percentage
                    );
                    scoreModel.timestamp = com.google.firebase.Timestamp.now();

                    // 🔥 HIGH SCORE LOGIC:
                    // We save to a specific document: "USERID_CATEGORYID"
                    // This will overwrite the previous score for this specific category.
                    String docId = currentUserId + "_" + categoryId;

                    // Inside saveScoreToLeaderboard
                    db.collection("scores").document(docId).get().addOnSuccessListener(doc -> {
                        int existingScore = 0;
                        if (doc.exists() && doc.contains("score")) {
                            existingScore = doc.getLong("score").intValue();
                        }

                        // Only update if the new score is higher
                        if (score > existingScore) {
                            db.collection("scores").document(docId).set(scoreModel);
                        }
                    });

                    // Keep your existing leaderboard code if you want a history of all attempts
                    db.collection("leaderboards")
                            .document(currentUserId + "_" + System.currentTimeMillis())
                            .set(scoreModel);
                });
    }

    private void hideAllButtons() {
        for (MaterialButton btn : optionButtons) btn.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (quizTimer != null) quizTimer.cancel();
    }

}