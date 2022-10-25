package com.nextlabs.rms.application;

public class ApplicationRepositoryContent {

    private String fileId;

    private String name;

    private String path;

    private String pathId;

    private boolean isFolder;

    private Long createdTime;

    private Long lastModifiedTime;

    private Long fileSize;

    private String createdByUser;

    private String lastModifiedByUser;

    private boolean protectedFile;

    private String fileType;

    private boolean encryptable;

    public ApplicationRepositoryContent() {
        encryptable = true;
    }

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

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPathId() {
        return pathId;
    }

    public void setPathId(String pathId) {
        this.pathId = pathId;
    }

    public boolean isFolder() {
        return isFolder;
    }

    public void setFolder(boolean folder) {
        isFolder = folder;
    }

    public Long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Long createdTime) {
        this.createdTime = createdTime;
    }

    public Long getLastModifiedTime() {
        return lastModifiedTime;
    }

    public void setLastModifiedTime(Long lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getCreatedByUser() {
        return createdByUser;
    }

    public void setCreatedByUser(String createdByUser) {
        this.createdByUser = createdByUser;
    }

    public String getLastModifiedByUser() {
        return lastModifiedByUser;
    }

    public void setLastModifiedByUser(String lastModifiedByUser) {
        this.lastModifiedByUser = lastModifiedByUser;
    }

    public boolean isProtectedFile() {
        return protectedFile;
    }

    public void setProtectedFile(boolean protectedFile) {
        this.protectedFile = protectedFile;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public boolean isEncryptable() {
        return encryptable;
    }

    public void setEncryptable(boolean encryptable) {
        this.encryptable = encryptable;
    }

    @Override
    public String toString() {
        return "ApplicationRepositoryContent{" + "fileId='" + fileId + '\'' + ", name='" + name + '\'' + ", path='" + path + '\'' + ", pathId='" + pathId + '\'' + ", isFolder=" + isFolder + ", createdTime=" + createdTime + ", lastModifiedTime=" + lastModifiedTime + ", createdByUser='" + createdByUser + '\'' + ", lastModifiedByUser='" + lastModifiedByUser + '\'' + ", protectedFile=" + protectedFile + '}';
    }
}
