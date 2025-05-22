package com.example.internlink;

import androidx.annotation.NonNull;

import java.util.Objects;

public class Application {
    private String applicationId;
    private String projectId;
    private String userId;
    private String companyId;
    private String status; // Submitted, Under Review, Shortlisted, Rejected, Interview Scheduled
    private long appliedDate;
    private String resumeUrl;
    private String notes;
    private Integer quizGrade;
    private boolean isReapplication;



    // Required empty constructor for Firebase
    public Application() {
    }

    public Application(String projectId, String userId, String companyId,
                       String status, long appliedDate, String resumeUrl, String notes, Integer quizGrade) {
        setProjectId(projectId);
        setUserId(userId);
        setCompanyId(companyId);
        setStatus(status);
        setAppliedDate(appliedDate);
        setResumeUrl(resumeUrl);
        setNotes(notes);
        setQuizGrade(quizGrade); // ✅ new field
    }

    public boolean isReapplication() {
        return isReapplication;
    }
    public void setReapplication(boolean isReapplication) {
        this.isReapplication = isReapplication;
    }

    public Integer getQuizGrade() {
        return quizGrade;
    }

    public void setQuizGrade(Integer quizGrade) {
        this.quizGrade = quizGrade;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getUserId() {
        return userId;
    }

    public String getCompanyId() {
        return companyId;
    }

    public String getStatus() {
        return status;
    }

    public long getAppliedDate() {
        return appliedDate;
    }

    public String getResumeUrl() {
        return resumeUrl;
    }

    public String getNotes() {
        return notes;
    }

    // Setters with validation
    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public void setProjectId(String projectId) {
        if (projectId == null || projectId.trim().isEmpty()) {
            throw new IllegalArgumentException("Project ID cannot be null or empty");
        }
        this.projectId = projectId;
    }

    public void setUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        this.userId = userId;
    }

    public void setCompanyId(String companyId) {
        if (companyId == null || companyId.trim().isEmpty()) {
            throw new IllegalArgumentException("Company ID cannot be null or empty");
        }
        this.companyId = companyId;
    }

    public void setStatus(String status) {
        if (!isValidStatus(status)) {
            throw new IllegalArgumentException("Invalid status value: " + status);
        }
        this.status = status;
    }

    public void setAppliedDate(long appliedDate) {
        if (appliedDate <= 0) {
            throw new IllegalArgumentException("Applied date must be positive");
        }
        this.appliedDate = appliedDate;
    }

    public void setResumeUrl(String resumeUrl) {
        // Resume URL can be null (for applications without resume requirement)
        this.resumeUrl = resumeUrl;
    }

    public void setNotes(String notes) {
        // Notes can be null
        this.notes = notes;
    }

    // Validation helper
    private boolean isValidStatus(String status) {
        return status != null &&
                (
                        status.equals("Pending") ||
                        status.equals("Shortlisted") ||
                        status.equals("Rejected") ||
                        status.equals("Accepted") ||
                        status.equals("Interview Scheduled"));
    }

    // For debugging
    @NonNull
    @Override
    public String toString() {
        return "Application{" +
                "applicationId='" + applicationId + '\'' +
                ", projectId='" + projectId + '\'' +
                ", userId='" + userId + '\'' +
                ", companyId='" + companyId + '\'' +
                ", status='" + status + '\'' +
                ", appliedDate=" + appliedDate +
                ", resumeUrl='" + resumeUrl + '\'' +
                ", notes='" + notes + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Application that = (Application) o;
        return appliedDate == that.appliedDate &&
                Objects.equals(applicationId, that.applicationId) &&
                Objects.equals(projectId, that.projectId) &&
                Objects.equals(userId, that.userId) &&
                Objects.equals(companyId, that.companyId) &&
                Objects.equals(status, that.status) &&
                Objects.equals(resumeUrl, that.resumeUrl) &&
                Objects.equals(notes, that.notes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(applicationId, projectId, userId, companyId, status, appliedDate, resumeUrl, notes);
    }

    // Builder pattern for fluent creation
    public static class Builder {
        private String projectId;
        private String userId;
        private String companyId;
        private String status = "Pending";
        private long appliedDate;
        private String resumeUrl;
        private String notes;
        private Integer quizGrade;

        public Builder setProjectId(String projectId) {
            this.projectId = projectId;
            return this;
        }

        public Builder setUserId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder setCompanyId(String companyId) {
            this.companyId = companyId;
            return this;
        }

        public Builder setStatus(String status) {
            this.status = status;
            return this;
        }

        public Builder setAppliedDate(long appliedDate) {
            this.appliedDate = appliedDate;
            return this;
        }

        public Builder setResumeUrl(String resumeUrl) {
            this.resumeUrl = resumeUrl;
            return this;
        }

        public Builder setNotes(String notes) {
            this.notes = notes;
            return this;
        }
        public Builder setQuizGrade(Integer quizGrade) {
            this.quizGrade = quizGrade;
            return this;
        }

        public Application build() {
            return new Application(projectId, userId, companyId, status, appliedDate, resumeUrl, notes, quizGrade);
        }
    }
}