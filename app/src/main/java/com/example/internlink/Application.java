package com.example.internlink;

import androidx.annotation.NonNull;
import android.util.Log;

import java.util.Objects;

public class Application {
    private String applicationId;
    private String projectId;
    private String userId;
    private String companyId;
    private String status; // Submitted, Under Review, Shortlisted, Rejected, Interview Scheduled
    private Object appliedDate; // ✅ Changed to Object to handle both long and string
    private String resumeUrl;
    private String notes;
    private Object quizGrade; // ✅ Changed to Object to handle both Integer and String
    private boolean isReapplication;
    private String interviewDate;
    private String interviewTime;
    private Object lastUpdated; // ✅ Changed to Object to handle both long and string
    private String parentApplicationId;

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
        setQuizGrade(quizGrade);
    }

    public String getParentApplicationId() {
        return parentApplicationId;
    }

    public void setParentApplicationId(String parentApplicationId) {
        this.parentApplicationId = parentApplicationId;
    }

    public void setInterviewDate(String interviewDate) {
        this.interviewDate = interviewDate;
    }

    public void setInterviewTime(String interviewTime) {
        this.interviewTime = interviewTime;
    }

    public String getInterviewDate() {
        return interviewDate;
    }

    public String getInterviewTime() {
        return interviewTime;
    }

    // ✅ SAFE GETTER: Handles both long and string types
    public long getLastUpdated() {
        if (lastUpdated instanceof Long) {
            return (Long) lastUpdated;
        } else if (lastUpdated instanceof String) {
            try {
                return Long.parseLong((String) lastUpdated);
            } catch (NumberFormatException e) {
                Log.w("Application", "Invalid lastUpdated format: " + lastUpdated);
                return System.currentTimeMillis(); // default value
            }
        } else if (lastUpdated instanceof Number) {
            return ((Number) lastUpdated).longValue();
        }
        return System.currentTimeMillis(); // default if null
    }

    // ✅ SAFE SETTER: Only one setter that accepts Object
    public void setLastUpdated(Object lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public boolean isReapplication() {
        return isReapplication;
    }

    public void setReapplication(boolean isReapplication) {
        this.isReapplication = isReapplication;
    }

    // ✅ SAFE GETTER: Handles both Integer and String types
    public Integer getQuizGrade() {
        if (quizGrade instanceof Integer) {
            return (Integer) quizGrade;
        } else if (quizGrade instanceof String) {
            try {
                return Integer.parseInt((String) quizGrade);
            } catch (NumberFormatException e) {
                Log.w("Application", "Invalid quizGrade format: " + quizGrade);
                return null; // return null for invalid grades
            }
        } else if (quizGrade instanceof Long) {
            return ((Long) quizGrade).intValue();
        } else if (quizGrade instanceof Number) {
            return ((Number) quizGrade).intValue();
        }
        return null; // default if null or invalid
    }

    // ✅ SAFE SETTER: Only one setter that accepts Object
    public void setQuizGrade(Object quizGrade) {
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

    // ✅ SAFE GETTER: Handles both long and string types
    public long getAppliedDate() {
        if (appliedDate instanceof Long) {
            return (Long) appliedDate;
        } else if (appliedDate instanceof String) {
            try {
                return Long.parseLong((String) appliedDate);
            } catch (NumberFormatException e) {
                Log.w("Application", "Invalid appliedDate format: " + appliedDate);
                return System.currentTimeMillis(); // default value
            }
        } else if (appliedDate instanceof Number) {
            return ((Number) appliedDate).longValue();
        }
        return System.currentTimeMillis(); // default if null
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
        // Map "Under Review" to "Shortlisted"
        if (status != null && status.equalsIgnoreCase("Under Review")) {
            this.status = "Shortlisted";
        } else if (status != null && (status.equals("Accepted") ||
                status.equals("Rejected") ||
                status.equals("Pending") ||
                status.equals("Shortlisted"))) {
            this.status = status;
        } else {
            throw new IllegalArgumentException("Invalid status value: " + status);
        }
    }

    // ✅ SAFE SETTER: Only one setter that accepts Object
    public void setAppliedDate(Object appliedDate) {
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
                (status.equals("Pending") ||
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
                ", appliedDate=" + getAppliedDate() + // Use getter for safe conversion
                ", resumeUrl='" + resumeUrl + '\'' +
                ", notes='" + notes + '\'' +
                ", quizGrade=" + getQuizGrade() + // Use getter for safe conversion
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Application that = (Application) o;
        return getAppliedDate() == that.getAppliedDate() && // Use getter
                Objects.equals(applicationId, that.applicationId) &&
                Objects.equals(projectId, that.projectId) &&
                Objects.equals(userId, that.userId) &&
                Objects.equals(companyId, that.companyId) &&
                Objects.equals(status, that.status) &&
                Objects.equals(resumeUrl, that.resumeUrl) &&
                Objects.equals(notes, that.notes) &&
                Objects.equals(getQuizGrade(), that.getQuizGrade()); // Use getter
    }

    @Override
    public int hashCode() {
        return Objects.hash(applicationId, projectId, userId, companyId, status,
                getAppliedDate(), resumeUrl, notes, getQuizGrade()); // Use getters
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