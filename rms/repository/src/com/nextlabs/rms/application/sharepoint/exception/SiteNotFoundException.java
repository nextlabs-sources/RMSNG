package com.nextlabs.rms.application.sharepoint.exception;

import com.nextlabs.rms.exception.ApplicationRepositoryException;

public class SiteNotFoundException extends ApplicationRepositoryException {

    private static final long serialVersionUID = 1632267877677307577L;

    public SiteNotFoundException(String message) {
        super(message);
    }

    public SiteNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
