package com.nextlabs.rms.repository.exception;

public class IOException extends RepositoryException {

    private static final long serialVersionUID = -7418842071119990634L;

    public IOException(String msg) {
        super(msg);
    }

    public IOException(String msg, Throwable e) {
        super(msg, e);
    }
}
