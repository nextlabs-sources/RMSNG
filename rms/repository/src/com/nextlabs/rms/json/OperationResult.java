package com.nextlabs.rms.json;

public class OperationResult {

    private Integer statusCode;

    private boolean result;

    private String message;

    public OperationResult() {
    }

    public OperationResult(int statusCode, boolean result, String message) {
        this.statusCode = statusCode;
        this.result = result;
        this.message = message;
    }

    public boolean isResult() {
        return result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

}
