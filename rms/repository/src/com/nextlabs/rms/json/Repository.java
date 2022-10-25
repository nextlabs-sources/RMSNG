package com.nextlabs.rms.json;

import java.io.Serializable;

public class Repository implements Serializable {

    private static final long serialVersionUID = -8680897642067024996L;

    private String repoId;

    private String repoName;

    private String sid;

    private String repoType;

    private String accountName;

    private String repoTypeDisplayName;

    private boolean isShared;

    private Long quota;

    private Long usage;

    private Long myVaultUsage;

    private Long vaultQuota;

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getRepoId() {
        return repoId;
    }

    public void setRepoId(String repoId) {
        this.repoId = repoId;
    }

    public String getRepoName() {
        return repoName;
    }

    public void setRepoName(String repoName) {
        this.repoName = repoName;
    }

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public String getRepoType() {
        return repoType;
    }

    public void setRepoType(String repoType) {
        this.repoType = repoType;
    }

    public String getRepoTypeDisplayName() {
        return repoTypeDisplayName;
    }

    public void setRepoTypeDisplayName(String repoTypeDisplayName) {
        this.repoTypeDisplayName = repoTypeDisplayName;
    }

    public boolean isShared() {
        return isShared;
    }

    public void setShared(boolean isShared) {
        this.isShared = isShared;
    }

    public Long getUsage() {
        return usage;
    }

    public void setUsage(Long usage) {
        this.usage = usage;
    }

    public Long getQuota() {
        return quota;
    }

    public void setQuota(Long quota) {
        this.quota = quota;
    }

    public Long getMyVaultUsage() {
        return myVaultUsage;
    }

    public void setMyVaultUsage(Long myVaultUsage) {
        this.myVaultUsage = myVaultUsage;
    }

    public Long getVaultQuota() {
        return vaultQuota;
    }

    public void setVaultQuota(Long myVaultQuota) {
        this.vaultQuota = myVaultQuota;
    }

}
