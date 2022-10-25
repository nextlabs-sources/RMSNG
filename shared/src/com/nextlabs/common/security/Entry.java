package com.nextlabs.common.security;

public abstract class Entry {

    private String alias;
    private String tokenGroupName;

    public String getAlias() {
        return alias;
    }

    public String getTokenGroupName() {
        return tokenGroupName;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public void setTokenGroupName(String tenantName) {
        this.tokenGroupName = tenantName;
    }

    public abstract String getType();
}
