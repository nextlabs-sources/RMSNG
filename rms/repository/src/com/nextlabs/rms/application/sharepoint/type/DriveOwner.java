package com.nextlabs.rms.application.sharepoint.type;

import com.google.gson.annotations.SerializedName;

public class DriveOwner {

    @SerializedName("group")
    private DriveClient group;

    public DriveClient getGroup() {
        return group;
    }

    public void setGroup(DriveClient group) {
        this.group = group;
    }
}
