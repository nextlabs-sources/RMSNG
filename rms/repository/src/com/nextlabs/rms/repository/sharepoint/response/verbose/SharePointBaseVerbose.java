package com.nextlabs.rms.repository.sharepoint.response.verbose;

import com.google.gson.annotations.SerializedName;
import com.nextlabs.rms.repository.sharepoint.response.SharePointBase;

public class SharePointBaseVerbose {

    @SerializedName("d")
    private SharePointBase base;

    public SharePointBase getBase() {
        return base;
    }

    public void setBase(SharePointBase base) {
        this.base = base;
    }

}
