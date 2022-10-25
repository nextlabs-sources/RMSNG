package com.nextlabs.rms.exception;

public class FileExpiredException extends Exception {

    private static final long serialVersionUID = -1006364562734150309L;

    public FileExpiredException() {
    }

    public FileExpiredException(String message) {
        super(message);
    }
}
