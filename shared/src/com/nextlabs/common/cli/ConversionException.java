package com.nextlabs.common.cli;

public class ConversionException extends RuntimeException {

    private static final long serialVersionUID = -691510243981653801L;

    public ConversionException(String msg) {
        super(msg);
    }

    public ConversionException(String msg, Throwable throwable) {
        super(msg, throwable);
    }
}
