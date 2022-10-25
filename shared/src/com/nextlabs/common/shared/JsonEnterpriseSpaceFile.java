package com.nextlabs.common.shared;

public class JsonEnterpriseSpaceFile {

    private String id;
    private String duid;
    private String pathDisplay;
    private String pathId;
    private String name;
    private String fileType;
    private Long lastModified;
    private Long creationTime;
    private Long size;
    private boolean folder;
    private JsonEnterpriseSpaceMember uploader;
    private JsonEnterpriseSpaceMember lastModifiedUser;

    public Long getCreationTime() {
        return creationTime;
    }

    public Long getSize() {
        return size;
    }

    public String getId() {
        return id;
    }

    public Long getLastModified() {
        return lastModified;
    }

    public JsonEnterpriseSpaceMember getUploader() {
        return uploader;
    }

    public String getPathDisplay() {
        return pathDisplay;
    }

    public String getPathId() {
        return pathId;
    }

    public boolean isFolder() {
        return folder;
    }

    public void setCreationTime(Long creationTime) {
        this.creationTime = creationTime;
    }

    public void setSize(Long fileSize) {
        this.size = fileSize;
    }

    public void setFolder(boolean isFolder) {
        this.folder = isFolder;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setLastModified(Long lastModifiedTime) {
        this.lastModified = lastModifiedTime;
    }

    public void setUploader(JsonEnterpriseSpaceMember uploader) {
        this.uploader = uploader;
    }

    public void setPathDisplay(String pathDisplay) {
        this.pathDisplay = pathDisplay;
    }

    public void setPathId(String pathId) {
        this.pathId = pathId;
    }

    public String getName() {
        return name;
    }

    public void setName(String fileName) {
        this.name = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getDuid() {
        return duid;
    }

    public void setDuid(String duid) {
        this.duid = duid;
    }

    public JsonEnterpriseSpaceMember getLastModifiedUser() {
        return lastModifiedUser;
    }

    public void setLastModifiedUser(JsonEnterpriseSpaceMember lastModifiedUser) {
        this.lastModifiedUser = lastModifiedUser;
    }
}
