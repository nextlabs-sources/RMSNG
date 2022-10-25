package com.nextlabs.rms.repository.onedrive;

import com.google.gson.annotations.SerializedName;

public class OneDriveSharingLink {

    @SerializedName("type")
    private String type;
    @SerializedName("scope")
    private String scope;
    @SerializedName("webUrl")
    private String webUrl;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getWebUrl() {
        return webUrl;
    }

    public void setWebUrl(String webUrl) {
        this.webUrl = webUrl;
    }
}
