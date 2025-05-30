package com.example.internlink;

public class Applicant {
    private String name;
    private String position;
    private String status;
    private int profileImageResId;
    private String userId;
    private String projectId;
    private Long appliedDate;
    private String cvUrl;

    // ✅ Default constructor (needed for Firebase)
    public Applicant() {
    }

    // Constructor with basic parameters
    public Applicant(String name, String position, String status, int profileImageResId) {
        this.name = name;
        this.position = position;
        this.status = status;
        this.profileImageResId = profileImageResId;
    }

    // ✅ Constructor with all parameters (useful for creating complete objects)
    public Applicant(String name, String position, String status, int profileImageResId,
                     String userId, String projectId, Long appliedDate, String cvUrl) {
        this.name = name;
        this.position = position;
        this.status = status;
        this.profileImageResId = profileImageResId;
        this.userId = userId;
        this.projectId = projectId;
        this.appliedDate = appliedDate;
        this.cvUrl = cvUrl;
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

    public String getCvUrl() {
        return cvUrl;
    }

    // ✅ Missing setter for cvUrl
    public void setCvUrl(String cvUrl) {
        this.cvUrl = cvUrl;
    }

    // ✅ Utility methods for better object handling
    @Override
    public String toString() {
        return "Applicant{" +
                "name='" + name + '\'' +
                ", position='" + position + '\'' +
                ", status='" + status + '\'' +
                ", userId='" + userId + '\'' +
                ", projectId='" + projectId + '\'' +
                ", appliedDate=" + appliedDate +
                ", cvUrl='" + cvUrl + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Applicant applicant = (Applicant) obj;
        return userId != null ? userId.equals(applicant.userId) : applicant.userId == null;
    }

    @Override
    public int hashCode() {
        return userId != null ? userId.hashCode() : 0;
    }

    // ✅ Helper methods for status checking (useful for filtering and UI logic)
    public boolean isPending() {
        return "Pending".equalsIgnoreCase(status);
    }

    public boolean isAccepted() {
        return "Accepted".equalsIgnoreCase(status);
    }

    public boolean isRejected() {
        return "Rejected".equalsIgnoreCase(status);
    }

    public boolean isShortlisted() {
        return "Shortlisted".equalsIgnoreCase(status);
    }

    // ✅ Helper method to check if CV is available
    public boolean hasCv() {
        return cvUrl != null && !cvUrl.trim().isEmpty();
    }

    // ✅ Helper method to get formatted applied date
    public String getFormattedAppliedDate() {
        if (appliedDate == null) {
            return "Unknown";
        }

        long now = System.currentTimeMillis();
        long diffMillis = now - appliedDate;

        long hours = diffMillis / (1000 * 60 * 60);
        if (hours < 24) {
            return hours + " hours ago";
        } else {
            long days = hours / 24;
            if (days < 30) {
                return days + (days == 1 ? " day ago" : " days ago");
            } else {
                long months = days / 30;
                return months + (months == 1 ? " month ago" : " months ago");
            }
        }
    }

    // ✅ Helper method to check if application is recent (within last 7 days)
    public boolean isRecentApplication() {
        if (appliedDate == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        long sevenDaysMillis = 7L * 24 * 60 * 60 * 1000;
        return (now - appliedDate) <= sevenDaysMillis;
    }
}