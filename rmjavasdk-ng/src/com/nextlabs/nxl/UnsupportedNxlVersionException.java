package com.nextlabs.nxl;

public class UnsupportedNxlVersionException extends NxlException {

    private static final long serialVersionUID = 1L;

    public UnsupportedNxlVersionException() {
    }

    public UnsupportedNxlVersionException(String message) {
        super(message);
    }

}
