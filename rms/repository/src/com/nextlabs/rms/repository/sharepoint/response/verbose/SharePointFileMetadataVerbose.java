package com.nextlabs.rms.repository.sharepoint.response.verbose;

import com.google.gson.annotations.SerializedName;
import com.nextlabs.rms.repository.sharepoint.response.SharePointFileMetadata;

public class SharePointFileMetadataVerbose {

    @SerializedName("d")
    private SharePointFileMetadata metaData;

    public SharePointFileMetadata getMetadata() {
        return metaData;
    }

    public void setMetadata(SharePointFileMetadata metaData) {
        this.metaData = metaData;
    }
}
