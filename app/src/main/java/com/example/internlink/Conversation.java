package com.example.internlink;

public class Conversation {
    private String chatId;
    private String otherUserId;
    private String otherUserName;
    private String otherUserRole;
    private String otherUserLogoUrl;
    private String lastMessage;
    private long lastMessageTime;
    private String lastSenderId;
    private String projectId;
    private String projectTitle;
    private String applicationId;
    private int unreadCount;
    private boolean isTyping;

    public Conversation() {
        // Default constructor required for Firebase
    }

    // Getters and setters
    public String getChatId() {
        return chatId;
    }
    public boolean isTyping() {
        return isTyping;
    }

    public void setTyping(boolean typing) {
        isTyping = typing;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getOtherUserId() {
        return otherUserId;
    }

    public void setOtherUserId(String otherUserId) {
        this.otherUserId = otherUserId;
    }

    public String getOtherUserName() {
        return otherUserName;
    }

    public void setOtherUserName(String otherUserName) {
        this.otherUserName = otherUserName;
    }

    public String getOtherUserRole() {
        return otherUserRole;
    }

    public void setOtherUserRole(String otherUserRole) {
        this.otherUserRole = otherUserRole;
    }

    public String getOtherUserLogoUrl() {
        return otherUserLogoUrl;
    }

    public void setOtherUserLogoUrl(String otherUserLogoUrl) {
        this.otherUserLogoUrl = otherUserLogoUrl;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public long getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(long lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public String getLastSenderId() {
        return lastSenderId;
    }

    public void setLastSenderId(String lastSenderId) {
        this.lastSenderId = lastSenderId;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getProjectTitle() {
        return projectTitle;
    }

    public void setProjectTitle(String projectTitle) {
        this.projectTitle = projectTitle;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    // Helper methods
    public boolean isFromCurrentUser(String currentUserId) {
        return currentUserId != null && currentUserId.equals(lastSenderId);
    }

    public String getDisplayMessage() {
        if (lastMessage == null || lastMessage.trim().isEmpty()) {
            return "No messages yet";
        }

        // Handle special message types
        if (lastMessage.startsWith("📷")) {
            return "📷 Photo";
        } else if (lastMessage.startsWith("📄") || lastMessage.startsWith("📁") || lastMessage.startsWith("📎")) {
            return "📎 File";
        } else {
            return lastMessage;
        }
    }
}