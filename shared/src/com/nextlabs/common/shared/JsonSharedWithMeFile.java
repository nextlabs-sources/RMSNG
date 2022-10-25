package com.nextlabs.common.shared;

import java.util.Map;

public class JsonSharedWithMeFile {

    private String duid;
    private String name;
    private Long size;
    private String fileType;
    private Long sharedDate;
    private String sharedBy;
    private String transactionId;
    private String transactionCode;
    private String sharedLink;
    private String[] rights;
    private Map<String, String[]> tags;
    private String comment;
    private boolean isOwner;
    private JsonExpiry validity;
    private int protectionType = -1;
    //    Should be used later
    //    private JsonSharedProject sharedByProject; 
    private String sharedByProject;
    private String sharedByProjectName;

    public String getDuid() {
        return duid;
    }

    public void setDuid(String duid) {
        this.duid = duid;
    }

    public String getFileName() {
        return name;
    }

    public void setFileName(String name) {
        this.name = name;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public Long getSharedDate() {
        return sharedDate;
    }

    public void setSharedDate(Long sharedDate) {
        this.sharedDate = sharedDate;
    }

    public String getSharedBy() {
        return sharedBy;
    }

    public void setSharedBy(String sharedBy) {
        this.sharedBy = sharedBy;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getTransactionCode() {
        return transactionCode;
    }

    public void setTransactionCode(String transactionCode) {
        this.transactionCode = transactionCode;
    }

    public String getSharedLink() {
        return sharedLink;
    }

    public void setSharedLink(String sharedLink) {
        this.sharedLink = sharedLink;
    }

    public String[] getRights() {
        return rights;
    }

    public void setRights(String[] rights) {
        this.rights = rights;
    }

    public Map<String, String[]> getTags() {
        return tags;
    }

    public void setTags(Map<String, String[]> tags) {
        this.tags = tags;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public boolean isOwner() {
        return isOwner;
    }

    public void setOwner(boolean owner) {
        isOwner = owner;
    }

    public JsonExpiry getValidity() {
        return validity;
    }

    public void setValidity(JsonExpiry validity) {
        this.validity = validity;
    }

    public int getProtectionType() {
        return protectionType;
    }

    public void setProtectionType(int protectionType) {
        this.protectionType = protectionType;
    }

    public String getSharedByProject() {
        return sharedByProject;
    }

    public void setSharedByProject(String sharedByProject) {
        this.sharedByProject = sharedByProject;
    }

    public String getSharedByProjectName() {
        return sharedByProjectName;
    }

    public void setSharedByProjectName(String sharedByProjectName) {
        this.sharedByProjectName = sharedByProjectName;
    }

}
