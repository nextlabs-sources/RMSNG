package com.nextlabs.rms.idp;

public class GoogleIdpAttributes {

    private String appId;
    private String appSecret;
    private boolean enableApproval;
    private String signupUrl;

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    public boolean isEnableApproval() {
        return enableApproval;
    }

    public void setEnableApproval(boolean enableApproval) {
        this.enableApproval = enableApproval;
    }

    public String getSignupUrl() {
        return signupUrl;
    }

    public void setSignupUrl(String signupUrl) {
        this.signupUrl = signupUrl;
    }

}
