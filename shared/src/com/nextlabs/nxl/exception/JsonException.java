package com.nextlabs.nxl.exception;

public class JsonException extends Exception {

    private static final long serialVersionUID = 786854858071644255L;
    private final int statusCode;
    private final String message;

    public JsonException(int statusCode, String message) {
        this.statusCode = statusCode;
        this.message = message;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getMessage() {
        return message;
    }
}
