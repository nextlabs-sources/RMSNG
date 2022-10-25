/**
 *
 */
package com.nextlabs.rms.repository.defaultrepo;

/**
 * @author nnallagatla
 *
 */
public class RMSRepositorySearchException extends Exception {

    private static final long serialVersionUID = 8008338625829444302L;

    public RMSRepositorySearchException(String message) {
        super(message);
    }

    public RMSRepositorySearchException(String message, Throwable cause) {
        super(message, cause);
    }

    public RMSRepositorySearchException(Throwable cause) {
        super(cause);
    }
}
