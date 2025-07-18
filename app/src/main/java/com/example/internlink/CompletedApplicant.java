package com.example.internlink;

public class CompletedApplicant {
    private String userId;
    private String name;
    private String email;
    private String profileUrl;
    private String projectId;
    private String certificateUrl;
    private String projectTitle;

    public CompletedApplicant(String userId, String name, String email, String profileUrl, String projectId, String projectTitle) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.profileUrl = profileUrl;
        this.projectId = projectId;
        this.projectTitle = projectTitle;
    }

    // Getters
    public String getProjectTitle() { return projectTitle; }
    public String getUserId() { return userId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getProfileUrl() { return profileUrl; }
    public String getProjectId() { return projectId; }
    public String getCertificateUrl() { return certificateUrl; }

    // Setter for certificate
    public void setCertificateUrl(String certificateUrl) {
        this.certificateUrl = certificateUrl;
    }
}