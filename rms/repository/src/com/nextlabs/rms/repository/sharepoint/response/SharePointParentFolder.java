package com.nextlabs.rms.repository.sharepoint.response;

import com.google.gson.annotations.SerializedName;

public class SharePointParentFolder {

    @SerializedName("UniqueId")
    private String uniqueId;
    @SerializedName("ServerRelativeUrl")
    private String serverRelativeUrl;
    @SerializedName("Name")
    private String name;
    @SerializedName("ItemCount")
    private Integer itemCount;

    public Integer getItemCount() {
        return itemCount;
    }

    public String getName() {
        return name;
    }

    public String getServerRelativeUrl() {
        return serverRelativeUrl;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public void setItemCount(Integer itemCount) {
        this.itemCount = itemCount;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setServerRelativeUrl(String serverRelativeUrl) {
        this.serverRelativeUrl = serverRelativeUrl;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }
}
