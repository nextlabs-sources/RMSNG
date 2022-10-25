package com.nextlabs.rms.repository.sharepoint.response;

import com.google.gson.annotations.SerializedName;

public class SharePointFileMetadata {

    @SerializedName("UniqueId")
    private String uniqueId;
    @SerializedName("Name")
    private String name;
    @SerializedName("ServerRelativeUrl")
    private String serverRelativeUrl;

    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getServerRelativeUrl() {
        return serverRelativeUrl;
    }

    public void setServerRelativeUrl(String serverRelativeUrl) {
        this.serverRelativeUrl = serverRelativeUrl;
    }
}
