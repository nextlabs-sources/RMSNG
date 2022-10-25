package com.nextlabs.rms.json;

import com.nextlabs.common.shared.JsonExpiry;

import java.util.ArrayList;
import java.util.List;

public class SharedFileResponse {

    private boolean result;
    private int statusCode;
    private List<String> messages;
    private String sharedLink;
    private String duid;
    private String filePathId;
    private String fileName;
    private String newSharedEmailsStr;
    private String alreadySharedEmailStr;
    private JsonExpiry validity;
    private int protectionType;

    private List<String> newSharedProjects;
    private List<String> alreadySharedProjects;

    public List<String> getNewSharedProjects() {
        return newSharedProjects;
    }

    public void setNewSharedProjects(List<String> newSharedProjects) {
        this.newSharedProjects = newSharedProjects;
    }

    public List<String> getAlreadySharedProjects() {
        return alreadySharedProjects;
    }

    public void setAlreadySharedProjects(List<String> alreadySharedProjects) {
        this.alreadySharedProjects = alreadySharedProjects;
    }

    public SharedFileResponse() {
        messages = new ArrayList<>();
        protectionType = -1;
    }

    public List<String> getMessages() {
        return messages;
    }

    public String getSharedLink() {
        return sharedLink;
    }

    public boolean isResult() {
        return result;
    }

    public void addMessage(String message) {
        messages.add(message);
    }

    public void setMessages(List<String> messages) {
        this.messages = messages;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public void setSharedLink(String sharedLink) {
        this.sharedLink = sharedLink;
    }

    public String getDuid() {
        return duid;
    }

    public void setDuid(String duid) {
        this.duid = duid;
    }

    public String getFilePathId() {
        return filePathId;
    }

    public void setFilePathId(String filePathId) {
        this.filePathId = filePathId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getNewSharedEmailsStr() {
        return newSharedEmailsStr;
    }

    public void setNewSharedEmailsStr(String newSharedEmailsStr) {
        this.newSharedEmailsStr = newSharedEmailsStr;
    }

    public String getAlreadySharedEmailStr() {
        return alreadySharedEmailStr;
    }

    public void setAlreadySharedEmailStr(String alreadySharedEmailStr) {
        this.alreadySharedEmailStr = alreadySharedEmailStr;
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
}
