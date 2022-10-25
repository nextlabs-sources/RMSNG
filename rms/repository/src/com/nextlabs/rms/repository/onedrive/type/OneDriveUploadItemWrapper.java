package com.nextlabs.rms.repository.onedrive.type;

import com.google.gson.annotations.SerializedName;

public class OneDriveUploadItemWrapper {

    @SerializedName("item")
    private OneDriveUploadItem item;

    public void setItem(OneDriveUploadItem item) {
        this.item = item;
    }

    public OneDriveUploadItem getItem() {
        return this.item;
    }

    public OneDriveUploadItemWrapper(OneDriveUploadItem item) {
        this.item = item;
    }
}
