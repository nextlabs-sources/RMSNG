package com.nextlabs.rms.idp;

public class AzureAdIdpAttributes {

    public static final String AUTH_ENDPOINT = "https://login.microsoftonline.com/";
    public static final String GRAPH_ENDPOINT = "https://graph.microsoft.com/";

    private String directoryId;
    private String appId;
    private String appSecret;
    private boolean enableApproval;
    private String signupUrl;
    private String evalUserIdAttribute;

    public String getEvalUserIdAttribute() {
        return evalUserIdAttribute;
    }

    public void setEvalUserIdAttribute(String evalUserIdAttribute) {
        this.evalUserIdAttribute = evalUserIdAttribute;
    }

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

    public String getDirectoryId() {
        return directoryId;
    }

    public void setDirectoryId(String directoryId) {
        this.directoryId = directoryId;
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
