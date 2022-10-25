package com.nextlabs.rms.exception;

public class UnauthorizedApplicationRepositoryException extends ApplicationRepositoryException {

    private static final long serialVersionUID = 3114013941483482064L;
    private final String repoName;

    public UnauthorizedApplicationRepositoryException(String msg, Throwable e) {
        super(msg, e);
        this.repoName = null;
    }

    public UnauthorizedApplicationRepositoryException(String msg) {
        super(msg);
        this.repoName = null;
    }

    public UnauthorizedApplicationRepositoryException(String msg, Throwable e, String repoName) {
        super(msg, e);
        this.repoName = repoName;
    }

    public String getRepoName() {
        return this.repoName;
    }
}
