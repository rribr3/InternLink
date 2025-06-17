package com.example.internlink;

public class Announcement {
    private String id;
    private String title;
    private String message;
    private String date;
    private boolean isRead;
    private long timestamp;
    private String category;
    private String severity;
    private String reportId;
    private String priority;

    public Announcement() {}

    public Announcement(String id, String title, String message, String date, boolean isRead) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.date = date;
        this.isRead = isRead;
        this.timestamp = System.currentTimeMillis();
    }

    // Required getters & setters
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setBody(String message) { this.message = message; }
    public void setDate(String date) { this.date = date; }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getBody() { return message; }
    public String getDate() { return date; }
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { this.isRead = read; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

}