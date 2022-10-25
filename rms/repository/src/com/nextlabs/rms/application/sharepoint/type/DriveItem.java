package com.nextlabs.rms.application.sharepoint.type;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

public class DriveItem {

    @SerializedName("createdDateTime")
    private Date createdDateTime;
    @SerializedName("description")
    private String description;
    @SerializedName("id")
    private String id;
    @SerializedName("lastModifiedDateTime")
    private Date lstModifiedTime;
    @SerializedName("name")
    private String name;
    @SerializedName("webUrl")
    private String webUrl;
    @SerializedName("driveType")
    private String driveType;
    @SerializedName("createdBy")
    private DriveCreatedBy createdBy;
    @SerializedName("lastModifiedBy")
    private DriveLastModifiedBy lastModifiedBy;
    @SerializedName("owner")
    private DriveOwner owner;
    @SerializedName("quota")
    private DriveQuota quota;

    public Date getCreatedDateTime() {
        return createdDateTime;
    }

    public void setCreatedDateTime(Date createdDateTime) {
        this.createdDateTime = createdDateTime;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getLstModifiedTime() {
        return lstModifiedTime;
    }

    public void setLstModifiedTime(Date lstModifiedTime) {
        this.lstModifiedTime = lstModifiedTime;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWebUrl() {
        return webUrl;
    }

    public void setWebUrl(String webUrl) {
        this.webUrl = webUrl;
    }

    public String getDriveType() {
        return driveType;
    }

    public void setDriveType(String driveType) {
        this.driveType = driveType;
    }

    public DriveCreatedBy getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(DriveCreatedBy createdBy) {
        this.createdBy = createdBy;
    }

    public DriveLastModifiedBy getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(DriveLastModifiedBy lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public DriveOwner getOwner() {
        return owner;
    }

    public void setOwner(DriveOwner owner) {
        this.owner = owner;
    }

    public DriveQuota getQuota() {
        return quota;
    }

    public void setQuota(DriveQuota quota) {
        this.quota = quota;
    }
}
