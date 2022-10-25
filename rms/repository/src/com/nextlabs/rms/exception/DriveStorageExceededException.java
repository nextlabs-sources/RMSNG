package com.nextlabs.rms.exception;

public class DriveStorageExceededException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = 1779294292639168982L;

    private final long storage;

    public DriveStorageExceededException() {
        this(0);
    }

    public DriveStorageExceededException(long storageUsed) {
        this.storage = storageUsed;
    }

    public long getStorage() {
        return storage;
    }
}
