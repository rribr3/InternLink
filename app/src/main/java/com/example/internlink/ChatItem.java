package com.example.internlink;

public class ChatItem {
    private String chatId;
    private String otherUserId;
    private String otherUserName;
    private String otherUserType;
    private String lastMessage;
    private long lastMessageTime;
    private int unreadCount;
    private boolean lastMessageFromMe;

    // Getters and setters
    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }

    public String getOtherUserId() { return otherUserId; }
    public void setOtherUserId(String otherUserId) { this.otherUserId = otherUserId; }

    public String getOtherUserName() { return otherUserName; }
    public void setOtherUserName(String otherUserName) { this.otherUserName = otherUserName; }

    public String getOtherUserType() { return otherUserType; }
    public void setOtherUserType(String otherUserType) { this.otherUserType = otherUserType; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public long getLastMessageTime() { return lastMessageTime; }
    public void setLastMessageTime(long lastMessageTime) { this.lastMessageTime = lastMessageTime; }

    public int getUnreadCount() { return unreadCount; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }

    public boolean isLastMessageFromMe() { return lastMessageFromMe; }
    public void setLastMessageFromMe(boolean lastMessageFromMe) { this.lastMessageFromMe = lastMessageFromMe; }

    public String getFormattedTime() {
        long now = System.currentTimeMillis();
        long diff = now - lastMessageTime;

        if (diff < 60000) { // Less than 1 minute
            return "Just now";
        } else if (diff < 3600000) { // Less than 1 hour
            int minutes = (int) (diff / 60000);
            return minutes + " min ago";
        } else if (diff < 86400000) { // Less than 1 day
            int hours = (int) (diff / 3600000);
            return hours + " hr ago";
        } else if (diff < 172800000) { // Less than 2 days
            return "Yesterday";
        } else {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM d",
                    java.util.Locale.getDefault());
            return sdf.format(new java.util.Date(lastMessageTime));
        }
    }
}