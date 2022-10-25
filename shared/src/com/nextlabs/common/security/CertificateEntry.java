package com.nextlabs.common.security;

import java.security.cert.Certificate;

public class CertificateEntry extends Entry {

    private Certificate certificate;

    public Certificate getCertificate() {
        return certificate;
    }

    @Override
    public String getType() {
        return CertificateEntry.class.getSimpleName();
    }

    public void setCertificate(Certificate certificate) {
        this.certificate = certificate;
    }
}
