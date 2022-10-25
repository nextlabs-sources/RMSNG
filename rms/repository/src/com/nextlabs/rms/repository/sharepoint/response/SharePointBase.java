package com.nextlabs.rms.repository.sharepoint.response;

import com.google.gson.annotations.SerializedName;

public class SharePointBase {

    @SerializedName("ServerRelativeUrl")
    private String serverRelativeUrl;

    public String getServerRelativeUrl() {
        return serverRelativeUrl;
    }

    public void setServerRelativeUrl(String serverRelativeUrl) {
        this.serverRelativeUrl = serverRelativeUrl;
    }
}
