package com.nextlabs.common.util;

public final class Hex {

    private static final char[] LOWER_DIGITS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd',
        'e', 'f' };

    private static final char[] UPPER_DIGITS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D',
        'E', 'F' };

    private static String encodeHex(byte[] data, boolean upperCase) {
        char[] toDigits = upperCase ? UPPER_DIGITS : LOWER_DIGITS;
        int length = data.length;
        StringBuilder builder = new StringBuilder(length << 1);
        for (int i = 0; i < length; i++) {
            builder.append(toDigits[(0xF0 & data[i]) >>> 4]);
            builder.append(toDigits[0x0F & data[i]]);
        }
        return builder.toString();
    }

    public static byte[] toByteArray(String hex) {
        int length = hex.length();
        if (length % 2 != 0) {
            throw new NumberFormatException("Invalid Hex length");
        }
        byte[] result = new byte[length >> 1];
        for (int j = 0; j < length >> 1; j++) {
            result[j] = ((byte)Integer.parseInt(hex.substring(j << 1, (j << 1) + 2), 16));
        }
        return result;
    }

    public static String toHexString(byte[] data) {
        return toHexString(data, true);
    }

    public static String toHexString(byte[] data, boolean upperCase) {
        return encodeHex(data, upperCase);
    }

    private Hex() {
    }
}
