package com.nextlabs.rms.exception;

public class RMSException extends Exception {

    private static final long serialVersionUID = -3976357141429994035L;

    public RMSException(String errMsg) {
        super(errMsg);
    }

    public RMSException(String msg, Throwable e) {
        super(msg, e);
    }
}
