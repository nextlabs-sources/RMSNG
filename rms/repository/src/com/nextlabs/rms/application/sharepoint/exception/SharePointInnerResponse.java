package com.nextlabs.rms.application.sharepoint.exception;

import com.google.gson.annotations.SerializedName;

public class SharePointInnerResponse {

    @SerializedName("code")
    private String code;
    @SerializedName("errorType")
    private String errorType;
    @SerializedName("debugMessage")
    private String debugMessage;
    @SerializedName("stackTrace")
    private String stackTrace;
    @SerializedName("throwSite")
    private String throwSite;
    @SerializedName("innererror")
    private SharePointInnerResponse innererror;

    public String getCode() {
        return code;
    }

    public String getDebugMessage() {
        return debugMessage;
    }

    public String getErrorType() {
        return errorType;
    }

    public SharePointInnerResponse getInnererror() {
        return innererror;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public String getThrowSite() {
        return throwSite;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setDebugMessage(String debugMessage) {
        this.debugMessage = debugMessage;
    }

    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }

    public void setInnererror(SharePointInnerResponse innererror) {
        this.innererror = innererror;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public void setThrowSite(String throwSite) {
        this.throwSite = throwSite;
    }
}
