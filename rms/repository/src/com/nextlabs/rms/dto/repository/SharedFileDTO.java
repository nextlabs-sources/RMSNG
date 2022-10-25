package com.nextlabs.rms.dto.repository;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * @author nnallagatla
 *
 */
public class SharedFileDTO implements Serializable {

    private static final long serialVersionUID = 1132010995434244172L;
    private long id;
    private String transactionID;
    private int userId;
    private String ticket;
    private String sharingUserEmail;
    private Integer projectId;
    private String tenantName;
    private String tenantId;
    private String repositoryId;
    private String repositoryName;
    private String repositoryType;
    private String documentUID;
    private String filePathId;
    private String filePath;
    private String fileName;
    private Date createdDate;
    private Date updatedDate;
    private String deviceType;
    private String deviceId;
    private Set<?> shareWith;
    private int grantedRights;
    private boolean revoked;
    private String owner;
    private String clientId;
    private Integer platformId;
    private String comment;
    private String policy;
    private String expiryStr;
    private Integer sourceProjectId;
    private boolean userConfirmedFileOverWrite;

    List<String> sharedWithProject;

    public List<String> getSharedWithProject() {
        return sharedWithProject;
    }

    public void setSharedWithProject(List<String> sharedWithProject) {
        this.sharedWithProject = sharedWithProject;
    }

    public Integer getSourceProjectId() {
        return sourceProjectId;
    }

    public void setSourceProjectId(Integer sourceProjectId) {
        this.sourceProjectId = sourceProjectId;
    }

    public String getTicket() {
        return ticket;
    }

    public void setTicket(String ticket) {
        this.ticket = ticket;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public String getDocumentUID() {
        return documentUID;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getFilePathId() {
        return filePathId;
    }

    public long getId() {
        return id;
    }

    public String getOwner() {
        return owner;
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public int getGrantedRights() {
        return grantedRights;
    }

    public Set<?> getShareWith() {
        return shareWith;
    }

    public String getSharingUserEmail() {
        return sharingUserEmail;
    }

    public Integer getProjectId() {
        return projectId;
    }

    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }

    public String getTenantName() {
        return tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getTransactionID() {
        return transactionID;
    }

    public Date getUpdatedDate() {
        return updatedDate;
    }

    public int getUserId() {
        return userId;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public void setDocumentUID(String documentUID) {
        this.documentUID = documentUID;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void setFilePathId(String filePathId) {
        this.filePathId = filePathId;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    public void setRevoked(boolean isRevoked) {
        this.revoked = isRevoked;
    }

    public void setGrantedRights(int grantedRights) {
        this.grantedRights = grantedRights;
    }

    public void setShareWith(Set<?> shareWith) {
        this.shareWith = shareWith;
    }

    public void setSharingUserEmail(String sharingUserEmail) {
        this.sharingUserEmail = sharingUserEmail;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public void setTransactionID(String transactionID) {
        this.transactionID = transactionID;
    }

    public void setUpdatedDate(Date updatedDate) {
        this.updatedDate = updatedDate;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getRepositoryType() {
        return repositoryType;
    }

    public void setRepositoryType(String repositoryType) {
        this.repositoryType = repositoryType;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public Integer getPlatformId() {
        return platformId;
    }

    public void setPlatformId(Integer platformId) {
        this.platformId = platformId;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getPolicy() {
        return policy;
    }

    public void setPolicy(String policy) {
        this.policy = policy;
    }

    public String getExpiryStr() {
        return expiryStr;
    }

    public void setExpiryStr(String expiryStr) {
        this.expiryStr = expiryStr;
    }

    public boolean isUserConfirmedFileOverWrite() {
        return userConfirmedFileOverWrite;
    }

    public void setUserConfirmedFileOverWrite(boolean userConfirmedFileOverWrite) {
        this.userConfirmedFileOverWrite = userConfirmedFileOverWrite;
    }

}
