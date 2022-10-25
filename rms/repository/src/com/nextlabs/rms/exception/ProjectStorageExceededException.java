package com.nextlabs.rms.exception;

public class ProjectStorageExceededException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = -3489133498083181516L;

    private final long storage;

    public ProjectStorageExceededException() {
        this(0);
    }

    public ProjectStorageExceededException(long storageUsed) {
        this.storage = storageUsed;
    }

    public long getStorage() {
        return storage;
    }

}
