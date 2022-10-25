package com.nextlabs.rms.repository.sharepoint.response.verbose;

import com.google.gson.annotations.SerializedName;
import com.nextlabs.rms.repository.sharepoint.response.SharePointFileUploadResponse;

public class SharePointFileUploadResponseVerbose {

    @SerializedName("d")
    SharePointFileUploadResponse data;

    public SharePointFileUploadResponse getData() {
        return data;
    }

    public void setData(SharePointFileUploadResponse data) {
        this.data = data;
    }
}
