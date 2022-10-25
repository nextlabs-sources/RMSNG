package com.nextlabs.common.security;

import com.bluejungle.framework.crypt.IDecryptor;
import com.bluejungle.framework.crypt.IEncryptor;
import com.bluejungle.framework.crypt.ReversibleEncryptor;

public final class PropertyEncryptDecryptUtil {

    public static String encrypt(String secret) {
        IEncryptor encryptor = new ReversibleEncryptor();
        return encryptor.encrypt(secret);
    }

    public static String decrypt(String encryptedSecret) {

        IDecryptor decryptor = new ReversibleEncryptor();
        return decryptor.decrypt(encryptedSecret);

    }

    private PropertyEncryptDecryptUtil() {
    }
}
