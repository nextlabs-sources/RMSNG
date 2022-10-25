package com.nextlabs.rms.viewer.conversion;

import com.nextlabs.common.shared.JsonExpiry;

import java.util.Map;

public class DocumentMetaData {

    private String displayName;

    private String originalFileName;

    private int numPages;

    private String[] rights;

    private boolean owner;

    private boolean isNXL;

    private String errMsg;

    private WaterMark watermark;

    private String efsId;

    private String repoId;

    private String repoType;

    private String repoName;

    private boolean isRepoReadOnly;

    private String filePath;

    private String filePathDisplay;

    private long lastModifiedDate;

    private long fileSize;

    private Map<String, String[]> tagsMap;

    private String rmsURL;

    private String duid;

    private String ownerId;

    private boolean showPathInfo;

    private boolean projectFile;

    private boolean workspaceFile;

    private String projectId;

    private String transactionId;

    private String transactionCode;

    private boolean isSingleImageFile;

    private JsonExpiry validity;

    private int protectionType = -1;

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getErrMsg() {
        return errMsg;
    }

    public void setErrMsg(String errMsg) {
        this.errMsg = errMsg;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public int getNumPages() {
        return numPages;
    }

    public void setNumPages(int numPages) {
        this.numPages = numPages;
    }

    public WaterMark getWatermark() {
        return watermark;
    }

    public void setWatermark(WaterMark watermark) {
        this.watermark = watermark;
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

    public Map<String, String[]> getTagsMap() {
        return tagsMap;
    }

    public void setTagsMap(Map<String, String[]> tagsMap) {
        this.tagsMap = tagsMap;
    }

    public long getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(long lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getRepoName() {
        return repoName;
    }

    public void setRepoName(String repoName) {
        this.repoName = repoName;
    }

    public boolean isRepoReadOnly() {
        return isRepoReadOnly;
    }

    public void setRepoReadOnly(boolean isRepoReadOnly) {
        this.isRepoReadOnly = isRepoReadOnly;
    }

    public boolean isNXL() {
        return isNXL;
    }

    public void setNXL(boolean isNXL) {
        this.isNXL = isNXL;
    }

    public String getRmsURL() {
        return rmsURL;
    }

    public void setRmsURL(String rmsURL) {
        this.rmsURL = rmsURL;
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

    public String getDuid() {
        return duid;
    }

    public void setDuid(String duid) {
        this.duid = duid;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getEfsId() {
        return efsId;
    }

    public void setEfsId(String efsId) {
        this.efsId = efsId;
    }

    public boolean isShowPathInfo() {
        return showPathInfo;
    }

    public void setShowPathInfo(boolean showPathInfo) {
        this.showPathInfo = showPathInfo;
    }

    public boolean isProjectFile() {
        return projectFile;
    }

    public void setProjectFile(boolean projectFile) {
        this.projectFile = projectFile;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
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

    public boolean isSingleImageFile() {
        return isSingleImageFile;
    }

    public void setSingleImageFile(boolean isSingleImageFile) {
        this.isSingleImageFile = isSingleImageFile;
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

    public boolean isWorkspaceFile() {
        return workspaceFile;
    }

    public void setWorkspaceFile(boolean workspaceFile) {
        this.workspaceFile = workspaceFile;
    }

}
