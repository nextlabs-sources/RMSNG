/**
 *
 */
package com.nextlabs.rms.exception;

/**
 * @author nnallagatla
 *
 */
public class DuplicateRepositoryNameException extends Exception {

    private final String repoName;

    private static final long serialVersionUID = 3620980537687315841L;

    public DuplicateRepositoryNameException(String repoName) {
        this.repoName = repoName;
    }

    /**
     * @param repoName
     * @param message
     */
    public DuplicateRepositoryNameException(String repoName, String message) {
        super(message);
        this.repoName = repoName;
    }

    /**
    * @return repoName
    */
    public String getRepoName() {
        return repoName;
    }

}
