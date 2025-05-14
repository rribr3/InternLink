package com.example.internlink;

public class Announcement {
    private String id;
    private String title;
    private String body;
    private String date;
    private boolean isRead;
    private long timestamp;

    public Announcement(String id, String title, String body, String date, boolean isRead) {
        this.id = id;
        this.title = title;
        this.body = body;
        this.date = date;
        this.isRead = isRead;
    }

    // Required getters & setters
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getDate() { return date; }
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { this.isRead = read; }
}