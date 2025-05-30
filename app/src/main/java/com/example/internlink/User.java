package com.example.internlink;

public class User {
    private String name;
    private String email;
    private String role;
    private String industry;
    private String logoUrl;
    private String userId;

    public User() {
        // Default constructor required for Firebase
    }

    public User(String name, String email, String role) {
        this.name = name;
        this.email = email;
        this.role = role;
    }

    // Getters and setters for the fields
    public String getName() {
        return name;
    }
    public String getIndustry() {
        return industry;
    }
    public String getUid() {
        return userId;
    }
    public String getLogoUrl() {
        return logoUrl;
    }
    public void setUid(String userId) {
        this.userId = userId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
