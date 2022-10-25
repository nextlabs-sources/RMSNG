package com.nextlabs.rms.json;

public class RepositoryRedirectOperationResult extends OperationResult {

    private String redirectUrl;

    private int redirectCode;

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    public int getRedirectCode() {
        return redirectCode;
    }

    public void setRedirectCode(int redirectCode) {
        this.redirectCode = redirectCode;
    }
}
