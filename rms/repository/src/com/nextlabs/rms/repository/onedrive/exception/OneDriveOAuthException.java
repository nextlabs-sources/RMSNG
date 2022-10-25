package com.nextlabs.rms.repository.onedrive.exception;

import com.google.gson.annotations.SerializedName;

public class OneDriveOAuthException extends RuntimeException {

    private static final long serialVersionUID = -3858181508376490363L;
    private final transient OneDriveOAuthErrorResponse error;
    private final int statusCode;

    public OneDriveOAuthException(int statusCode, String msg, OneDriveOAuthErrorResponse error) {
        super(msg);
        this.error = error;
        this.statusCode = statusCode;
    }

    public OneDriveOAuthErrorResponse getError() {
        return error;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public static class OneDriveResponse {

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
}
