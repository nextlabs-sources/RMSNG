/**
 *
 */
package com.nextlabs.rms.exception;

/**
 * @author nnallagatla
 *
 */
public class RepositoryNotFoundException extends Exception {

    private static final long serialVersionUID = -1027109578392165137L;

    private final String repoId;

    public RepositoryNotFoundException(String repoId) {
        this.repoId = repoId;
    }

    public String getRepoId() {
        return repoId;
    }
}
