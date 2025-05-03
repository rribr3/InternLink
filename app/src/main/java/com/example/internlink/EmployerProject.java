package com.example.internlink;

public class EmployerProject {
    private String title;
    private int applicantsCount;
    private int positionsCount;
    private int iconResId;
    private int positions;
    private int applicants;

    public EmployerProject(String title, int applicantsCount, int positionsCount, int iconResId) {
        this.title = title;
        this.applicantsCount = applicantsCount;
        this.positionsCount = positionsCount;
        this.iconResId = iconResId;
    }

    // Getters
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
    public int getPositions() {
        return positions;
    }

    public void setPositions(int positions) {
        this.positions = positions;
    }
    public int getApplicants() {
        return applicants;
    }

    public void setApplicants(int applicants) {
        this.applicants = applicants;
    }
}