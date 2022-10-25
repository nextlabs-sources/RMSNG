package com.nextlabs.rms.viewer.json;

import java.io.Serializable;

public class SharedFile implements Serializable {

    private static final long serialVersionUID = 1L;
    private String transactionId;
    private String transactionCode;

    public SharedFile(String transactionId, String transactionCode) {
        this.transactionId = transactionId;
        this.transactionCode = transactionCode;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getTransactionCode() {
        return transactionCode;
    }

    public void setTransactionCode(String transactionCode) {
        this.transactionCode = transactionCode;
    }

}
