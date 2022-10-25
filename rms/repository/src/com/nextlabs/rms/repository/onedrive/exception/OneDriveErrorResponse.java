package com.nextlabs.rms.repository.onedrive.exception;

import com.google.gson.annotations.SerializedName;

public class OneDriveErrorResponse {

    @SerializedName("error")
    private OneDriveOAuthException.OneDriveResponse error;

    public OneDriveOAuthException.OneDriveResponse getError() {
        return error;
    }

    public void setError(OneDriveOAuthException.OneDriveResponse error) {
        this.error = error;
    }
}
