package com.nextlabs.rms.auth;

public interface ITokenResponse {

    public String getAccessToken();

    public String getRefreshToken();

    public Long getExpiresInSeconds();
}
