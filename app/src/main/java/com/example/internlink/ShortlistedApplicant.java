package com.example.internlink;

public class ShortlistedApplicant {
    private String userId;
    private String projectId;
    private String applicationId;
    private String name;
    private String degree;
    private String university;
    private String projectTitle;
    private String interviewDate;
    private String interviewTime;
    private String interviewMode;
    private String interviewLocation;
    private String interviewNotes;
    private String interviewStatus;
    private String interviewMethod;

    public ShortlistedApplicant() {}

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getInterviewMethod() {
        return interviewMethod;
    }

    public void setInterviewMethod(String interviewMethod) {
        this.interviewMethod = interviewMethod;
    }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getApplicationId() { return applicationId; }
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDegree() { return degree; }
    public void setDegree(String degree) { this.degree = degree; }

    public String getUniversity() { return university; }
    public void setUniversity(String university) { this.university = university; }

    public String getProjectTitle() { return projectTitle; }
    public void setProjectTitle(String projectTitle) { this.projectTitle = projectTitle; }

    public String getInterviewDate() { return interviewDate; }
    public void setInterviewDate(String interviewDate) { this.interviewDate = interviewDate; }

    public String getInterviewTime() { return interviewTime; }
    public void setInterviewTime(String interviewTime) { this.interviewTime = interviewTime; }

    public String getInterviewMode() { return interviewMode; }
    public void setInterviewMode(String interviewMode) { this.interviewMode = interviewMode; }

    public String getInterviewLocation() { return interviewLocation; }
    public void setInterviewLocation(String interviewLocation) { this.interviewLocation = interviewLocation; }

    public String getInterviewNotes() { return interviewNotes; }
    public void setInterviewNotes(String interviewNotes) { this.interviewNotes = interviewNotes; }

    public String getInterviewStatus() { return interviewStatus; }
    public void setInterviewStatus(String interviewStatus) { this.interviewStatus = interviewStatus; }

    // Helper methods
    public String getFormattedDegree() {
        StringBuilder formatted = new StringBuilder();
        if (degree != null && !degree.trim().isEmpty()) {
            formatted.append(degree.trim());
        }
        if (university != null && !university.trim().isEmpty()) {
            if (formatted.length() > 0) formatted.append(" at ");
            formatted.append(university.trim());
        }
        return formatted.length() > 0 ? formatted.toString() : "Student";
    }

    public boolean hasInterviewScheduled() {
        return interviewDate != null && !interviewDate.trim().isEmpty() &&
                interviewTime != null && !interviewTime.trim().isEmpty();
    }

    public boolean isUpcoming() {
        // Simple check - in real app, you'd compare with current date/time
        return hasInterviewScheduled() && "Scheduled".equals(interviewStatus);
    }
}