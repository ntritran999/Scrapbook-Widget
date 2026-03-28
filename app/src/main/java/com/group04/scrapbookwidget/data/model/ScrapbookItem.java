package com.group04.scrapbookwidget.data.model;

public class ScrapbookItem {
    private String id, type, createdBy, createdAt;
    private ItemContent content;
    private Layout layout;

    // Default constructor for JSON deserialization
    public ScrapbookItem() {}

    // Constructor for creating new items
    public ScrapbookItem(String type, String createdBy, ItemContent content, Layout layout) {
        this.type = type;
        this.createdBy = createdBy;
        this.content = content;
        this.layout = layout;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public ItemContent getContent() {
        return content;
    }

    public Layout getLayout() {
        return layout;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public void setContent(ItemContent content) {
        this.content = content;
    }

    public void setLayout(Layout layout) {
        this.layout = layout;
    }
}
