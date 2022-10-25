package com.nextlabs.common.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class ByteUtils {

    public static final byte LF = 0x0A;
    public static final byte CR = 0x0D;

    private static byte int0(int x) {
        return (byte)(x);
    }

    private static byte int1(int x) {
        return (byte)(x >> 8);
    }

    private static byte int2(int x) {
        return (byte)(x >> 16);
    }

    private static byte int3(int x) {
        return (byte)(x >> 24);
    }

    public static int readInt(byte[] bytes, int offset) {
        return readInt(bytes, offset, ByteOrder.BIG_ENDIAN);
    }

    public static int readInt(byte[] bytes, int offset, ByteOrder order) {
        return ByteBuffer.wrap(bytes, offset, 4).order(order).getInt();
    }

    public static long readLong(byte[] bytes, int offset) {
        return readLong(bytes, offset, ByteOrder.BIG_ENDIAN);
    }

    public static long readLong(byte[] bytes, int offset, ByteOrder order) {
        return ByteBuffer.wrap(bytes, offset, 8).order(order).getLong();
    }

    public static void writeInt(byte[] bytes, int value) {
        writeInt(bytes, value, 0, ByteOrder.BIG_ENDIAN);
    }

    public static void writeInt(byte[] bytes, int value, int offset, ByteOrder order) {
        boolean bigEndian = order == ByteOrder.BIG_ENDIAN;
        if (bigEndian) {
            bytes[offset] = int3(value);
            bytes[offset + 1] = int2(value);
            bytes[offset + 2] = int1(value);
            bytes[offset + 3] = int0(value);
        } else {
            bytes[offset] = int0(value);
            bytes[offset + 1] = int1(value);
            bytes[offset + 2] = int2(value);
            bytes[offset + 3] = int3(value);
        }
    }

    private ByteUtils() {
    }
}
