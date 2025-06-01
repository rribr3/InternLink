package com.example.internlink;

public class FeedbackSummary {
    private float averageRating;
    private int totalReviews;
    private int fiveStarCount;
    private int fourStarCount;
    private int threeStarCount;
    private int twoStarCount;
    private int oneStarCount;

    public FeedbackSummary() {
        // Required empty constructor
    }

    public FeedbackSummary(float averageRating, int totalReviews, int fiveStarCount,
                           int fourStarCount, int threeStarCount, int twoStarCount, int oneStarCount) {
        this.averageRating = averageRating;
        this.totalReviews = totalReviews;
        this.fiveStarCount = fiveStarCount;
        this.fourStarCount = fourStarCount;
        this.threeStarCount = threeStarCount;
        this.twoStarCount = twoStarCount;
        this.oneStarCount = oneStarCount;
    }

    // Getters and Setters
    public float getAverageRating() { return averageRating; }
    public void setAverageRating(float averageRating) { this.averageRating = averageRating; }

    public int getTotalReviews() { return totalReviews; }
    public void setTotalReviews(int totalReviews) { this.totalReviews = totalReviews; }

    public int getFiveStarCount() { return fiveStarCount; }
    public void setFiveStarCount(int fiveStarCount) { this.fiveStarCount = fiveStarCount; }

    public int getFourStarCount() { return fourStarCount; }
    public void setFourStarCount(int fourStarCount) { this.fourStarCount = fourStarCount; }

    public int getThreeStarCount() { return threeStarCount; }
    public void setThreeStarCount(int threeStarCount) { this.threeStarCount = threeStarCount; }

    public int getTwoStarCount() { return twoStarCount; }
    public void setTwoStarCount(int twoStarCount) { this.twoStarCount = twoStarCount; }

    public int getOneStarCount() { return oneStarCount; }
    public void setOneStarCount(int oneStarCount) { this.oneStarCount = oneStarCount; }
}