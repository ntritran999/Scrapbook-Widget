package com.group04.scrapbookwidget.data.model;

import com.google.gson.annotations.SerializedName;

public class LeaveGroupResponse {
    @SerializedName("removed")
    private boolean removed;
    
    @SerializedName("groupId")
    private String groupId;
    
    @SerializedName("userId")
    private String userId;
    
    @SerializedName("ownershipTransferredTo")
    private String ownershipTransferredTo;
    
    @SerializedName("groupDeleted")
    private boolean groupDeleted;

    public boolean isRemoved() {
        return removed;
    }

    public void setRemoved(boolean removed) {
        this.removed = removed;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getOwnershipTransferredTo() {
        return ownershipTransferredTo;
    }

    public void setOwnershipTransferredTo(String ownershipTransferredTo) {
        this.ownershipTransferredTo = ownershipTransferredTo;
    }

    public boolean isGroupDeleted() {
        return groupDeleted;
    }

    public void setGroupDeleted(boolean groupDeleted) {
        this.groupDeleted = groupDeleted;
    }
}
