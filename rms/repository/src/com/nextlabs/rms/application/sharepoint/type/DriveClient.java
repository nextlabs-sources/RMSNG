package com.nextlabs.rms.application.sharepoint.type;

import com.google.gson.annotations.SerializedName;

public class DriveClient {

    @SerializedName("email")
    private String email;
    @SerializedName("id")
    private String id;
    @SerializedName("displayName")
    private String displayName;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
