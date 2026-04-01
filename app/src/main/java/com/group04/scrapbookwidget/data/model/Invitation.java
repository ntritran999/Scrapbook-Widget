package com.group04.scrapbookwidget.data.model;

import com.google.gson.annotations.SerializedName;

public class Invitation {
    private String id;
    private String groupId;
    private String invitedUserId;
    private String invitedBy;
    private String status;
    private Object createdAt;
    
    @SerializedName("group")
    private Group group;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getInvitedUserId() { return invitedUserId; }
    public void setInvitedUserId(String invitedUserId) { this.invitedUserId = invitedUserId; }

    public String getInvitedBy() { return invitedBy; }
    public void setInvitedBy(String invitedBy) { this.invitedBy = invitedBy; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Object getCreatedAt() { return createdAt; }
    public void setCreatedAt(Object createdAt) { this.createdAt = createdAt; }

    public Group getGroup() { return group; }
    public void setGroup(Group group) { this.group = group; }
}
