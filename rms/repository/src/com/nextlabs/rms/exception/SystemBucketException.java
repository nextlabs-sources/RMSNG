package com.nextlabs.rms.exception;

public class SystemBucketException extends Exception {

    private static final long serialVersionUID = -3976357141429994035L;

    public SystemBucketException(String message) {
        super(message);
    }

    public SystemBucketException(String message, Throwable cause) {
        super(message, cause);
    }

}
