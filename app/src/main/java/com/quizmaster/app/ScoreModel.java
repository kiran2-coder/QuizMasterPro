package com.quizmaster.app;

import com.google.firebase.Timestamp;

public class ScoreModel {
    public String userId, username, category, quizId;
    public float score; // 🔥 CHANGED: Support float for negative marking
    public int percentage;
    public int attempted, skipped;
    public Timestamp timestamp;

    // Required empty constructor for Firestore
    public ScoreModel() {}

    public ScoreModel(String userId, String username, String category, String quizId, float score, int percentage, int attempted, int skipped) {
        this.userId = userId;
        this.username = username;
        this.category = category;
        this.quizId = quizId;
        this.score = score;
        this.percentage = percentage;
        this.attempted = attempted;
        this.skipped = skipped;
    }
}
