package com.nextlabs.rms.application.sharepoint.exception;

import com.google.gson.annotations.SerializedName;

public class SharePointResponse {

    @SerializedName("message")
    private String message;
    @SerializedName("code")
    private String code;
    @SerializedName("innererror")
    private SharePointInnerResponse innerError;

    public String getCode() {
        return code;
    }

    public SharePointInnerResponse getInnerError() {
        return innerError;
    }

    public String getMessage() {
        return message;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setInnerError(SharePointInnerResponse innererror) {
        this.innerError = innererror;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
