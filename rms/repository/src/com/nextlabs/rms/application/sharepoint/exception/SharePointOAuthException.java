package com.nextlabs.rms.application.sharepoint.exception;

public class SharePointOAuthException extends RuntimeException {

    private static final long serialVersionUID = 4535225599727118561L;
    private final transient SharePointOAuthErrorResponse error;
    private final int statusCode;

    public SharePointOAuthException(int statusCode, String msg, SharePointOAuthErrorResponse error) {
        super(msg);
        this.error = error;
        this.statusCode = statusCode;
    }

    public SharePointOAuthErrorResponse getError() {
        return error;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
