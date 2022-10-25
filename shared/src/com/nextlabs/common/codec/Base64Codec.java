package com.nextlabs.common.codec;

import org.apache.commons.codec.binary.Base64;

public final class Base64Codec {

    public static byte[] decode(byte[] src) {
        return Base64.decodeBase64(src);
    }

    public static byte[] decode(String base64String) {
        return Base64.decodeBase64(base64String);
    }

    public static byte[] encode(byte[] src) {
        return Base64.encodeBase64(src);
    }

    public static byte[] encodeChunked(byte[] src) {
        return Base64.encodeBase64Chunked(src);
    }

    public static String encodeAsString(byte[] src) {
        return Base64.encodeBase64String(src);
    }

    public static byte[] encodeUrlSafe(byte[] src) {
        return Base64.encodeBase64URLSafe(src);
    }

    private Base64Codec() {
    }
}
