package com.nextlabs.rms.exception;

public class TenantException extends Exception {

    private static final long serialVersionUID = -4251181823947987584L;

    public TenantException() {
    }

    public TenantException(String message) {
        super(message);
    }

    public TenantException(Throwable cause) {
        super(cause);
    }

    public TenantException(String message, Throwable cause) {
        super(message, cause);
    }

    public TenantException(String message, Throwable cause, boolean enableSuppression,
        boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
