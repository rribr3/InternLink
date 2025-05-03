package com.example.internlink;

public class Applicant {
    private String name;
    private String position;
    private String status;
    private int profileImageResId;

    public Applicant(String name, String position, String status, int profileImageResId) {
        this.name = name;
        this.position = position;
        this.status = status;
        this.profileImageResId = profileImageResId;
    }

    // Getters
    public String getName() {
        return name;
    }

    public String getPosition() {
        return position;
    }

    public String getStatus() {
        return status;
    }

    public int getProfileImageResId() {
        return profileImageResId;
    }
}