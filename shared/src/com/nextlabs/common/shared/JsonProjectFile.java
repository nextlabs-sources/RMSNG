package com.nextlabs.common.shared;

import java.util.Set;

public class JsonProjectFile {

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
    private JsonProjectMember owner;
    private JsonProjectMember lastModifiedUser;
    private Set<String> shareWithPersonal;
    private Set<String> shareWithEnterprise;
    //    Should be used later
    //    private Set<JsonSharedProject> shareWithProject;
    private Set<Integer> shareWithProject;
    private Set<String> shareWithProjectName;
    private boolean isShared;
    private boolean revoked;

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

    public Set<String> getShareWithPersonal() {
        return shareWithPersonal;
    }

    public void setShareWithPersonal(Set<String> shareWithPersonal) {
        this.shareWithPersonal = shareWithPersonal;
    }

    public Set<String> getShareWithEnterprise() {
        return shareWithEnterprise;
    }

    public void setShareWithEnterprise(Set<String> shareWithEnterprise) {
        this.shareWithEnterprise = shareWithEnterprise;
    }

    public boolean isShared() {
        return isShared;
    }

    public void setShared(boolean isShared) {
        this.isShared = isShared;
    }

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

    public JsonProjectMember getOwner() {
        return owner;
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

    public void setOwner(JsonProjectMember owner) {
        this.owner = owner;
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

    public JsonProjectMember getLastModifiedUser() {
        return lastModifiedUser;
    }

    public void setLastModifiedUser(JsonProjectMember lastModifiedUser) {
        this.lastModifiedUser = lastModifiedUser;
    }

    public Set<Integer> getShareWithProject() {
        return shareWithProject;
    }

    public void setShareWithProject(Set<Integer> shareWithProject) {
        this.shareWithProject = shareWithProject;
    }

    public Set<String> getShareWithProjectName() {
        return shareWithProjectName;
    }

    public void setShareWithProjectName(Set<String> shareWithProjectName) {
        this.shareWithProjectName = shareWithProjectName;
    }

}
