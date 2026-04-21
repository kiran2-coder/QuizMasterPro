package com.quizmaster.app;

public class UserModel {
    public String id, username, email, role, status, name;
    public long createdAt;

    public UserModel() {}

    public UserModel(String id, String username, String email, String role, String status, String name, long createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.status = status;
        this.name = name;
        this.createdAt = createdAt;
    }
}
