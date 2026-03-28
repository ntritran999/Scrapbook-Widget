package com.group04.scrapbookwidget.data.model;

import com.google.gson.annotations.SerializedName;

public class User {
    @SerializedName("id")
    private String id;
    @SerializedName("uid")
    private String uid;
    @SerializedName("email")
    private String email;
    @SerializedName("name")
    private String name;
    @SerializedName("displayName")
    private String displayName;
    @SerializedName("username")
    private String username;
    @SerializedName("nickname")
    private String nickname;
    @SerializedName("password")
    private String password;
    @SerializedName("avatarUrl")
    private String avatarUrl;
    @SerializedName("status")
    private String status;
    @SerializedName("token")
    private String token;
    @SerializedName("idToken")
    private String idToken;

    public User() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { 
        if (name != null) return name;
        if (displayName != null) return displayName;
        if (nickname != null) return nickname;
        return username;
    }
    public void setName(String name) { this.name = name; }

    public String getDisplayName() { 
        if (displayName != null && !displayName.isEmpty()) return displayName;
        if (name != null && !name.isEmpty()) return name;
        if (nickname != null && !nickname.isEmpty()) return nickname;
        return username;
    }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getIdToken() { return idToken; }
    public void setIdToken(String idToken) { this.idToken = idToken; }
}
