package com.nextlabs.rms.application.sharepoint.type;

import com.google.gson.annotations.SerializedName;

public class SharePointParentReference {

    @SerializedName("driveId")
    private String driveId;
    @SerializedName("driveType")
    private String driveType;
    @SerializedName("id")
    private String id;
    @SerializedName("path")
    private String path;

    public String getDriveId() {
        return driveId;
    }

    public void setDriveId(String driveId) {
        this.driveId = driveId;
    }

    public String getDriveType() {
        return driveType;
    }

    public void setDriveType(String driveType) {
        this.driveType = driveType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
