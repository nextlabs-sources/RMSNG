/**
 *
 */
package com.nextlabs.rms.exception;

/**
 * @author nnallagatla
 *
 */
public class RepositoryAlreadyExists extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = -3552274348695207021L;

    /**
     *
     */
    public RepositoryAlreadyExists() {
    }

    /**
     * @param message
     */
    public RepositoryAlreadyExists(String message) {
        super(message);
    }

    /**
     * @param cause
     */
    public RepositoryAlreadyExists(Throwable cause) {
        super(cause);
    }

    /**
     * @param message
     * @param cause
     */
    public RepositoryAlreadyExists(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param message
     * @param cause
     * @param enableSuppression
     * @param writableStackTrace
     */
    public RepositoryAlreadyExists(String message, Throwable cause, boolean enableSuppression,
        boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
