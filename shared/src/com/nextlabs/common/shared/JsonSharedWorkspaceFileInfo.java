package com.nextlabs.common.shared;

import java.util.Map;

public class JsonSharedWorkspaceFileInfo {

    private String path;
    private String pathId;
    private String name;
    private String fileType;
    private Long lastModified;
    private Long size;
    private String[] rights;
    private boolean uploader;
    private boolean protectedFile;
    private Map<String, String[]> tags;
    private int protectionType = -1;
    private JsonExpiry expiry;
    private Long creationTime;
    private JsonEnterpriseSpaceMember uploadedBy;
    private JsonEnterpriseSpaceMember lastModifiedUser;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public Long getLastModified() {
        return lastModified;
    }

    public void setLastModified(Long lastModified) {
        this.lastModified = lastModified;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String[] getRights() {
        return rights;
    }

    public void setRights(String[] rights) {
        this.rights = rights;
    }

    public boolean isUploader() {
        return uploader;
    }

    public void setUploader(boolean uploader) {
        this.uploader = uploader;
    }

    public boolean isProtectedFile() {
        return protectedFile;
    }

    public void setProtectedFile(boolean protectedFile) {
        this.protectedFile = protectedFile;
    }

    public Map<String, String[]> getTags() {
        return tags;
    }

    public void setTags(Map<String, String[]> tags) {
        this.tags = tags;
    }

    public int getProtectionType() {
        return protectionType;
    }

    public void setProtectionType(int protectionType) {
        this.protectionType = protectionType;
    }

    public JsonExpiry getExpiry() {
        return expiry;
    }

    public void setExpiry(JsonExpiry expiry) {
        this.expiry = expiry;
    }

    public Long getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Long creationTime) {
        this.creationTime = creationTime;
    }

    public JsonEnterpriseSpaceMember getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(JsonEnterpriseSpaceMember uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public JsonEnterpriseSpaceMember getLastModifiedUser() {
        return lastModifiedUser;
    }

    public void setLastModifiedUser(JsonEnterpriseSpaceMember lastModifiedUser) {
        this.lastModifiedUser = lastModifiedUser;
    }

}
