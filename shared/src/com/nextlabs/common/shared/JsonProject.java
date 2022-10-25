package com.nextlabs.common.shared;

public class JsonProject {

    private int id;
    private String parentTenantId;
    private String parentTenantName;
    private String tokenGroupName;
    private String name;
    private String description;
    private String invitationMsg;
    private String displayName;
    private long creationTime;
    private long configurationModified;
    private Long totalMembers;
    private Long totalFiles;
    private Boolean ownedByMe;
    private JsonUser owner;
    private String accountType;
    private long trialEndTime;
    private JsonProjectMemberList projectMembers;
    private String expiry;
    private String watermark;

    public String getParentTenantId() {
        return parentTenantId;
    }

    public String getParentTenantName() {
        return parentTenantName;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public String getDescription() {
        return description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Boolean getOwnedByMe() {
        return ownedByMe;
    }

    public JsonUser getOwner() {
        return owner;
    }

    public Long getTotalFiles() {
        return totalFiles;
    }

    public Long getTotalMembers() {
        return totalMembers;
    }

    public String getAccountType() {
        return accountType;
    }

    public long getTrialEndTime() {
        return trialEndTime;
    }

    public long getConfigurationModified() {
        return configurationModified;
    }

    public String getExpiry() {
        return expiry;
    }

    public String getWatermark() {
        return watermark;
    }

    public void setParentTenantId(String parentTenantId) {
        this.parentTenantId = parentTenantId;
    }

    public void setParentTenantName(String parentTenantName) {
        this.parentTenantName = parentTenantName;
    }

    public void setExpiry(String expiry) {
        this.expiry = expiry;
    }

    public void setWatermark(String watermark) {
        this.watermark = watermark;
    }

    public void setConfigurationModified(long configurationModified) {
        this.configurationModified = configurationModified;
    }

    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setOwnedByMe(Boolean ownedByMe) {
        this.ownedByMe = ownedByMe;
    }

    public void setOwner(JsonUser owner) {
        this.owner = owner;
    }

    public void setTotalFiles(Long totalFiles) {
        this.totalFiles = totalFiles;
    }

    public void setTotalMembers(Long totalMembers) {
        this.totalMembers = totalMembers;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public void setTrialEndTime(long trialEndTime) {
        this.trialEndTime = trialEndTime;
    }

    public JsonProjectMemberList getProjectMembers() {
        return projectMembers;
    }

    public void setProjectMembers(JsonProjectMemberList projectMembers) {
        this.projectMembers = projectMembers;
    }

    public String getInvitationMsg() {
        return invitationMsg;
    }

    public void setInvitationMsg(String invitationMsg) {
        this.invitationMsg = invitationMsg;
    }

    public String getTokenGroupName() {
        return tokenGroupName;
    }

    public void setTokenGroupName(String tokenGroupName) {
        this.tokenGroupName = tokenGroupName;
    }
}
