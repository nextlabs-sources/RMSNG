package com.nextlabs.rms.application.sharepoint.exception;

import com.google.gson.annotations.SerializedName;

public class SharePointErrorResponse {

    @SerializedName("error")
    private SharePointResponse error;

    public SharePointResponse getError() {
        return error;
    }

    public void setError(SharePointResponse error) {
        this.error = error;
    }
}
