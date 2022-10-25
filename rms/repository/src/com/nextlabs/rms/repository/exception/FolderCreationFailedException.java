package com.nextlabs.rms.repository.exception;

public class FolderCreationFailedException extends IOException {

    private static final long serialVersionUID = 8036351271446365358L;

    public FolderCreationFailedException(String msg) {
        super(msg);
    }

    public FolderCreationFailedException(String msg, Throwable e) {
        super(msg, e);
    }
}
