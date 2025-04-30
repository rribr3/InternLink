package com.example.internlink;

public class OnboardingItem {
    int imageResId;
    String title;
    String description;

    public OnboardingItem(int imageResId, String title, String description) {
        this.imageResId = imageResId;
        this.title = title;
        this.description = description;
    }
    public int getImageRes() {
        return imageResId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}

