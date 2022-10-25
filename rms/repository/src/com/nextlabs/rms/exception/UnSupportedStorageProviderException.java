/**
 *
 */
package com.nextlabs.rms.exception;

/**
 * @author nnallagatla
 *
 */
public class UnSupportedStorageProviderException extends Exception {

    private static final long serialVersionUID = 3420171537687315841L;

    public UnSupportedStorageProviderException() {

    }

    /**
     * @param message
     */
    public UnSupportedStorageProviderException(String message) {
        super(message);
    }

    /**
     * @param cause
     */
    public UnSupportedStorageProviderException(Throwable cause) {
        super(cause);
    }

    /**
     * @param message
     * @param cause
     */
    public UnSupportedStorageProviderException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param message
     * @param cause
     * @param enableSuppression
     * @param writableStackTrace
     */
    public UnSupportedStorageProviderException(String message, Throwable cause, boolean enableSuppression,
        boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
