/**
 *
 */
package com.nextlabs.rms.exception;

/**
 * @author nnallagatla
 *
 */
public class UserNotFoundException extends Exception {

    private static final long serialVersionUID = 2278993119841935524L;

    private final long userId;

    public UserNotFoundException() {
        this(0);
    }

    public UserNotFoundException(long userId) {
        this.userId = userId;
    }

    /**
     * @param message
     */
    public UserNotFoundException(String message) {
        super(message);
        this.userId = 0;
    }

    /**
     * @param message
     * @param cause
     */
    public UserNotFoundException(String message, Throwable cause) {
        super(message, cause);
        this.userId = 0;
    }

    /**
     * @param message
     * @param cause
     * @param enableSuppression
     * @param writableStackTrace
     */
    public UserNotFoundException(String message, Throwable cause, boolean enableSuppression,
        boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.userId = 0;
    }

    /**
     * @param cause
     */
    public UserNotFoundException(Throwable cause) {
        super(cause);
        this.userId = 0;
    }

    public long getUserId() {
        return userId;
    }
}
