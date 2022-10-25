package com.nextlabs.rms.repository.onedrive;

import com.google.gson.annotations.SerializedName;

public class OneDriveDriveResponse {

    @SerializedName("owner")
    private OneDriveOwner owner;
    @SerializedName("id")
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public OneDriveOwner getOwner() {
        return owner;
    }

    public void setOwner(OneDriveOwner owner) {
        this.owner = owner;
    }
}
