package com.nextlabs.common.util;

import com.nextlabs.nxl.exception.FIPSError;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;

public final class RandomUtils {

    private static final SecureRandom RANDOM = createDRBG();

    private static SecureRandom createDRBG() {
        try {
            return SecureRandom.getInstance("DEFAULT", "BCFIPS");
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new FIPSError("DRBG algorithm or provider not available", e);
        }
    }

    public static void nextBytes(byte[] data) {
        RANDOM.nextBytes(data);
    }

    public static byte[] nextBytes(int count) {
        return RANDOM.generateSeed(count);
    }

    private RandomUtils() {
    }
}
