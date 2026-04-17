package com.group04.scrapbookwidget.data.model;

import java.util.List;
import java.util.Objects;

public class Message {
    public enum Status {
        SENDING, SENT, FAILED
    }

    private String id;
    private String content;
    private String createdBy;
    private String createdAt;
    private String type;
    private String senderId;
    private String senderName;
    private String senderAvatar;
    private String time;
    private List<SeenBy> seenBy;
    private String seenByText;
    private Status status = Status.SENT; // Default to SENT for received/initial messages

    public static class SeenBy {
        private String id;
        private String userId; // Added userId field
        private String name;
        private String avatarUrl;
        private String seenAt;
        private String lastSeenMessageId; // Added lastSeenMessageId
        private String lastSeenAt; // Added lastSeenAt
        private int unreadCount; // Added unreadCount

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getAvatarUrl() { return avatarUrl; }
        public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
        public String getSeenAt() { return seenAt; }
        public void setSeenAt(String seenAt) { this.seenAt = seenAt; }
        public String getLastSeenMessageId() { return lastSeenMessageId; }
        public void setLastSeenMessageId(String lastSeenMessageId) { this.lastSeenMessageId = lastSeenMessageId; }
        public String getLastSeenAt() { return lastSeenAt; }
        public void setLastSeenAt(String lastSeenAt) { this.lastSeenAt = lastSeenAt; }
        public int getUnreadCount() { return unreadCount; }
        public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SeenBy seenBy = (SeenBy) o;
            return Objects.equals(userId, seenBy.userId) && 
                   Objects.equals(avatarUrl, seenBy.avatarUrl);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, avatarUrl);
        }
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public String getSenderAvatar() { return senderAvatar; }
    public void setSenderAvatar(String senderAvatar) { this.senderAvatar = senderAvatar; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public List<SeenBy> getSeenBy() { return seenBy; }
    public void setSeenBy(List<SeenBy> seenBy) { this.seenBy = seenBy; }
    public String getSeenByText() { return seenByText; }
    public void setSeenByText(String seenByText) { this.seenByText = seenByText; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return Objects.equals(id, message.id) &&
                Objects.equals(content, message.content) &&
                status == message.status &&
                Objects.equals(seenBy, message.seenBy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, content, status, seenBy);
    }
}
