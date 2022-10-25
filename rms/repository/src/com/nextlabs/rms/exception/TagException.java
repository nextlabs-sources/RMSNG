package com.nextlabs.rms.exception;

public class TagException extends Exception {

    private static final long serialVersionUID = -3976357141429994035L;

    public TagException(String errMsg) {
        super(errMsg);
    }

    public TagException(Exception exception) {
        super(exception);
    }
}
