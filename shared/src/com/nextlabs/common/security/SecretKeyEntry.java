package com.nextlabs.common.security;

import javax.crypto.SecretKey;

public class SecretKeyEntry extends Entry {

    private SecretKey key;

    public SecretKey getKey() {
        return key;
    }

    @Override
    public String getType() {
        return SecretKeyEntry.class.getSimpleName();
    }

    public void setKey(SecretKey key) {
        this.key = key;
    }
}
