package com.nextlabs.rms.exception;

public class ValidateException extends RuntimeException {

    private final int errorCode;

    private final String message;

    private final String extFields;

    public ValidateException(int errorCode, String message) {
        this.errorCode = errorCode;
        this.message = message;
        this.extFields = null;
    }

    public ValidateException(int errorCode, String message, String extFields) {
        this.errorCode = errorCode;
        this.message = message;
        this.extFields = extFields;
    }

    public ValidateException(String message) {
        this.message = message;
        this.extFields = null;
        this.errorCode = 400;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public String getExtFields() {
        return extFields;
    }

    public String getMessage() {
        return message;
    }

}
