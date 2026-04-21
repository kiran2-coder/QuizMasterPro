package com.quizmaster.app;

import com.google.firebase.Timestamp;

public class ScoreModel {
    public String userId, username, category, quizId;
    public int score, percentage;
    public Timestamp timestamp;

    // Required empty constructor for Firestore
    public ScoreModel() {}

    public ScoreModel(String userId, String username, String category, String quizId, int score, int percentage) {
        this.userId = userId;
        this.username = username;
        this.category = category;
        this.quizId = quizId;
        this.score = score;
        this.percentage = percentage;
    }
}
