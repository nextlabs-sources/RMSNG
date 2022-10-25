package com.nextlabs.rms.viewer.exception;

public class CacheNotFoundException extends RMSException {

    public CacheNotFoundException(String errMsg) {
        super(errMsg);
    }

    public CacheNotFoundException(String msg, Throwable e) {
        super(msg, e);
    }
}
