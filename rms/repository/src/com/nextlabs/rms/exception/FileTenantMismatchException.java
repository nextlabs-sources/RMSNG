package com.nextlabs.rms.exception;

public class FileTenantMismatchException extends Exception {

    private static final long serialVersionUID = 1L;

    public FileTenantMismatchException() {
    }

    public FileTenantMismatchException(String message) {
        super(message);
    }

    public FileTenantMismatchException(Throwable cause) {
        super(cause);
    }

    public FileTenantMismatchException(String message, Throwable cause) {
        super(message, cause);
    }
}
