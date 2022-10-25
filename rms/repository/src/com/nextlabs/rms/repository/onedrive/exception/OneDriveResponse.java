package com.nextlabs.rms.repository.onedrive.exception;

import com.google.gson.annotations.SerializedName;

public class OneDriveResponse {

    @SerializedName("message")
    private String message;

    @SerializedName("code")
    private String code;

    @SerializedName("innererror")
    private OneDriveInnerResponse innerError;

    public String getCode() {
        return code;
    }

    public OneDriveInnerResponse getInnerError() {
        return innerError;
    }

    public String getMessage() {
        return message;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setInnerError(OneDriveInnerResponse innererror) {
        this.innerError = innererror;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
