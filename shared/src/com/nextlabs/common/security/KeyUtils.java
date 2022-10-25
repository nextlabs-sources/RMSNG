package com.nextlabs.common.security;

import com.nextlabs.common.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Collection;

import javax.crypto.Cipher;

import org.apache.commons.codec.binary.Base64;

public final class KeyUtils {

    public static final java.lang.String BEGIN_CERT = "-----BEGIN CERTIFICATE-----";
    public static final java.lang.String END_CERT = "-----END CERTIFICATE-----";

    public static Certificate readCertificate(byte[] buf) throws CertificateException, NoSuchProviderException {
        CertificateFactory factory = CertificateFactory.getInstance("X.509", "BCFIPS");
        return factory.generateCertificate(new ByteArrayInputStream(buf));
    }

    public static Certificate[] readCertificateChain(String pem) throws CertificateException, NoSuchProviderException {
        Collection<? extends Certificate> certificates = readCertificates(StringUtils.toBytesQuietly(pem));
        return certificates.toArray(new Certificate[certificates.size()]);
    }

    public static Collection<? extends Certificate> readCertificates(byte[] buf)
            throws CertificateException, NoSuchProviderException {
        CertificateFactory factory = CertificateFactory.getInstance("X.509", "BCFIPS");
        return factory.generateCertificates(new ByteArrayInputStream(buf));
    }

    public static PublicKey readDHPublicKey(byte[] encodedKey)
            throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
        return readPublicKey(encodedKey, "DH");
    }

    public static PublicKey readPublicKey(byte[] encodedKey, String algorithm)
            throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encodedKey);
        KeyFactory keyFactory = KeyFactory.getInstance(algorithm, "BCFIPS");
        return keyFactory.generatePublic(keySpec);
    }

    public static String toPEM(Certificate... certs) throws CertificateEncodingException {
        StringBuilder builder = new StringBuilder(512);
        if (certs != null) {
            for (Certificate certificate : certs) {
                byte[] encoded = Base64.encodeBase64Chunked(certificate.getEncoded());
                builder.append(BEGIN_CERT).append('\n');
                builder.append(new String(encoded, StandardCharsets.UTF_8));
                builder.append(END_CERT).append('\n');
            }
        }
        return builder.toString();
    }

    public static byte[] encrypt(String algorithm, Key key, byte[] data) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(algorithm, "BCFIPS");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(data);
    }

    public static byte[] decrypt(String algorithm, Key key, byte[] data) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(algorithm, "BCFIPS");
        cipher.init(Cipher.DECRYPT_MODE, key);
        return cipher.doFinal(data);
    }

    private KeyUtils() {
    }
}
