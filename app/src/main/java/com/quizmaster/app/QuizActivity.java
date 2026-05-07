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
    private float score = 0; // 🔥 CHANGED: Support float for negative marking
    private int attemptedCount = 0;
    private int skippedCount = 0;
    private CountDownTimer quizTimer;
    private long totalQuizTimeMs;
    private boolean isQuizActive = true;
    private boolean isQuizEnded = false;
    private String categoryId;

    // 🔥 NEGATIVE MARKING SETTINGS
    private boolean isNegativeMarkingEnabled = false;
    private float negativeMarks = 0.0f;

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
        
        // 🔥 LOAD NEGATIVE MARKING SETTINGS
        isNegativeMarkingEnabled = getIntent().getBooleanExtra("negativeMarking", false);
        negativeMarks = getIntent().getFloatExtra("negativeMarks", 0.0f);

        if (categoryId == null) {
            Toast.makeText(this, "Invalid category!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 2. Setup Toolbar with Category Name
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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
                if (isQuizActive) {
                    isQuizActive = false;
                    skippedCount++;
                    highlightCorrectAnswer();
                    new Handler(Looper.getMainLooper()).postDelayed(() -> showResults(), 2000);
                } else {
                    showResults();
                }
            }
        }.start();
    }

    private void showQuestion(int index) {
        if (isQuizEnded) return;

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

        binding.btnSkip.setVisibility(View.VISIBLE);
        binding.btnSkip.setEnabled(true);

        switch (question.correctAnswer.toUpperCase().trim()) {
            case "A": currentCorrectText = question.optionA; break;
            case "B": currentCorrectText = question.optionB; break;
            case "C": currentCorrectText = question.optionC; break;
            case "D": currentCorrectText = question.optionD; break;
            default: currentCorrectText = ""; break;
        }

        List<String> options = new ArrayList<>();
        options.add(question.optionA);
        options.add(question.optionB);
        options.add(question.optionC);
        options.add(question.optionD);
        Collections.shuffle(options);

        for (int i = 0; i < optionButtons.size(); i++) {
            MaterialButton btn = optionButtons.get(i);
            btn.setText(options.get(i));
            btn.setVisibility(View.VISIBLE);
            btn.setEnabled(true);
            btn.setStrokeColorResource(R.color.border_color);
            btn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, android.R.color.transparent)));
            btn.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        }
    }

    private void setupClickListeners() {
        for (MaterialButton btn : optionButtons) {
            btn.setOnClickListener(v -> checkAnswer(btn));
        }

        binding.btnSkip.setOnClickListener(v -> {
            if (!isQuizActive) return;
            isQuizActive = false;
            skippedCount++;
            highlightCorrectAnswer();
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                showQuestion(currentQuestionIndex + 1);
            }, 1500);
        });
    }

    private void checkAnswer(MaterialButton selectedBtn) {
        if (!isQuizActive) return;

        isQuizActive = false;
        attemptedCount++;
        binding.btnSkip.setEnabled(false);
        for (MaterialButton btn : optionButtons) btn.setEnabled(false);

        String selectedText = selectedBtn.getText().toString();
        boolean isCorrect = selectedText.equals(currentCorrectText);

        if (isCorrect) {
            score += 1.0f; // Add 1 mark for correct
            selectedBtn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.success)));
            selectedBtn.setTextColor(ContextCompat.getColor(this, R.color.white));
        } else {
            // 🔥 APPLY NEGATIVE MARKING
            if (isNegativeMarkingEnabled) {
                score -= negativeMarks;
                // Ensure score doesn't go below 0
                if (score < 0) score = 0;
            }
            
            selectedBtn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.error)));
            selectedBtn.setTextColor(ContextCompat.getColor(this, R.color.white));
            highlightCorrectAnswer();
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            showQuestion(currentQuestionIndex + 1);
        }, 1500);
    }

    private void highlightCorrectAnswer() {
        for (MaterialButton btn : optionButtons) {
            btn.setEnabled(false);
            if (btn.getText().toString().equals(currentCorrectText)) {
                btn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.success)));
                btn.setTextColor(ContextCompat.getColor(this, R.color.white));
            }
        }
        binding.btnSkip.setEnabled(false);
    }

    private void showResults() {
        if (isQuizEnded) return;
        isQuizEnded = true;

        if (quizTimer != null) quizTimer.cancel();
        isQuizActive = false;
        
        // Percentage based on correct answers (score might be lower due to negative marking)
        // But usually percentage is (Final Score / Max Score) * 100
        int percentage = questions.isEmpty() ? 0 : (int) ((score * 100.0) / questions.size());
        if (percentage < 0) percentage = 0;

        String scoreDisplay = String.format("%.2f", score).replaceAll("\\.00$", "");

        String resultText = "Quiz Complete! 🎉\n\n" +
                "Final Score: " + scoreDisplay + "/" + questions.size() + " (" + percentage + "%)\n" +
                "Attempted: " + attemptedCount + "\n" +
                "Skipped: " + skippedCount;
        
        if (isNegativeMarkingEnabled) {
            resultText += "\n(Negative Marking: -" + negativeMarks + " per wrong)";
        }
        
        binding.tvQuestion.setText(resultText);

        saveScoreToLeaderboard(categoryId, score, percentage);

        hideAllButtons();
        showResultButtons();
    }

    private void showResultButtons() {
        binding.btnOption1.setText("🏆 Leaderboard");
        binding.btnOption1.setVisibility(View.VISIBLE);
        binding.btnOption1.setEnabled(true);
        binding.btnOption1.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary)));
        binding.btnOption1.setTextColor(ContextCompat.getColor(this, R.color.white));
        binding.btnOption1.setOnClickListener(v -> startActivity(new Intent(this, LeaderboardActivity.class)));

        binding.btnOption2.setText("🏠 Home");
        binding.btnOption2.setVisibility(View.VISIBLE);
        binding.btnOption2.setEnabled(true);
        binding.btnOption2.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.secondary_teal)));
        binding.btnOption2.setTextColor(ContextCompat.getColor(this, R.color.white));

        binding.btnOption2.setOnClickListener(v -> {
            Intent intent = new Intent(QuizActivity.this, UserDashboardActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void saveScoreToLeaderboard(String categoryId, float finalScore, int percentage) {
        String currentUserId = FirebaseAuth.getInstance().getUid();
        String categoryName = getIntent().getStringExtra("categoryName");

        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String realName = documentSnapshot.getString("name");
                    if (realName == null) realName = "Anonymous";

                    ScoreModel scoreModel = new ScoreModel(
                            currentUserId,
                            realName,
                            categoryName != null ? categoryName : "General",
                            categoryId,
                            finalScore,
                            percentage,
                            attemptedCount,
                            skippedCount
                    );
                    scoreModel.timestamp = com.google.firebase.Timestamp.now();

                    String docId = currentUserId + "_" + categoryId;

                    db.collection("scores").document(docId).get().addOnSuccessListener(doc -> {
                        float existingScore = 0;
                        if (doc.exists() && doc.contains("score")) {
                            existingScore = doc.getDouble("score").floatValue();
                        }

                        if (finalScore > existingScore) {
                            db.collection("scores").document(docId).set(scoreModel);
                        }
                    });

                    db.collection("leaderboards")
                            .document(currentUserId + "_" + System.currentTimeMillis())
                            .set(scoreModel);
                });
    }

    private void hideAllButtons() {
        for (MaterialButton btn : optionButtons) btn.setVisibility(View.GONE);
        binding.btnSkip.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (quizTimer != null) quizTimer.cancel();
    }
}
