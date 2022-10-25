package com.nextlabs.rms.cc.exception;

public class DelegationPolicyException extends Exception {

    private static final long serialVersionUID = -10271344592165137L;
    private final String message;

    public DelegationPolicyException(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }

}
