package com.nextlabs.rms.application.sharepoint.type;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SharePointItems {

    @SerializedName("@odata.context")
    private String dataCtx;
    @SerializedName("@odata.nextLink")
    private String nextLink;
    @SerializedName("value")
    private List<SharePointItem> value;

    public String getDataCtx() {
        return dataCtx;
    }

    public void setDataCtx(String dataCtx) {
        this.dataCtx = dataCtx;
    }

    public String getNextLink() {
        return nextLink;
    }

    public void setNextLink(String nextLink) {
        this.nextLink = nextLink;
    }

    public List<SharePointItem> getValue() {
        return value;
    }

    public void setValue(List<SharePointItem> value) {
        this.value = value;
    }
}
