package com.nextlabs.rms.repository.exception;

public class InSufficientSpaceException extends IOException {

    private static final long serialVersionUID = 1L;

    public InSufficientSpaceException(String msg) {
        super(msg);

    }

    public InSufficientSpaceException(String msg, Throwable e) {
        super(msg, e);
    }

}
