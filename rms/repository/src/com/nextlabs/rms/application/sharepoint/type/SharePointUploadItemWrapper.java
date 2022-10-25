package com.nextlabs.rms.application.sharepoint.type;

import com.google.gson.annotations.SerializedName;

public class SharePointUploadItemWrapper {

    @SerializedName("item")
    private SharePointUploadItem item;

    public void setItem(SharePointUploadItem item) {
        this.item = item;
    }

    public SharePointUploadItem getItem() {
        return this.item;
    }

    public SharePointUploadItemWrapper(SharePointUploadItem item) {
        this.item = item;
    }
}
