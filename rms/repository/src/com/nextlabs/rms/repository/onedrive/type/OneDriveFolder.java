package com.nextlabs.rms.repository.onedrive.type;

import com.google.gson.annotations.SerializedName;

public class OneDriveFolder {

    @SerializedName("childCount")
    private Integer childCount;

    public Integer getChildCount() {
        return childCount;
    }

    public void setChildCount(Integer childCount) {
        this.childCount = childCount;
    }
}
