package com.group04.scrapbookwidget.data.model;

public class ItemContent {
    public String photoUrl, caption;

    // Default constructor for JSON deserialization
    public ItemContent() {}

    public ItemContent(String photoUrl, String caption) {
        this.photoUrl = photoUrl;
        this.caption = caption;
    }

    public ItemContent(String photoUrl) {
        this.photoUrl = photoUrl;
        this.caption = null;
    }
}
