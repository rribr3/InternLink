package com.example.internlink;

public class EmployerProject {
    private String projectId;
    private String title;
    private int applicantsCount;
    private int positionsCount;
    private int iconResId;
    private String status; // ✅ Add this field


    // Default constructor (needed for Firebase)
    public EmployerProject() {
    }

    // Constructor with parameters
    public EmployerProject(String title, int applicantsCount, int positionsCount, int iconResId) {
        this.title = title;
        this.applicantsCount = applicantsCount;
        this.positionsCount = positionsCount;
        this.iconResId = iconResId;
    }

    // ✅ Getters
    public String getProjectId() {
        return projectId;
    }

    public String getTitle() {
        return title;
    }

    public int getApplicantsCount() {
        return applicantsCount;
    }

    public int getPositionsCount() {
        return positionsCount;
    }

    public int getIconResId() {
        return iconResId;
    }

    // ✅ Setters (needed for refresh functionality)
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setApplicantsCount(int applicantsCount) {
        this.applicantsCount = applicantsCount;
    }

    public void setPositionsCount(int positionsCount) {
        this.positionsCount = positionsCount;
    }

    public void setIconResId(int iconResId) {
        this.iconResId = iconResId;
    }

    // ✅ Utility methods for better object handling
    @Override
    public String toString() {
        return "EmployerProject{" +
                "projectId='" + projectId + '\'' +
                ", title='" + title + '\'' +
                ", applicantsCount=" + applicantsCount +
                ", positionsCount=" + positionsCount +
                ", iconResId=" + iconResId +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        EmployerProject that = (EmployerProject) obj;
        return projectId != null ? projectId.equals(that.projectId) : that.projectId == null;
    }

    @Override
    public int hashCode() {
        return projectId != null ? projectId.hashCode() : 0;
    }

    // ✅ Add getter and setter for status
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // ✅ Method to check if project is completed
    public boolean isCompleted() {
        return "completed".equals(status);
    }

    // ✅ Method to check if project is active
    public boolean isActive() {
        return "approved".equals(status);
    }

    // ✅ Method to check if project is pending
    public boolean isPending() {
        return "pending".equals(status);
    }

    // Existing getters and setters...
}