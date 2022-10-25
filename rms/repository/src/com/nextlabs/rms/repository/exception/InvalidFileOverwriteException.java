package com.nextlabs.rms.repository.exception;

public class InvalidFileOverwriteException extends RepositoryException {

    private static final long serialVersionUID = 1L;

    public InvalidFileOverwriteException(String msg) {
        super(msg);
    }
}
