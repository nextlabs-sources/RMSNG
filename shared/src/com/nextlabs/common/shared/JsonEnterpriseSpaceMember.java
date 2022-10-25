package com.nextlabs.common.shared;

public class JsonEnterpriseSpaceMember {

    private int userId;
    private String displayName;
    private String email;
    private Long creationTime;
    private String picture;

    public Long getCreationTime() {
        return creationTime;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }

    public int getUserId() {
        return userId;
    }

    public void setCreationTime(Long creationTime) {
        this.creationTime = creationTime;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }
}
