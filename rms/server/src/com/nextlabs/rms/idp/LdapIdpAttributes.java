package com.nextlabs.rms.idp;

public class LdapIdpAttributes {

    private String name;
    private String ldapType;
    private String hostName;
    private String domain;
    private String searchBase;
    private String userSearchQuery;
    private String rmsGroup;
    private boolean ldapSSL;
    private String uniqueId;
    private String evalUserIdAttribute;
    private boolean securityPrincipalUseUserID;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLdapType() {
        return ldapType;
    }

    public void setLdapType(String ldapType) {
        this.ldapType = ldapType;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getSearchBase() {
        return searchBase;
    }

    public void setSearchBase(String searchBase) {
        this.searchBase = searchBase;
    }

    public String getUserSearchQuery() {
        return userSearchQuery;
    }

    public void setUserSearchQuery(String userSearchQuery) {
        this.userSearchQuery = userSearchQuery;
    }

    public String getRmsGroup() {
        return rmsGroup;
    }

    public void setRmsGroup(String rmsGroup) {
        this.rmsGroup = rmsGroup;
    }

    public boolean isLdapSSL() {
        return ldapSSL;
    }

    public void setLdapSSL(boolean ldapSSL) {
        this.ldapSSL = ldapSSL;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public String getEvalUserIdAttribute() {
        return evalUserIdAttribute;
    }

    public void setEvalUserIdAttribute(String evalUserIdAttribute) {
        this.evalUserIdAttribute = evalUserIdAttribute;
    }

    public boolean isSecurityPrincipalUseUserID() {
        return securityPrincipalUseUserID;
    }

    public void setSecurityPrincipalUseUserID(boolean securityPrincipalUseUserID) {
        this.securityPrincipalUseUserID = securityPrincipalUseUserID;
    }

}
