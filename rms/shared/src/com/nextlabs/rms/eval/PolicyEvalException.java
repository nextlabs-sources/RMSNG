package com.nextlabs.rms.eval;

public class PolicyEvalException extends Exception {

    private static final long serialVersionUID = 7020635405877631039L;

    public PolicyEvalException(String errMsg) {
        super(errMsg);
    }

    public PolicyEvalException(String errMsg, Throwable e) {
        super(errMsg, e);
    }

}
