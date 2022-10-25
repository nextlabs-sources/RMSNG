package com.nextlabs.rms.idp;

public class LocalIdpAttributes {

    private boolean enableApproval;
    private String signupUrl;

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
