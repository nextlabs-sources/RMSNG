package com.nextlabs.common.shared;

public class JsonRepoFile {

    private String pathId;
    private String pathDisplay;
    private String parentFileId;
    private Long fileSize;
    private Long fileLastModified;
    private boolean fromMyVault;

    public JsonRepoFile() {

    }

    public JsonRepoFile(String fileId, String filePath) {
        this.pathId = fileId;
        this.pathDisplay = filePath;
    }

    public JsonRepoFile(String fileId, String filePath, boolean fromMyVault) {
        this.pathId = fileId;
        this.pathDisplay = filePath;
        this.fromMyVault = fromMyVault;
    }

    public String getPathId() {
        return pathId;
    }

    public void setPathId(String fileId) {
        this.pathId = fileId;
    }

    public String getPathDisplay() {
        return pathDisplay;
    }

    public void setPathDisplay(String filePath) {
        this.pathDisplay = filePath;
    }

    public String getParentFileId() {
        return parentFileId;
    }

    public void setParentFileId(String parentFileId) {
        this.parentFileId = parentFileId;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public Long getFileLastModified() {
        return fileLastModified;
    }

    public void setFileLastModified(Long fileLastModified) {
        this.fileLastModified = fileLastModified;
    }

    public boolean isFromMyVault() {
        return fromMyVault;
    }

    public void setFromMyVault(boolean fromMyVault) {
        this.fromMyVault = fromMyVault;
    }

}
