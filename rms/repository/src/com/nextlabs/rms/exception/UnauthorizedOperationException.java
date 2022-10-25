/**
 *
 */
package com.nextlabs.rms.exception;

/**
 * @author nnallagatla
 *
 */
public class UnauthorizedOperationException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = 3620980532720315841L;

    /**
     *
     */
    public UnauthorizedOperationException() {

    }

    /**
     * @param message
     */
    public UnauthorizedOperationException(String message) {
        super(message);
    }

    /**
     * @param cause
     */
    public UnauthorizedOperationException(Throwable cause) {
        super(cause);
    }

    /**
     * @param message
     * @param cause
     */
    public UnauthorizedOperationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param message
     * @param cause
     * @param enableSuppression
     * @param writableStackTrace
     */
    public UnauthorizedOperationException(String message, Throwable cause, boolean enableSuppression,
        boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
