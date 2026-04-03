package com.group04.scrapbookwidget.data.model;

import java.util.List;

public class TodayMemory {
    private List<String> taggedUsernames;
    private String photoUrl;
    private String createdAt;

    public List<String> getTaggedUsernames() {
        return taggedUsernames;
    }

    public void setTaggedUsernames(List<String> taggedUsernames) {
        this.taggedUsernames = taggedUsernames;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
