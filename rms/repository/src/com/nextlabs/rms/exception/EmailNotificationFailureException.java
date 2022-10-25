package com.nextlabs.rms.exception;

/**
 * @author nnallagatla
 *
 */
public class EmailNotificationFailureException extends Exception {

    private static final long serialVersionUID = -1006364562734150309L;

    public EmailNotificationFailureException() {

    }

    /**
     * @param message
     */
    public EmailNotificationFailureException(String message) {
        super(message);

    }

    /**
     * @param cause
     */
    public EmailNotificationFailureException(Throwable cause) {
        super(cause);

    }

    /**
     * @param message
     * @param cause
     */
    public EmailNotificationFailureException(String message, Throwable cause) {
        super(message, cause);

    }

    /**
     * @param message
     * @param cause
     * @param enableSuppression
     * @param writableStackTrace
     */
    public EmailNotificationFailureException(String message, Throwable cause, boolean enableSuppression,
        boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);

    }
}
