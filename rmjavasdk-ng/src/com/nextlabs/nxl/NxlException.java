package com.nextlabs.nxl;

public class NxlException extends Exception {

    private static final long serialVersionUID = 1L;

    public NxlException() {
    }

    public NxlException(String message) {
        super(message);
    }

    public NxlException(String message, Throwable cause) {
        super(message, cause);
    }

    public NxlException(Throwable cause) {
        super(cause);
    }
}
