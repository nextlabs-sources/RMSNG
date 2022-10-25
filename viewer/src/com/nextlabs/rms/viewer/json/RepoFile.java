package com.nextlabs.rms.viewer.json;

import java.io.Serializable;

public class RepoFile implements Serializable {

    private static final long serialVersionUID = 1L;
    private String repoId;
    private String repoType;
    private String repoName;
    private String fileId;
    private String filePathDisplay;
    private boolean fromMyDrive;

    public RepoFile(String repoId, String repoType, String repoName, String fileId, String filePathDisplay) {
        this.repoId = repoId;
        this.repoType = repoType;
        this.repoName = repoName;
        this.fileId = fileId;
        this.filePathDisplay = filePathDisplay;
    }

    public String getRepoId() {
        return repoId;
    }

    public void setRepoId(String repoId) {
        this.repoId = repoId;
    }

    public String getRepoType() {
        return repoType;
    }

    public void setRepoType(String repoType) {
        this.repoType = repoType;
    }

    public String getRepoName() {
        return repoName;
    }

    public void setRepoName(String repoName) {
        this.repoName = repoName;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getFilePathDisplay() {
        return filePathDisplay;
    }

    public void setFilePathDisplay(String filePathDisplay) {
        this.filePathDisplay = filePathDisplay;
    }

    public boolean isFromMyDrive() {
        return fromMyDrive;
    }

    public void setFromMyDrive(boolean fromMyDrive) {
        this.fromMyDrive = fromMyDrive;
    }
}
