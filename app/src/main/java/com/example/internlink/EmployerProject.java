package com.example.internlink;

public class EmployerProject {
    private String title;
    private int applicantsCount;
    private int positionsCount;
    private int iconResId;

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
}