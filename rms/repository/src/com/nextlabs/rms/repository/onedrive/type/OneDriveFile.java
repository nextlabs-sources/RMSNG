package com.nextlabs.rms.repository.onedrive.type;

import com.google.gson.annotations.SerializedName;

public class OneDriveFile {

    @SerializedName("mimeType")
    private String mimeType;

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }
}
