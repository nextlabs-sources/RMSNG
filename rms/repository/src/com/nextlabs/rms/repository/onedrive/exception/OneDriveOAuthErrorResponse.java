package com.nextlabs.rms.repository.onedrive.exception;

import com.google.gson.annotations.SerializedName;

public class OneDriveOAuthErrorResponse {

    @SerializedName("error")
    private String error;
    @SerializedName("error_description")
    private String description;

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
