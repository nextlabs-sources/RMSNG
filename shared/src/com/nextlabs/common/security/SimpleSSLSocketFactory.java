package com.nextlabs.common.security;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class SimpleSSLSocketFactory extends SSLSocketFactory {

    public static final TrustManager TRUST_ALL_MANAGER = new TrustAllManager();
    public static final HostnameVerifier ALLOW_ALL_HOSTNAME_VERIFIER = new NoopHostnameVerifier();
    public static final String TLS = "TLS";
    public static final String TLS_V1 = "TLSv1";
    public static final String TLS_V1_1 = "TLSv1.1";
    public static final String TLS_V1_2 = "TLSv1.2";
    private final SSLContext context;
    private final TrustManager[] trustManagers;
    private final KeyManager[] keyManagers;
    private final SecureRandom random;
    private SSLSocketFactory socketFactory;

    public SimpleSSLSocketFactory() throws GeneralSecurityException {
        this(TLS_V1, true);
    }

    public SimpleSSLSocketFactory(String protocol) throws GeneralSecurityException {
        this(protocol, true);
    }

    public SimpleSSLSocketFactory(String protocol, boolean trustAll) throws GeneralSecurityException {
        this(protocol, null, trustAll ? new TrustManager[] { TRUST_ALL_MANAGER } : null, null);
    }

    public SimpleSSLSocketFactory(String protocol, KeyManager[] keyManagers, TrustManager[] trustManagers,
        SecureRandom random) throws GeneralSecurityException {
        context = SSLContext.getInstance(protocol);
        this.keyManagers = keyManagers;
        this.trustManagers = trustManagers;
        this.random = random;
        createAdapteeFactory();
    }

    private synchronized void createAdapteeFactory() throws KeyManagementException {
        context.init(keyManagers, trustManagers, random);
        socketFactory = context.getSocketFactory();
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return socketFactory.createSocket(host, port);
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort)
            throws IOException {
        return socketFactory.createSocket(address, port, localAddress, localPort);
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        return socketFactory.createSocket(s, host, port, autoClose);
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        return socketFactory.createSocket(host, port);
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort)
            throws IOException {
        return socketFactory.createSocket(host, port, localHost, localPort);
    }

    public SSLContext getContext() {
        return context;
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return socketFactory.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return socketFactory.getSupportedCipherSuites();
    }

    static class NoopHostnameVerifier implements HostnameVerifier {

        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }

    static class TrustAllManager implements X509TrustManager {

        public void checkClientTrusted(X509Certificate[] cert, String authType) throws CertificateException {
        }

        public void checkServerTrusted(X509Certificate[] cert, String authType) throws CertificateException {
        }

        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }
}
