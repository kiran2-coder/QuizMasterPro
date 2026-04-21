package com.quizmaster.app;

import java.util.HashMap;
import java.util.Map;

public class QuestionModel {
    public String id;
    public String question;
    public Map<String, String> options;  // ← BACKWARD COMPATIBLE
    public String correctAnswer;
    public long timestamp;

    // Individual option fields (for QuizActivity)
    public String optionA, optionB, optionC, optionD;

    public QuestionModel() {}

    // 🔥 BACKWARD COMPATIBLE: For AddQuestionsActivity
    public QuestionModel(String id, String question, Map<String, String> options, String correctAnswer) {
        this.id = id;
        this.question = question;
        this.options = options;
        this.correctAnswer = correctAnswer;
        this.timestamp = System.currentTimeMillis();

        // Auto-populate individual fields
        this.optionA = options.get("A");
        this.optionB = options.get("B");
        this.optionC = options.get("C");
        this.optionD = options.get("D");
    }

    // 🔥 FOR QuizActivity (6 individual params)
    public QuestionModel(String question, String optionA, String optionB, String optionC,
                         String optionD, String correctAnswer) {
        this.question = question;
        this.optionA = optionA;
        this.optionB = optionB;
        this.optionC = optionC;
        this.optionD = optionD;
        this.correctAnswer = correctAnswer;
        this.timestamp = System.currentTimeMillis();

        // Convert to Map for backward compatibility
        this.options = new HashMap<>();
        this.options.put("A", optionA);
        this.options.put("B", optionB);
        this.options.put("C", optionC);
        this.options.put("D", optionD);
    }
}
