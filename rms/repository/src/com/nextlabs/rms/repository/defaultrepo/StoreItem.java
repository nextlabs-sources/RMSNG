package com.nextlabs.rms.repository.defaultrepo;

import com.nextlabs.rms.hibernate.model.AllNxl;
import com.nextlabs.rms.hibernate.model.ProjectSpaceItem.Status;

import java.io.Serializable;
import java.util.Date;

public class StoreItem implements Serializable {

    private static final long serialVersionUID = -5746446500215065681L;
    private Long id;
    private String repoId;
    private Date lastModified;
    private String filePath;
    private String filePathSearchSpace;
    private String filePathDisplay;
    private boolean isDirectory;
    private long size;
    private String customUserMetatdata;
    private boolean deleted;
    private AllNxl nxl;
    private Date creationTime;
    private String duid;
    private int projectId;
    private String tenantId;
    private int permissions;
    private Date expiration;
    private int userId;
    private String fileParentPath;
    private int lastModifiedUserId;
    private String projectSpaceItemId;
    private String enterpriseSpaceItemId;
    private Status status = Status.ACTIVE;

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRepoId() {
        return repoId;
    }

    public void setRepoId(String repoId) {
        this.repoId = repoId;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFilePathSearchSpace() {
        return filePathSearchSpace;
    }

    public void setFilePathSearchSpace(String filePathSearchSpace) {
        this.filePathSearchSpace = filePathSearchSpace;
    }

    public String getFilePathDisplay() {
        return filePathDisplay;
    }

    public void setFilePathDisplay(String filePathDisplay) {
        this.filePathDisplay = filePathDisplay;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public void setDirectory(boolean isDirectory) {
        this.isDirectory = isDirectory;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getCustomUserMetatdata() {
        return customUserMetatdata;
    }

    public void setCustomUserMetatdata(String customUserMetatdata) {
        this.customUserMetatdata = customUserMetatdata;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public AllNxl getNxl() {
        return nxl;
    }

    public void setNxl(AllNxl nxl) {
        this.nxl = nxl;
    }

    public Date getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    public String getDuid() {
        return duid;
    }

    public void setDuid(String duid) {
        this.duid = duid;
    }

    public int getPermissions() {
        return permissions;
    }

    public void setPermissions(int permissions) {
        this.permissions = permissions;
    }

    public Date getExpiration() {
        return expiration;
    }

    public void setExpiration(Date expiration) {
        this.expiration = expiration;
    }

    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getFileParentPath() {
        return fileParentPath;
    }

    public void setFileParentPath(String fileParentPath) {
        this.fileParentPath = fileParentPath;
    }

    public int getLastModifiedUserId() {
        return lastModifiedUserId;
    }

    public void setLastModifiedUserId(int lastModifiedUserId) {
        this.lastModifiedUserId = lastModifiedUserId;
    }

    public String getProjectSpaceItemId() {
        return projectSpaceItemId;
    }

    public void setProjectSpaceItemId(String projectSpaceItemId) {
        this.projectSpaceItemId = projectSpaceItemId;
    }

    public String getEnterpriseSpaceItemId() {
        return enterpriseSpaceItemId;
    }

    public void setEnterpriseSpaceItemId(String enterpriseSpaceItemId) {
        this.enterpriseSpaceItemId = enterpriseSpaceItemId;
    }

}
