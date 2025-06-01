package com.example.internlink;

public class CompanyFeedback {
    private String feedbackId;
    private String studentId;
    private String studentName;
    private String companyId;
    private String projectId;
    private String projectTitle;
    private float rating;
    private String comment;
    private long timestamp;
    private String studentProfileUrl;

    public CompanyFeedback() {
        // Required empty constructor for Firebase
    }

    public CompanyFeedback(String studentId, String studentName, String companyId,
                           String projectId, String projectTitle, float rating,
                           String comment, long timestamp, String studentProfileUrl) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.companyId = companyId;
        this.projectId = projectId;
        this.projectTitle = projectTitle;
        this.rating = rating;
        this.comment = comment;
        this.timestamp = timestamp;
        this.studentProfileUrl = studentProfileUrl;
    }

    // Getters and Setters
    public String getFeedbackId() { return feedbackId; }
    public void setFeedbackId(String feedbackId) { this.feedbackId = feedbackId; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getCompanyId() { return companyId; }
    public void setCompanyId(String companyId) { this.companyId = companyId; }

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

    public String getStudentProfileUrl() { return studentProfileUrl; }
    public void setStudentProfileUrl(String studentProfileUrl) { this.studentProfileUrl = studentProfileUrl; }
}