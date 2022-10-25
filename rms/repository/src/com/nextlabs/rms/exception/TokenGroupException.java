package com.nextlabs.rms.exception;

public class TokenGroupException extends Exception {

    private static final long serialVersionUID = -3976357141429994035L;

    public TokenGroupException(String message) {
        super(message);
    }

    public TokenGroupException(String message, Throwable cause) {
        super(message, cause);
    }

}
