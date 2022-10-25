package com.nextlabs.rms.application;

import java.util.Date;

public class FileUploadMetadata {

    private String fileId;
    private String name;
    private Long size;
    private Date createdDateTime;
    private Date lastModifiedTime;
    private String pathDisplay;
    private String fileNameWithTimeStamp;

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Date getCreatedDateTime() {
        return createdDateTime;
    }

    public void setCreatedDateTime(Date createdDateTime) {
        this.createdDateTime = createdDateTime;
    }

    public Date getLastModifiedTime() {
        return lastModifiedTime;
    }

    public void setLastModifiedTime(Date lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }

    public String getPathDisplay() {
        return pathDisplay;
    }

    public void setPathDisplay(String pathDisplay) {
        this.pathDisplay = pathDisplay;
    }

    public String getFileNameWithTimeStamp() {
        return fileNameWithTimeStamp;
    }

    public void setFileNameWithTimeStamp(String fileNameWithTimeStamp) {
        this.fileNameWithTimeStamp = fileNameWithTimeStamp;
    }
}
