package com.nextlabs.nxl.exception;

public class FIPSError extends Error { //NOPMD

    private static final long serialVersionUID = 1L;
    private final String message;

    public FIPSError(String message, Throwable cause) {
        super(message, cause);
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
