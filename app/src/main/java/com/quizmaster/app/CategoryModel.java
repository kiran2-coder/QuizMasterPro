package com.quizmaster.app;

public class CategoryModel {
    public String id;
    public String name;
    public String description; // 🔥 ADDED: Description field
    public String customId;     // "science"
    public int timeLimit;
    public int questionCount;
    public boolean negativeMarking;
    public float negativeMarks;

    public CategoryModel() {}  // Firestore needs this

    public CategoryModel(String name, String description, int timeLimit, boolean negativeMarking, float negativeMarks) {
        this.name = name;
        this.description = description;
        this.customId = name.toLowerCase().replaceAll("[^a-z0-9]", "");
        this.timeLimit = timeLimit;
        this.questionCount = 0;
        this.negativeMarking = negativeMarking;
        this.negativeMarks = negativeMarks;
    }
    
    // Legacy constructor
    public CategoryModel(String name, int timeLimit, boolean negativeMarking, float negativeMarks) {
        this(name, "", timeLimit, negativeMarking, negativeMarks);
    }
}
