package com.group04.scrapbookwidget.data.model;

public class JoinByLinkRequest {
    private final String inviteCode;

    public JoinByLinkRequest(String inviteCode) {
        this.inviteCode = inviteCode;
    }

    public String getInviteCode() {
        return inviteCode;
    }
}
