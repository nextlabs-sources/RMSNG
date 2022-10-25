package com.nextlabs.rms.exception;

public class FileAlreadyRevokedException extends Exception {

    private static final long serialVersionUID = -1006364562734150309L;

    public FileAlreadyRevokedException() {
    }

    public FileAlreadyRevokedException(String message) {
        super(message);
    }

    public FileAlreadyRevokedException(Throwable cause) {
        super(cause);
    }

    public FileAlreadyRevokedException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileAlreadyRevokedException(String message, Throwable cause, boolean enableSuppression,
        boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
