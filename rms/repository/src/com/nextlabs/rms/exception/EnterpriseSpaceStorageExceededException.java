package com.nextlabs.rms.exception;

public class EnterpriseSpaceStorageExceededException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private final long storage;

    public EnterpriseSpaceStorageExceededException() {
        this(0);
    }

    public EnterpriseSpaceStorageExceededException(long storageUsed) {
        this.storage = storageUsed;
    }

    public long getStorage() {
        return storage;
    }

}
