package com.nextlabs.rms.viewer.json;

public class PrintFileUrl {

    private String url;

    private String errMsg;

    public PrintFileUrl(String url, String error) {
        this.url = url;
        this.errMsg = error;

    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getError() {
        return errMsg;
    }

    public void setError(String error) {
        this.errMsg = error;
    }

}
