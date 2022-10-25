package com.nextlabs.rms.repository.exception;

public class RepositoryException extends Exception {

    private static final long serialVersionUID = 4877284884285521128L;

    public RepositoryException(String msg, Throwable e) {
        super(msg, e);
    }

    public RepositoryException(String msg) {
        super(msg);
    }
}
