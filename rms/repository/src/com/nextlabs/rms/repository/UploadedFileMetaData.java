package com.nextlabs.rms.repository;

import java.io.File;
import java.util.Date;

public class UploadedFileMetaData {

    private String repoId;

    private String repoName;

    private String pathId;

    private String pathDisplay;

    private String fileNameWithTimeStamp;

    private File file;

    private String duid;

    private Date lastModifiedTime;

    private Date creationTime;

    private String createdBy;

    private long size;

    private boolean isFolder;

    public boolean isFolder() {
        return isFolder;
    }

    public void setFolder(boolean isFolder) {
        this.isFolder = isFolder;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getPathId() {
        return pathId;
    }

    public void setPathId(String filePathId) {
        this.pathId = filePathId;
    }

    public String getPathDisplay() {
        return pathDisplay;
    }

    public void setPathDisplay(String filePathDisplay) {
        this.pathDisplay = filePathDisplay;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public String getDuid() {
        return duid;
    }

    public void setDuid(String duid) {
        this.duid = duid;
    }

    public String getFileNameWithTimeStamp() {
        return fileNameWithTimeStamp;
    }

    public void setFileNameWithTimeStamp(String fileNameWithTimeStamp) {
        this.fileNameWithTimeStamp = fileNameWithTimeStamp;
    }

    public Date getLastModifiedTime() {
        return lastModifiedTime;
    }

    public void setLastModifiedTime(Date lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }

    public String getRepoId() {
        return repoId;
    }

    public void setRepoId(String repoId) {
        this.repoId = repoId;
    }

    public String getRepoName() {
        return repoName;
    }

    public void setRepoName(String repoName) {
        this.repoName = repoName;
    }

    public Date getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
