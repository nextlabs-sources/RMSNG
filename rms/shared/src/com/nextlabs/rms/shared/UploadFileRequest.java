package com.nextlabs.rms.shared;

public class UploadFileRequest {

    private String filename;
    private String conflictFileName;
    private byte[] bytes;
    private String userName;
    private String offset;
    private String tenantId;
    private String ticket;
    private String uid;
    private String tenantName;
    private String rightsJSON;
    private String watermark;
    private String shareWithJSON;
    private String repoId;
    private String filePathId;
    private String filePathDisplay;
    private boolean isFileAttached;
    private String clientId;
    private String comment;
    private String expiry;
    private boolean userConfirmedFileOverwrite;

    public UploadFileRequest(String filename, byte[] bytes, String userName, String offset, String tenantId) {
        this.filename = filename;
        this.bytes = bytes;
        this.userName = userName;
        this.offset = offset;
        this.tenantId = tenantId;
    }

    public String getFileName() {
        return filename;
    }

    public void setFileName(String filename) {
        this.filename = filename;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getOffset() {
        return offset;
    }

    public void setOffset(String offset) {
        this.offset = offset;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getTicket() {
        return ticket;
    }

    public void setTicket(String ticket) {
        this.ticket = ticket;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getTenantName() {
        return tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public String getRightsJSON() {
        return rightsJSON;
    }

    public void setRightsJSON(String rightsJSON) {
        this.rightsJSON = rightsJSON;
    }

    public String getShareWithJSON() {
        return shareWithJSON;
    }

    public void setShareWithJSON(String shareWithJSON) {
        this.shareWithJSON = shareWithJSON;
    }

    public String getRepoId() {
        return repoId;
    }

    public void setRepoId(String repoId) {
        this.repoId = repoId;
    }

    public String getFilePathId() {
        return filePathId;
    }

    public void setFilePathId(String filePathId) {
        this.filePathId = filePathId;
    }

    public String getFilePathDisplay() {
        return filePathDisplay;
    }

    public void setFilePathDisplay(String filePathDisplay) {
        this.filePathDisplay = filePathDisplay;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getConflictFileName() {
        return conflictFileName;
    }

    public void setConflictFileName(String conflictFileName) {
        this.conflictFileName = conflictFileName;
    }

    public boolean isFileAttached() {
        return isFileAttached;
    }

    public void setFileAttached(boolean isFileAttached) {
        this.isFileAttached = isFileAttached;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getWatermark() {
        return watermark;
    }

    public void setWatermark(String watermark) {
        this.watermark = watermark;
    }

    public String getExpiry() {
        return expiry;
    }

    public void setExpiry(String expiry) {
        this.expiry = expiry;
    }

    public boolean isUserConfirmedFileOverwrite() {
        return userConfirmedFileOverwrite;
    }

    public void setUserConfirmedFileOverwrite(boolean userConfirmedFileOverwrite) {
        this.userConfirmedFileOverwrite = userConfirmedFileOverwrite;
    }

}
