package com.nextlabs.common.shared;

public class Crash {

    private String hash;
    private String clientId;
    private String stacktrace;
    private String log;

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getHash() {
        return hash;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setStacktrace(String stacktrace) {
        this.stacktrace = stacktrace;
    }

    public String getStacktrace() {
        return stacktrace;
    }

    public void setLog(String log) {
        this.log = log;
    }

    public String getLog() {
        return log;
    }
}
