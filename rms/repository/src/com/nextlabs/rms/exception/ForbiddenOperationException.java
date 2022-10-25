/**
 *
 */
package com.nextlabs.rms.exception;

/**
 * @author nnallagatla
 *
 */
public class ForbiddenOperationException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = 6376301933965470052L;

    /**
     *
     */
    public ForbiddenOperationException() {

    }

    /**
     * @param message
     */
    public ForbiddenOperationException(String message) {
        super(message);
    }

    /**
     * @param cause
     */
    public ForbiddenOperationException(Throwable cause) {
        super(cause);
    }

    /**
     * @param message
     * @param cause
     */
    public ForbiddenOperationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param message
     * @param cause
     * @param enableSuppression
     * @param writableStackTrace
     */
    public ForbiddenOperationException(String message, Throwable cause, boolean enableSuppression,
        boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
