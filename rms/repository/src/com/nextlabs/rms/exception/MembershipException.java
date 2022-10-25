package com.nextlabs.rms.exception;

public class MembershipException extends Exception {

    private static final long serialVersionUID = 1L;

    public MembershipException() {
    }

    public MembershipException(String message) {
        super(message);
    }

    public MembershipException(Throwable cause) {
        super(cause);
    }

    public MembershipException(String message, Throwable cause) {
        super(message, cause);
    }
}
