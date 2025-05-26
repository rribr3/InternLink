package com.example.internlink;

public class Message {
    private String id;
    private String text;
    private String senderId;
    private String receiverId;
    private long timestamp;
    private String messageType; // "text", "image", "file", "system"
    private String status; // "sent", "delivered", "read"
    private String fileUrl;
    private String fileName;
    private long fileSize;

    // Default constructor required for Firebase
    public Message() {}

    public Message(String text, String senderId, String receiverId, long timestamp) {
        this.text = text;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.timestamp = timestamp;
        this.messageType = "text";
        this.status = "sent";
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public boolean isSentByCurrentUser(String currentUserId) {
        return senderId != null && senderId.equals(currentUserId);
    }

    public String getFormattedTime() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date(timestamp));
    }

    public String getFormattedDate() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date(timestamp));
    }

    public boolean isToday() {
        java.util.Calendar today = java.util.Calendar.getInstance();
        java.util.Calendar messageDate = java.util.Calendar.getInstance();
        messageDate.setTimeInMillis(timestamp);

        return today.get(java.util.Calendar.YEAR) == messageDate.get(java.util.Calendar.YEAR) &&
                today.get(java.util.Calendar.DAY_OF_YEAR) == messageDate.get(java.util.Calendar.DAY_OF_YEAR);
    }

    public boolean isYesterday() {
        java.util.Calendar yesterday = java.util.Calendar.getInstance();
        yesterday.add(java.util.Calendar.DAY_OF_YEAR, -1);
        java.util.Calendar messageDate = java.util.Calendar.getInstance();
        messageDate.setTimeInMillis(timestamp);

        return yesterday.get(java.util.Calendar.YEAR) == messageDate.get(java.util.Calendar.YEAR) &&
                yesterday.get(java.util.Calendar.DAY_OF_YEAR) == messageDate.get(java.util.Calendar.DAY_OF_YEAR);
    }

    public String getDateHeader() {
        if (isToday()) {
            return "Today";
        } else if (isYesterday()) {
            return "Yesterday";
        } else {
            return getFormattedDate();
        }
    }
}