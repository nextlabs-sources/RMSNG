package com.nextlabs.rms.application.sharepoint.exception;

import com.nextlabs.rms.exception.ApplicationRepositoryException;

public class InvalidHostNameException extends ApplicationRepositoryException {

    private static final long serialVersionUID = 1632467877697307577L;

    public InvalidHostNameException(String message) {
        super(message);
    }

    public InvalidHostNameException(String message, Throwable cause) {
        super(message, cause);
    }
}
