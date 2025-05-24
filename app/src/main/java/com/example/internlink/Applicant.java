package com.example.internlink;

public class Applicant {
    private String name;
    private String position;
    private String status;
    private int profileImageResId;
    private String userId;
    private String projectId;
    private Long appliedDate;

    public Applicant(String name, String position, String status, int profileImageResId) {
        this.name = name;
        this.position = position;
        this.status = status;
        this.profileImageResId = profileImageResId;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getProfileImageResId() {
        return profileImageResId;
    }

    public void setProfileImageResId(int profileImageResId) {
        this.profileImageResId = profileImageResId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public Long getAppliedDate() {
        return appliedDate;
    }

    public void setAppliedDate(Long appliedDate) {
        this.appliedDate = appliedDate;
    }
}