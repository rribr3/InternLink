package com.example.internlink;

// StudentFeedback.java
public class StudentFeedback {
    private String feedbackId;
    private String companyId;
    private String companyName;
    private String studentId;
    private String projectId;
    private String projectTitle;
    private float rating;
    private String comment;
    private long timestamp;
    private String companyLogoUrl;

    public StudentFeedback() {
        // Required empty constructor for Firebase
    }

    public StudentFeedback(String companyId, String companyName, String studentId,
                           String projectId, String projectTitle, float rating,
                           String comment, long timestamp, String companyLogoUrl) {
        this.companyId = companyId;
        this.companyName = companyName;
        this.studentId = studentId;
        this.projectId = projectId;
        this.projectTitle = projectTitle;
        this.rating = rating;
        this.comment = comment;
        this.timestamp = timestamp;
        this.companyLogoUrl = companyLogoUrl;
    }

    // Getters and Setters
    public String getFeedbackId() { return feedbackId; }
    public void setFeedbackId(String feedbackId) { this.feedbackId = feedbackId; }

    public String getCompanyId() { return companyId; }
    public void setCompanyId(String companyId) { this.companyId = companyId; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getProjectTitle() { return projectTitle; }
    public void setProjectTitle(String projectTitle) { this.projectTitle = projectTitle; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getCompanyLogoUrl() { return companyLogoUrl; }
    public void setCompanyLogoUrl(String companyLogoUrl) { this.companyLogoUrl = companyLogoUrl; }
}