package com.nextlabs.rms.repository.exception;

public class RepositoryAccessException extends Exception {

    private static final long serialVersionUID = 1L;

    private final String repoName;

    public RepositoryAccessException(String msg, Throwable e, String repoName) {
        super(msg, e);
        this.repoName = repoName;
    }

    public String getRepoName() {
        return this.repoName;
    }
}
