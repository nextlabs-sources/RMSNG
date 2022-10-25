package com.nextlabs.common.shared;

import com.nextlabs.common.shared.Constants.SHARESPACE;

import java.util.List;
import java.util.Map;

public class JsonSharing {

    private String duid;
    private Constants.SHARESPACE fromSpace;
    private Constants.SHARESPACE toSpace;
    private int projectId;
    private String tenantName;
    private String membershipId;
    private long permissions;
    private String watermark;
    private Map<String, String[]> tags;
    private String metadata;
    private String fileName;
    private String displayName;
    private String repositoryId;
    private String filePathId;
    private String filePath;

    private List<JsonRecipient> recipients;
    private String comment;
    private JsonExpiry expiry;
    private String efsId;
    private boolean userConfirmedFileOverwrite;

    public JsonSharing() {
    }

    public void setDuid(String duid) {
        this.duid = duid;
    }

    public String getDuid() {
        return duid;
    }

    public Constants.SHARESPACE getFromSpace() {
        return fromSpace;
    }

    public void setFromSpace(Constants.SHARESPACE fromSpace) {
        this.fromSpace = fromSpace;
    }

    public Constants.SHARESPACE getToSpace() {
        return toSpace;
    }

    public void setToSpace(Constants.SHARESPACE toSpace) {
        this.toSpace = toSpace;
    }

    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    public String getTenantName() {
        return tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public void setPermissions(int permissions) {
        this.permissions = permissions;
    }

    public int getPermissions() {
        return (int)permissions;
    }

    public Map<String, String[]> getTags() {
        return tags;
    }

    public void setTags(Map<String, String[]> tags) {
        this.tags = tags;
    }

    public void setMembershipId(String membershipId) {
        this.membershipId = membershipId;
    }

    public String getMembershipId() {
        return membershipId;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setFilePathId(String filePathId) {
        this.filePathId = filePathId;
    }

    public String getFilePathId() {
        return filePathId;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setRecipients(List<JsonRecipient> recipients) {
        this.recipients = recipients;
    }

    public List<JsonRecipient> getRecipients() {
        return recipients;
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

    public JsonExpiry getExpiry() {
        return expiry;
    }

    public void setExpiry(JsonExpiry expiry) {
        this.expiry = expiry;
    }

    public String getEfsId() {
        return efsId;
    }

    public void setEfsId(String efsId) {
        this.efsId = efsId;
    }

    public boolean isUserConfirmedFileOverwrite() {
        return userConfirmedFileOverwrite;
    }

    public void setUserConfirmedFileOverwrite(boolean userConfirmedFileOverwrite) {
        this.userConfirmedFileOverwrite = userConfirmedFileOverwrite;
    }

    public static JsonRecipient emailToJsonRecipient(String email) {
        JsonRecipient recipient = new JsonRecipient();
        recipient.setDestType(SHARESPACE.MYSPACE);
        recipient.setEmail(email);
        return recipient;
    }

    public static JsonRecipient projectToJsonRecipient(String projectId) {
        JsonRecipient recipient = new JsonRecipient();
        recipient.setDestType(SHARESPACE.PROJECTSPACE);
        recipient.setProjectId(Integer.parseInt(projectId));
        return recipient;
    }

    public static final class JsonRecipient {

        private Constants.SHARESPACE destType;
        private String email;
        private int projectId;
        private String tenantName;

        public JsonRecipient() {
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getEmail() {
            return email;
        }

        public Constants.SHARESPACE getDestType() {
            return destType;
        }

        public void setDestType(Constants.SHARESPACE destType) {
            this.destType = destType;
        }

        public int getProjectId() {
            return projectId;
        }

        public void setProjectId(int projectId) {
            this.projectId = projectId;
        }

        public String getTenantName() {
            return tenantName;
        }

        public void setTenantName(String tenantName) {
            this.tenantName = tenantName;
        }

    }
}
