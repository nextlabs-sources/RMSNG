package com.nextlabs.rms.json;

import com.nextlabs.rms.entity.setting.ServiceProviderType;

import java.util.List;

public class FileListResult {

    private boolean result;
    private List<String> messages;
    private Object content;
    private String redirectUrl;
    private Object invalidTokenRepos;
    private String repoId;
    private ServiceProviderType repoType;
    private String repoName;

    public boolean isResult() {
        return result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    public List<String> getMessages() {
        return messages;
    }

    public void setMessages(List<String> messages) {
        this.messages = messages;
    }

    public Object getContent() {
        return content;
    }

    public void setContent(Object content) {
        this.content = content;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    public Object getInvalidTokenRepos() {
        return invalidTokenRepos;
    }

    public void setInvalidTokenRepos(Object invalidTokenRepos) {
        this.invalidTokenRepos = invalidTokenRepos;
    }

    public ServiceProviderType getRepoType() {
        return repoType;
    }

    public void setRepoType(ServiceProviderType repoType) {
        this.repoType = repoType;
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
}
