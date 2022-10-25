package com.nextlabs.rms.repository.exception;

public class FileNotFoundException extends IOException {

    private static final long serialVersionUID = 8036351271446365358L;

    public FileNotFoundException(String msg) {
        super(msg);
    }

    public FileNotFoundException(String msg, Throwable e) {
        super(msg, e);
    }
}
