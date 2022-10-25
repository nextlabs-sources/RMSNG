package com.nextlabs.rms.json;

import com.nextlabs.common.shared.JsonExpiry;

import java.util.Map;

public class FileDetails {

    private Map<String, String[]> tags;
    private String[] rights;
    private boolean owner;
    private boolean nxl;
    private boolean revoked;
    private String duid;
    private JsonExpiry validity;
    private int protectionType;

    public FileDetails() {
        this.protectionType = -1;
    }

    public Map<String, String[]> getTags() {
        return tags;
    }

    public void setTags(Map<String, String[]> tagsMap) {
        this.tags = tagsMap;
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

    public boolean isNxl() {
        return nxl;
    }

    public void setNxl(boolean nxl) {
        this.nxl = nxl;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

    public String getDuid() {
        return duid;
    }

    public void setDuid(String duid) {
        this.duid = duid;
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
