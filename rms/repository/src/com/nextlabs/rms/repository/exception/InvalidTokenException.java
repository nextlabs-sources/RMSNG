package com.nextlabs.rms.repository.exception;

import com.nextlabs.rms.json.Repository;

public class InvalidTokenException extends UnauthorizedRepositoryException {

    private static final long serialVersionUID = 4113405042093743090L;

    private final Repository repository;

    public InvalidTokenException(String msg) {
        super(msg);
        this.repository = null;
    }

    public InvalidTokenException(String msg, Throwable e) {
        super(msg, e);
        this.repository = null;
    }

    public InvalidTokenException(String msg, Throwable e, Repository repository) {
        super(msg, e);
        this.repository = repository;
    }

    public Repository getRepo() {
        return this.repository;
    }
}
