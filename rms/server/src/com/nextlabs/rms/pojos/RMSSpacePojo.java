package com.nextlabs.rms.pojos;

public class RMSSpacePojo {

    String fileName;
    String spaceType;
    String spaceId;
    String filePathId;
    String transactionId;
    String parentPathId;
    String spaceRepoName;

    public String getSpaceRepoName() {
        return spaceRepoName;
    }

    public void setSpaceRepoName(String spaceRepoName) {
        this.spaceRepoName = spaceRepoName;
    }

    public String getFilePathId() {
        return filePathId;
    }

    public void setFilePathId(String filePathId) {
        this.filePathId = filePathId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getSpaceType() {
        return spaceType;
    }

    public void setSpaceType(String spaceType) {
        this.spaceType = spaceType;
    }

    public String getSpaceId() {
        return spaceId;
    }

    public void setSpaceId(String spaceID) {
        this.spaceId = spaceID;
    }

    public String getParentPathId() {
        return parentPathId;
    }

    public void setParentPathId(String parentPathId) {
        this.parentPathId = parentPathId;
    }
}
