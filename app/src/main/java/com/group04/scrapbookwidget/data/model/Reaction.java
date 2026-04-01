package com.group04.scrapbookwidget.data.model;

import com.google.gson.annotations.SerializedName;

public class Reaction {
    @SerializedName("id")
    private String userId;
    private String type = "like";
    public Reaction(String userId) {
        this.userId = userId;
    }
    public String getUserId() {
        return userId;
    }
    public String getType() {
        return  type;
    }
}
