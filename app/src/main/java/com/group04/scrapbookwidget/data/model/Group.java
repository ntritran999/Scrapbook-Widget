package com.group04.scrapbookwidget.data.model;

import com.google.gson.annotations.SerializedName;

public class Group {
    private String id;
    private String groupName;
    private String avatarUrl;
    private String createdBy;
    private Object createdAt; // Can be Long (Firestore) or String (ISO date from API)
    private Message latestMessage;
    private int unreadCount; // Added unreadCount field

    @SerializedName("latestPage")
    private ScrapbookPage latestPage;

    @SerializedName("defaultPage")
    private ScrapbookPage defaultPage;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Object getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Object createdAt) {
        this.createdAt = createdAt;
    }

    public Message getLatestMessage() {
        return latestMessage;
    }

    public void setLatestMessage(Message latestMessage) {
        this.latestMessage = latestMessage;
    }

    public ScrapbookPage getLatestPage() {
        return latestPage;
    }

    public void setLatestPage(ScrapbookPage latestPage) {
        this.latestPage = latestPage;
    }

    public ScrapbookPage getDefaultPage() {
        return defaultPage;
    }

    public void setDefaultPage(ScrapbookPage defaultPage) {
        this.defaultPage = defaultPage;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }
}
