package com.nextlabs.rms.repository.onedrive.exception;

public class OneDriveServiceException extends RuntimeException {

    private static final long serialVersionUID = 1039996722239826337L;
    private final transient OneDriveErrorResponse error;
    private final int statusCode;

    public OneDriveServiceException(int statusCode, String msg, OneDriveErrorResponse error) {
        super(msg);
        this.error = error;
        this.statusCode = statusCode;
    }

    public OneDriveErrorResponse getError() {
        return error;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
