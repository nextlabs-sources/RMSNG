package com.nextlabs.rms.application.sharepoint.exception;

import com.nextlabs.rms.exception.ApplicationRepositoryException;

public class DriveNotFoundException extends ApplicationRepositoryException {

    private static final long serialVersionUID = 8361177449075617416L;

    public DriveNotFoundException(String message) {
        super(message);
    }

    public DriveNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
