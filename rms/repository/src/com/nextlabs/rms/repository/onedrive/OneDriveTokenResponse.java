package com.nextlabs.rms.repository.onedrive;

import com.google.gson.annotations.SerializedName;
import com.nextlabs.rms.auth.ITokenResponse;

public class OneDriveTokenResponse implements ITokenResponse {

    @SerializedName("token_type")
    private String tokenType;
    @SerializedName("expires_in")
    private Long expiresInSeconds;
    @SerializedName("scope")
    private String scope;
    @SerializedName("access_token")
    private String accessToken;
    @SerializedName("refresh_token")
    private String refreshToken;

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Long getExpiresInSeconds() {
        return expiresInSeconds;
    }

    public void setExpiresInSeconds(Long expiresInSeconds) {
        this.expiresInSeconds = expiresInSeconds;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
