package com.nextlabs.rms.dto.repository;

import com.nextlabs.nxl.Rights;
import com.nextlabs.rms.hibernate.model.AllNxl.Status;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;

public class SharedNxlFile implements Serializable {

    private static final long serialVersionUID = -4038125600569155662L;
    private String tenantId;
    private String fileName;
    private Set<String> shareWith;
    private Rights[] rights;
    private String duid;
    private String policy;
    private Date creationTime;
    private Date lastModified;
    private int userId;
    private Status status;
    private long size;
    private String repoId;
    private String filePath;
    private String filePathDisplay;
    private boolean deleted;
    private boolean shared;
    private String customMetadata;

    public Date getCreationTime() {
        return creationTime;
    }

    public String getDuid() {
        return duid;
    }

    public String getFileName() {
        return fileName;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public Rights[] getRights() {
        return rights;
    }

    public Set<String> getShareWith() {
        return shareWith;
    }

    public String getTenantId() {
        return tenantId;
    }

    public int getUserId() {
        return userId;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    public void setDuid(String duid) {
        this.duid = duid;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public void setRights(Rights[] rightsGranted) {
        this.rights = rightsGranted;
    }

    public void setShareWith(Set<String> shareWith) {
        this.shareWith = shareWith;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getRepoId() {
        return repoId;
    }

    public void setRepoId(String repoId) {
        this.repoId = repoId;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFilePathDisplay() {
        return filePathDisplay;
    }

    public void setFilePathDisplay(String filePathDisplay) {
        this.filePathDisplay = filePathDisplay;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public boolean isShared() {
        return shared;
    }

    public void setShared(boolean shared) {
        this.shared = shared;
    }

    public String getCustomMetadata() {
        return customMetadata;
    }

    public void setCustomMetadata(String customMetadata) {
        this.customMetadata = customMetadata;
    }

    public String getPolicy() {
        return policy;
    }

    public void setPolicy(String policy) {
        this.policy = policy;
    }
}
