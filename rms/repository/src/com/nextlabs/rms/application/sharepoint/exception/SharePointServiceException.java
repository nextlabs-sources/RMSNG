package com.nextlabs.rms.application.sharepoint.exception;

public class SharePointServiceException extends RuntimeException {

    private static final long serialVersionUID = -3839732126177286634L;
    private final transient SharePointErrorResponse error;
    private final int statusCode;

    public SharePointServiceException(int statusCode, String msg, SharePointErrorResponse error) {
        super(msg);
        this.error = error;
        this.statusCode = statusCode;
    }

    public SharePointErrorResponse getError() {
        return error;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
