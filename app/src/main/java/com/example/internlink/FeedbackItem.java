package com.example.internlink;

public class FeedbackItem {
    public String message;
    public String userType;
    public int rating;

    public FeedbackItem(String message, String userType, int rating) {
        this.message = message;
        this.userType = userType;
        this.rating = rating;
    }
}
