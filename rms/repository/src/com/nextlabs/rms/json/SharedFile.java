package com.nextlabs.rms.json;

import java.util.Map;
import java.util.Set;

public class SharedFile {

    private String pathId;
    private String pathDisplay;
    private String repoId;
    private long sharedOn;
    private Set<String> sharedWith;
    private String repoName;
    private String[] rights;
    private String name;
    private String fileType;
    private String duid;
    private boolean revoked;
    private boolean deleted;
    private boolean shared;
    private boolean favorited;
    private long size;
    private Map<String, String> customMetadata;

    public String getName() {
        return name;
    }

    public String getPathId() {
        return pathId;
    }

    public String getFileType() {
        return fileType;
    }

    public String getRepoName() {
        return repoName;
    }

    public String[] getRights() {
        return rights;
    }

    public long getSharedOn() {
        return sharedOn;
    }

    public Set<String> getSharedWith() {
        return sharedWith;
    }

    public void setName(String fileName) {
        this.name = fileName;
    }

    public void setPathId(String filePath) {
        this.pathId = filePath;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public void setRepoName(String repoName) {
        this.repoName = repoName;
    }

    public void setRights(String[] rights) {
        this.rights = rights;
    }

    public void setSharedOn(long sharedOn) {
        this.sharedOn = sharedOn;
    }

    public void setSharedWith(Set<String> sharedWith) {
        this.sharedWith = sharedWith;
    }

    public String getDuid() {
        return duid;
    }

    public void setDuid(String duid) {
        this.duid = duid;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

    public boolean isShared() {
        return shared;
    }

    public void setShared(boolean shared) {
        this.shared = shared;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public Map<String, String> getCustomMetadata() {
        return customMetadata;
    }

    public void setCustomMetadata(Map<String, String> customMetadata) {
        this.customMetadata = customMetadata;
    }

    public String getPathDisplay() {
        return pathDisplay;
    }

    public void setPathDisplay(String pathDisplay) {
        this.pathDisplay = pathDisplay;
    }

    public String getRepoId() {
        return repoId;
    }

    public void setRepoId(String repoId) {
        this.repoId = repoId;
    }

    public boolean isFavorited() {
        return favorited;
    }

    public void setFavorited(boolean favorited) {
        this.favorited = favorited;
    }

}
