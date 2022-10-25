package com.nextlabs.rms.cache;

/**
 * @author RMS-DEV-TEAM@nextlabs.com
 * @version 1.0.0
 */

import com.nextlabs.common.util.Hex;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class UserAttributeCacheItem implements Serializable {

    private static final long serialVersionUID = 1845197891352327881L;
    private static final String ENCRYPT_KEY = "6p4oc2qEJiM2pBQZ";
    public static final String EMAIL = "email";
    public static final String DISPLAYNAME = "displayName";
    public static final String ADUSERNAME = "aduser";
    public static final String ADPASS = "adpass";
    public static final String ADDOMAIN = "addomain";
    public static final String UNIQUE_ID_ATTRIBUTE = "idp_unique_id";
    public static final String ATTRIBUTE_TYPE = "attrtype";
    public static final String ATTRIBUTE_INDEX = "attrindex";
    public static final int STRING_VALUE = 0;
    public static final int MULTI_VALUE = 1;
    public static final int NUMBER_VALUE = 2;
    private Map<String, List<String>> userAttributes;

    public Map<String, List<String>> getUserAttributes() {
        return userAttributes;
    }

    public void setUserAttributes(Map<String, List<String>> userAttributes) {
        this.userAttributes = userAttributes;
    }

    public static String getKey(int userId, String clientId) {
        return new StringBuilder(40).append(userId).append('@').append(clientId).toString();
    }

    public static String encrypt(String value) throws GeneralSecurityException {
        byte[] raw = ENCRYPT_KEY.getBytes(Charset.forName("UTF-8"));
        Key skeySpec = new SecretKeySpec(raw, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", "BCFIPS");
        byte[] iv = new byte[cipher.getBlockSize()];
        IvParameterSpec ivParams = new IvParameterSpec(iv);
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, ivParams);
        return Hex.toHexString(cipher.doFinal(value.getBytes(Charset.forName("UTF-8"))));
    }

    public static String decrypt(String encrypted) throws GeneralSecurityException {
        byte[] raw = ENCRYPT_KEY.getBytes(Charset.forName("UTF-8"));
        Key key = new SecretKeySpec(raw, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", "BCFIPS");
        byte[] ivByte = new byte[cipher.getBlockSize()];
        IvParameterSpec ivParamsSpec = new IvParameterSpec(ivByte);
        cipher.init(Cipher.DECRYPT_MODE, key, ivParamsSpec);
        return new String(cipher.doFinal(Hex.toByteArray(encrypted)), StandardCharsets.UTF_8);
    }
}
