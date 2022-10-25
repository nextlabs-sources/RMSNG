package com.nextlabs.rms.repository.sharepoint;

import com.nextlabs.rms.repository.sharepoint.response.SPRestErrorResponse;

public class SPRestServiceException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private final int statusCode;
    private final SPRestErrorResponse error;

    public SPRestServiceException(int statusCode, String msg, SPRestErrorResponse error) {
        super(msg);
        this.error = error;
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public SPRestErrorResponse getError() {
        return error;
    }
}
