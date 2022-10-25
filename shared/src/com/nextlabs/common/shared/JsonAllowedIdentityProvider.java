package com.nextlabs.common.shared;

public class JsonAllowedIdentityProvider {

    private int type;
    private Integer number;

    public JsonAllowedIdentityProvider() {
    }

    public JsonAllowedIdentityProvider(int type) {
        this.type = type;
    }

    public JsonAllowedIdentityProvider(int type, Integer number) {
        this.type = type;
        this.number = number;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }
}
