package com.nextlabs.common.util;

import com.nextlabs.common.security.IKeyStoreManager;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class AuthUtils {

    public static final int HMAC_SHA256_LENGTH = 32;

    private AuthUtils() {
    }

    public static byte[] md5(byte[]... buf) throws NoSuchAlgorithmException, NoSuchProviderException {
        return hash(IKeyStoreManager.ALG_MD5, buf);
    }

    public static byte[] md5(String data) throws NoSuchAlgorithmException, NoSuchProviderException {
        return hash(IKeyStoreManager.ALG_MD5, StringUtils.toBytesQuietly(data));
    }

    public static byte[] hash(byte[]... buf) throws NoSuchAlgorithmException, NoSuchProviderException {
        return hash(IKeyStoreManager.ALG_HMAC_SHA256, buf);
    }

    public static byte[] hash(String algorithm, byte[]... buf)
            throws NoSuchAlgorithmException, NoSuchProviderException {
        MessageDigest md = MessageDigest.getInstance(algorithm, "BCFIPS");
        for (byte[] b : buf) {
            md.update(b);
        }
        return md.digest();
    }

    public static byte[] hmac(String userName, String passwd) throws GeneralSecurityException {
        try {
            byte[] buf = Hex.toByteArray(passwd);
            return hmac(IKeyStoreManager.ALG_HMAC_SHA256, StringUtils.toBytesQuietly(userName), buf, null);
        } catch (NumberFormatException e) {
            return new byte[0];
        }
    }

    public static byte[] hmac(String userName, byte[] passwd) throws GeneralSecurityException {
        return hmac(IKeyStoreManager.ALG_HMAC_SHA256, StringUtils.toBytesQuietly(userName), passwd, null);
    }

    public static byte[] hmac(byte[] message, byte[] key, byte[] salt) throws GeneralSecurityException {
        return hmac(IKeyStoreManager.ALG_HMAC_SHA256, message, key, salt);
    }

    public static byte[] hmac(String algorithm, byte[] message, byte[] key, byte[] salt)
            throws GeneralSecurityException {
        SecretKeySpec secretKey = new SecretKeySpec(key, algorithm);
        Mac mac = Mac.getInstance(algorithm, "BCFIPS");
        mac.init(secretKey);
        mac.update(message);
        if (salt != null) {
            mac.update(salt);
        }
        return mac.doFinal();
    }

    public static String normalize(String name) {
        return name.replaceAll("[^\\x00-\\x7F]", "");
    }
}
