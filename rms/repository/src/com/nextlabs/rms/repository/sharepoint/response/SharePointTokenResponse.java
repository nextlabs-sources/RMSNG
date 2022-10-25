package com.nextlabs.rms.repository.sharepoint.response;

import com.google.gson.annotations.SerializedName;

public class SharePointTokenResponse {

    @SerializedName("token_type")
    private String tokenType;
    @SerializedName("access_token")
    private String accessToken;
    @SerializedName("resource")
    private String resource;
    @SerializedName("expires_in")
    private Long expiresIn;
    @SerializedName("expires_on")
    private Long expiresOn;
    @SerializedName("not_before")
    private Long notBefore;

    public String getAccessToken() {
        return accessToken;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public Long getExpiresOn() {
        return expiresOn;
    }

    public Long getNotBefore() {
        return notBefore;
    }

    public String getResource() {
        return resource;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public void setExpiresOn(Long expiresOn) {
        this.expiresOn = expiresOn;
    }

    public void setNotBefore(Long notBefore) {
        this.notBefore = notBefore;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }
}
