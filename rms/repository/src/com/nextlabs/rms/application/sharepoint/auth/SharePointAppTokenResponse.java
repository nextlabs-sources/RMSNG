package com.nextlabs.rms.application.sharepoint.auth;

import com.google.gson.annotations.SerializedName;
import com.nextlabs.rms.auth.ITokenResponse;

public class SharePointAppTokenResponse implements ITokenResponse {

    @SerializedName("token_type")
    private String tokenType;
    @SerializedName("expires_in")
    private Long expiresIn;
    @SerializedName("ext_expires_in")
    private Long extExpiresIn;
    @SerializedName("access_token")
    private String accessToken;

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    @Override
    public Long getExpiresInSeconds() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public Long getExtExpiresIn() {
        return extExpiresIn;
    }

    public void setExtExpiresIn(Long extExpiresIn) {
        this.extExpiresIn = extExpiresIn;
    }

    @Override
    public String getAccessToken() {
        return accessToken;
    }

    @Override
    public String getRefreshToken() {
        return null;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}
