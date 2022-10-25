package com.nextlabs.rms.repository.onedrive;

import com.google.gson.annotations.SerializedName;

public class OneDrivePermission {

    @SerializedName("id")
    private String id;
    @SerializedName("roles")
    private String[] roles;
    @SerializedName("link")
    private OneDriveSharingLink link;
    @SerializedName("shareId")
    private String shareId;

    public String getShareId() {
        return shareId;
    }

    public void setShareId(String shareId) {
        this.shareId = shareId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String[] getRoles() {
        return roles;
    }

    public void setRoles(String[] roles) {
        this.roles = roles;
    }

    public OneDriveSharingLink getLink() {
        return link;
    }

    public void setLink(OneDriveSharingLink link) {
        this.link = link;
    }
}
