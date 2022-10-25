package com.nextlabs.rms.repository.onedrive.type;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class OneDriveItems {

    @SerializedName("value")
    private List<OneDriveItem> value;

    /**
     * The url to the next page of this collection, or null
     */
    @SerializedName("@odata.nextLink")
    private String nextLink;

    public List<OneDriveItem> getValue() {
        return value;
    }

    public void setValue(List<OneDriveItem> value) {
        this.value = value;
    }

    public String getNextLink() {
        return nextLink;
    }

    public void setNextLink(String nextLink) {
        this.nextLink = nextLink;
    }
}
