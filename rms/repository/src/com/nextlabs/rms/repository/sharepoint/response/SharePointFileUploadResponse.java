package com.nextlabs.rms.repository.sharepoint.response;

import com.google.gson.annotations.SerializedName;

public class SharePointFileUploadResponse {

    @SerializedName("Length")
    private int length;
    @SerializedName("Name")
    private String name;
    @SerializedName("ServerRelativeUrl")
    private String serverRelativeUrl;
    @SerializedName("TimeCreated")
    private String createdTime;
    @SerializedName("TimeLastModified")
    private String lastModifiedTime;
    @SerializedName("Title")
    private String title;
    @SerializedName("UniqueId")
    private String uniqueId;

    public String getCreatedTime() {
        return createdTime;
    }

    public String getLastModifiedTime() {
        return lastModifiedTime;
    }

    public int getLength() {
        return length;
    }

    public String getName() {
        return name;
    }

    public String getServerRelativeUrl() {
        return serverRelativeUrl;
    }

    public String getTitle() {
        return title;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public void setCreatedTime(String createdTime) {
        this.createdTime = createdTime;
    }

    public void setLastModifiedTime(String lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setServerRelativeUrl(String serverRelativeUrl) {
        this.serverRelativeUrl = serverRelativeUrl;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }
}
