package com.nextlabs.common.shared;

import java.util.Set;

public class JsonMyVaultMetadata {

    private String name;
    private Set<String> recipients;
    private String fileLink;
    private Long protectedOn;
    private Long sharedOn;
    private String[] rights;
    private boolean shared;
    private boolean deleted;
    private boolean revoked;
    private JsonExpiry validity;
    private int protectionType = -1;

    public JsonExpiry getValidity() {
        return validity;
    }

    public void setValidity(JsonExpiry validity) {
        this.validity = validity;
    }

    public String getFileLink() {
        return fileLink;
    }

    public String getName() {
        return name;
    }

    public Set<String> getRecipients() {
        return recipients;
    }

    public String[] getRights() {
        return rights;
    }

    public Long getProtectedOn() {
        return protectedOn;
    }

    public void setProtectedOn(Long protectedOn) {
        this.protectedOn = protectedOn;
    }

    public Long getSharedOn() {
        return sharedOn;
    }

    public void setFileLink(String fileLink) {
        this.fileLink = fileLink;
    }

    public void setFileName(String fileName) {
        this.name = fileName;
    }

    public void setRecipients(Set<String> recipients) {
        this.recipients = recipients;
    }

    public void setRights(String[] rights) {
        this.rights = rights;
    }

    public void setSharedOn(Long sharedOn) {
        this.sharedOn = sharedOn;
    }

    public boolean isShared() {
        return shared;
    }

    public void setShared(boolean shared) {
        this.shared = shared;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

    public int getProtectionType() {
        return protectionType;
    }

    public void setProtectionType(int protectionType) {
        this.protectionType = protectionType;
    }

}
