package com.quizmaster.app;

public class CategoryModel {
    public String id;
    public String name;
    public String customId;     // "science"
    public int timeLimit;
    public int questionCount;

    public CategoryModel() {}  // Firestore needs this

    public CategoryModel(String name, int timeLimit) {
        this.name = name;
        this.customId = name.toLowerCase().replaceAll("[^a-z0-9]", "");
        this.timeLimit = timeLimit;
        this.questionCount = 0;
    }
}

