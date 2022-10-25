package com.nextlabs.rms.security;

import com.nextlabs.common.Environment;
import com.nextlabs.common.security.SimpleSSLSocketFactory;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

public class SSLSocketFactoryGenerator {

    public static final String KEYSTORE_FILE_SECURE = "agent-keystore.jks";
    public static final String KEYSTORE_FILE_PASS = "agent-key.dat";
    public static final String TRUSTSTORE_FILE_SECURE = "agent-truststore.jks";
    public static final String KEYSTORE_PASS_UNSECURE = "password";
    public static final String KEYSTORE_TYPE = "JKS";
    public static final String SSL_ALGORITHM = "SunX509";
    public static final String KEYSTORE_FILE_UNSECURE = "temp_agent-keystore.jks";
    public static final File CERT_PATH = new File(Environment.getInstance().getSharedConfDir(), "cert");

    private boolean isSecureMode;

    public SSLSocketFactoryGenerator(boolean mode) {
        isSecureMode = mode;
    }

    public SSLSocketFactory getSSLSocketFactory() throws IOException, GeneralSecurityException {

        KeyManager[] keyManagers = null;
        TrustManager[] trustManagers = null;

        if (!isSecureMode) {
            keyManagers = getKeyManagersUnsecure();
            trustManagers = getTrustManagersUnsecure();
        } else {
            keyManagers = getKeyManagersSecure();
            trustManagers = getTrustManagersSecure();
        }

        SSLContext context = SSLContext.getInstance("TLS");
        context.init(keyManagers, trustManagers, null);
        return context.getSocketFactory();
    }

    public KeyStore getTrustStoreSecure() {
        return CommArtifactsCache.getInstance().getTrustStoreSecure();
    }

    public KeyManager[] getKeyManagers() {
        if (!isSecureMode) {
            return getKeyManagersUnsecure();
        } else {
            return getKeyManagersSecure();
        }
    }

    public TrustManager[] getTrustManagers() {
        if (!isSecureMode) {
            return getTrustManagersUnsecure();
        } else {
            return getTrustManagersSecure();
        }
    }

    private KeyManager[] getKeyManagersUnsecure() {
        return CommArtifactsCache.getInstance().getKeyManagersUnsecure();
    }

    private KeyManager[] getKeyManagersSecure() {
        return CommArtifactsCache.getInstance().getKeyManagersSecure();

    }

    private TrustManager[] getTrustManagersUnsecure() {
        return new TrustManager[] { SimpleSSLSocketFactory.TRUST_ALL_MANAGER };
    }

    private TrustManager[] getTrustManagersSecure() {
        return CommArtifactsCache.getInstance().getTrustManagersSecure();
    }
}
