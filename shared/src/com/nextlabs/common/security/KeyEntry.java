package com.nextlabs.common.security;

import java.security.Key;
import java.security.cert.Certificate;

public class KeyEntry extends Entry {

    private Key key;
    private Certificate[] certificates;

    public Certificate[] getCertificates() {
        return certificates;
    }

    public Key getKey() {
        return key;
    }

    @Override
    public String getType() {
        return KeyEntry.class.getSimpleName();
    }

    public void setCertificates(Certificate[] certificates) {
        this.certificates = certificates;
    }

    public void setKey(Key key) {
        this.key = key;
    }
}
