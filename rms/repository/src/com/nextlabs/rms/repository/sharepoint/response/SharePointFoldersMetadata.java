package com.nextlabs.rms.repository.sharepoint.response;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SharePointFoldersMetadata {

    @SerializedName("value")
    private List<SharePointFileMetadata> values;

    public List<SharePointFileMetadata> getValues() {
        return values;
    }

    public void setValues(List<SharePointFileMetadata> values) {
        this.values = values;
    }
}
