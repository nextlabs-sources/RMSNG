package com.nextlabs.rms.repository.defaultrepo;

public final class InvalidDefaultRepositoryException extends Exception {

    private static final long serialVersionUID = 1L;

    public InvalidDefaultRepositoryException(String msg) {
        super(msg);
    }

    public InvalidDefaultRepositoryException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
