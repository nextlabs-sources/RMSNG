package com.nextlabs.rms.viewer.exception;

public class UnsupportedFormatException extends Exception {

    private static final long serialVersionUID = -1843278910175455248L;
    private final String extension;

    public UnsupportedFormatException(String extension) {
        this.extension = extension;
    }

    public String getExtension() {
        return extension;
    }
}
