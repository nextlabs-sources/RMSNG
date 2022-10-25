package com.nextlabs.common.shared;

import java.util.Map;
import java.util.Set;

public class JsonProjectFileInfo {

    private String pathDisplay;
    private String pathId;
    private String name;
    private String fileType;
    private Long lastModified;
    private Long size;
    private String[] rights;
    private boolean owner;
    private boolean nxl;
    private Map<String, String[]> tags;
    private int protectionType = -1;
    private JsonExpiry expiry;
    private Long creationTime;
    private JsonProjectMember createdBy;
    private JsonProjectMember lastModifiedUser;
    private Set<JsonSharedProject> shareWithProjects;
    private boolean isShared;
    private boolean revoked;

    public String getName() {
        return name;
    }

    public void setName(String fileName) {
        this.name = fileName;
    }

    public Map<String, String[]> getClassification() {
        return tags;
    }

    public void setClassification(Map<String, String[]> tags) {
        this.tags = tags;
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

    public void setLastModified(Long lastModifiedTime) {
        this.lastModified = lastModifiedTime;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long fileSize) {
        this.size = fileSize;
    }

    public String[] getRights() {
        return rights;
    }

    public void setRights(String[] rights) {
        this.rights = rights;
    }

    public boolean isOwner() {
        return owner;
    }

    public void setOwner(boolean owner) {
        this.owner = owner;
    }

    public boolean isNxl() {
        return nxl;
    }

    public void setNxl(boolean nxl) {
        this.nxl = nxl;
    }

    public String getPathDisplay() {
        return pathDisplay;
    }

    public void setPathDisplay(String pathDisplay) {
        this.pathDisplay = pathDisplay;
    }

    public String getPathId() {
        return pathId;
    }

    public void setPathId(String pathId) {
        this.pathId = pathId;
    }

    public JsonExpiry getExpiry() {
        return expiry;
    }

    public void setExpiry(JsonExpiry expiry) {
        this.expiry = expiry;
    }

    public int getProtectionType() {
        return protectionType;
    }

    public void setProtectionType(int protectionType) {
        this.protectionType = protectionType;
    }

    public Long getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Long creationTime) {
        this.creationTime = creationTime;
    }

    public JsonProjectMember getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(JsonProjectMember createdBy) {
        this.createdBy = createdBy;
    }

    public JsonProjectMember getLastModifiedUser() {
        return lastModifiedUser;
    }

    public void setLastModifiedUser(JsonProjectMember lastModifiedUser) {
        this.lastModifiedUser = lastModifiedUser;
    }

    public Set<JsonSharedProject> getShareWithProjects() {
        return shareWithProjects;
    }

    public void setShareWithProjects(Set<JsonSharedProject> shareWithProjects) {
        this.shareWithProjects = shareWithProjects;
    }

    public boolean isShared() {
        return isShared;
    }

    public void setShared(boolean isShared) {
        this.isShared = isShared;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

}
