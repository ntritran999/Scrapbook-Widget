package com.group04.scrapbookwidget.data.model;

import java.util.List;

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
        private String name;
        private String avatarUrl;
        private String seenAt;

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getAvatarUrl() { return avatarUrl; }
        public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
        public String getSeenAt() { return seenAt; }
        public void setSeenAt(String seenAt) { this.seenAt = seenAt; }
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
}
