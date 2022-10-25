package com.nextlabs.rms.application.sharepoint.type;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

public class SharePointItem {

    @SerializedName("id")
    private String id;
    @SerializedName("name")
    private String name;
    @SerializedName("size")
    private long size;
    @SerializedName("lastModifiedDateTime")
    private Date lstModifiedTime;
    @SerializedName("createdDateTime")
    private Date createdDateTime;
    @SerializedName("@microsoft.graph.downloadUrl")
    private String downloadUrl;
    @SerializedName("folder")
    private SharePointFolder folder;
    @SerializedName("file")
    private SharePointFile file;
    @SerializedName("parentReference")
    private SharePointParentReference parentRef;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public Date getCreatedDateTime() {
        return createdDateTime;
    }

    public void setCreatedDateTime(Date createdDateTime) {
        this.createdDateTime = createdDateTime;
    }

    public Date getLstModifiedTime() {
        return lstModifiedTime;
    }

    public void setLstModifiedTime(Date lstModifiedTime) {
        this.lstModifiedTime = lstModifiedTime;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public SharePointFolder getFolder() {
        return folder;
    }

    public void setFolder(SharePointFolder folder) {
        this.folder = folder;
    }

    public SharePointFile getFile() {
        return file;
    }

    public void setFile(SharePointFile file) {
        this.file = file;
    }

    public SharePointParentReference getParentRef() {
        return parentRef;
    }

    public void setParentRef(SharePointParentReference parentRef) {
        this.parentRef = parentRef;
    }
}
