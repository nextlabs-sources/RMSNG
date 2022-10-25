package com.nextlabs.rms.repository.exception;

public class NonUniqueFileException extends IOException {

    private static final long serialVersionUID = 701663093668639400L;
    private final String filename;

    public NonUniqueFileException(String msg, String filename) {
        super(msg);
        this.filename = filename;
    }

    public String getFilename() {
        return filename;
    }
}
