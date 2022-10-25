package com.nextlabs.rms.repository.exception;

public class FileConflictException extends IOException {

    private static final long serialVersionUID = 1L;

    private final String folderName;

    public FileConflictException(String msg, Throwable e, String folderName) {
        super(msg, e);
        this.folderName = folderName;
    }

    public FileConflictException(String msg) {
        super(msg);
        this.folderName = null;
    }

    public String getFolderName() {
        return this.folderName;
    }
}
