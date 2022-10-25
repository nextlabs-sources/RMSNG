package com.nextlabs.rms.exception;

public class VaultStorageExceededException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = -7609690171801379694L;

    private final long storage;

    public VaultStorageExceededException() {
        this(0);
    }

    public VaultStorageExceededException(long storageUsed) {
        this.storage = storageUsed;
    }

    public long getStorage() {
        return storage;
    }

}
