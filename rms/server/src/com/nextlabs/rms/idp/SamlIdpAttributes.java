package com.nextlabs.rms.idp;

public class SamlIdpAttributes {

    private String name;
    private String spEntityId;
    private String spAcsUrl;
    private String idpEntityId;
    private String idpSsoUrl;
    private String idpX509Cert;
    private String loginBtnText;

    private String spX509Cert;
    private String spPrivKey;

    private boolean debug;
    private boolean strict = true;

    private String signAlgo = "sha256";
    private String authNContext;
    private String spNameIdFormat = "unspecified";
    private String evalUserIdAttribute;

    private String buttonText;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSpEntityId() {
        return spEntityId;
    }

    public void setSpEntityId(String spEntityId) {
        this.spEntityId = spEntityId;
    }

    public String getSpAcsUrl() {
        return spAcsUrl;
    }

    public void setSpAcsUrl(String spAcsUrl) {
        this.spAcsUrl = spAcsUrl;
    }

    public String getIdpEntityId() {
        return idpEntityId;
    }

    public void setIdpEntityId(String idpEntityId) {
        this.idpEntityId = idpEntityId;
    }

    public String getIdpSsoUrl() {
        return idpSsoUrl;
    }

    public void setIdpSsoUrl(String idpSsoUrl) {
        this.idpSsoUrl = idpSsoUrl;
    }

    public String getIdpX509Cert() {
        return idpX509Cert;
    }

    public void setIdpX509Cert(String idpX509Cert) {
        this.idpX509Cert = idpX509Cert;
    }

    public String getLoginBtnText() {
        return loginBtnText;
    }

    public void setLoginBtnText(String loginBtnText) {
        this.loginBtnText = loginBtnText;
    }

    public String getSpX509Cert() {
        return spX509Cert;
    }

    public void setSpX509Cert(String spX509Cert) {
        this.spX509Cert = spX509Cert;
    }

    public String getSpPrivKey() {
        return spPrivKey;
    }

    public void setSpPrivKey(String spPrivKey) {
        this.spPrivKey = spPrivKey;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public boolean isStrict() {
        return strict;
    }

    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    public String getSignAlgo() {
        return signAlgo;
    }

    public void setSignAlgo(String signAlgo) {
        this.signAlgo = signAlgo;
    }

    public String getAuthNContext() {
        return authNContext;
    }

    public void setAuthNContext(String authNContext) {
        this.authNContext = authNContext;
    }

    public String getSpNameIdFormat() {
        return spNameIdFormat;
    }

    public void setSpNameIdFormat(String spNameIdFormat) {
        this.spNameIdFormat = spNameIdFormat;
    }

    public String getButtonText() {
        return buttonText;
    }

    public void setButtonText(String buttonText) {
        this.buttonText = buttonText;
    }

    public String getEvalUserIdAttribute() {
        return evalUserIdAttribute;
    }

    public void setEvalUserIdAttribute(String evalUserIdAttribute) {
        this.evalUserIdAttribute = evalUserIdAttribute;
    }
}
