package com.nextlabs.rms.exception;

public class ApplicationRepositoryException extends Exception {

    private static final long serialVersionUID = -4480696622112154590L;

    public ApplicationRepositoryException(String message) {
        super(message);
    }

    public ApplicationRepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
