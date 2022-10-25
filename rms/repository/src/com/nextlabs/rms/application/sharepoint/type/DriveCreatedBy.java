package com.nextlabs.rms.application.sharepoint.type;

import com.google.gson.annotations.SerializedName;

public class DriveCreatedBy {

    @SerializedName("user")
    private DriveClient user;

    public DriveClient getUser() {
        return user;
    }

    public void setUser(DriveClient user) {
        this.user = user;
    }
}
