package com.nextlabs.rms.repository.sharepoint.response;

import com.google.gson.annotations.SerializedName;

public class ContextInfo {

    @SerializedName("FormDigestTimeoutSeconds")
    private long formDigestTimeout;
    @SerializedName("FormDigestValue")
    private String formDigestValue;
    @SerializedName("LibraryVersion")
    private String libaryVersion;
    @SerializedName("SiteFullUrl")
    private String siteFullUrl;
    @SerializedName("WebFullUrl")
    private String webFullUrl;

    public long getFormDigestTimeout() {
        return formDigestTimeout;
    }

    public String getFormDigestValue() {
        return formDigestValue;
    }

    public String getLibaryVersion() {
        return libaryVersion;
    }

    public String getSiteFullUrl() {
        return siteFullUrl;
    }

    public String getWebFullUrl() {
        return webFullUrl;
    }

    public void setFormDigestTimeout(long formDigestTimeout) {
        this.formDigestTimeout = formDigestTimeout;
    }

    public void setFormDigestValue(String formDigestValue) {
        this.formDigestValue = formDigestValue;
    }

    public void setLibaryVersion(String libaryVersion) {
        this.libaryVersion = libaryVersion;
    }

    public void setSiteFullUrl(String siteFullUrl) {
        this.siteFullUrl = siteFullUrl;
    }

    public void setWebFullUrl(String webFullUrl) {
        this.webFullUrl = webFullUrl;
    }
}
