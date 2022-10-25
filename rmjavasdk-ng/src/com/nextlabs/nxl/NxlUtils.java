package com.nextlabs.nxl;

import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.GeneralSecurityException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

final class NxlUtils {

    public static final String ALG_HMAC_SHA256 = "HmacSHA256";

    public static final int NXL_SECTION_FLAG_ENCRYPTED = 0x00000001;
    public static final int NXL_SECTION_FLAG_COMPRESSED = 0x00000002;

    public static final String ALGORITHM_AES = "AES";
    public static final String CIPHER_AES = "AES/CBC/NoPadding";
    public static final int BLOCK_SIZE = 512;

    private NxlUtils() {
    }

    public static byte[] encrypt(byte[] key, byte[] iv, byte[] buf) throws GeneralSecurityException {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            EncryptOutputStream eos = new EncryptOutputStream(bos, key, iv);
            eos.write(buf);
            eos.close();
            bos.close();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new GeneralSecurityException(e);
        }
    }

    public static byte[] encryptOneBlock(byte[] key, byte[] iv, byte[] buf) throws GeneralSecurityException {
        return cryptOneBlock(Cipher.ENCRYPT_MODE, key, iv, buf);
    }

    public static byte[] decrypt(byte[] key, byte[] iv, byte[] buf, int length) throws GeneralSecurityException {
        ByteArrayInputStream bis = null;
        DecryptInputStream dis = null;
        try {
            bis = new ByteArrayInputStream(buf);
            dis = new DecryptInputStream(bis, key, iv, length);
            return IOUtils.readFully(dis);
        } catch (IOException e) {
            throw new GeneralSecurityException(e);
        } finally {
            IOUtils.closeQuietly(dis);
            IOUtils.closeQuietly(bis);
        }
    }

    public static byte[] decryptOneBlock(byte[] key, byte[] iv, byte[] buf) throws GeneralSecurityException {
        return cryptOneBlock(Cipher.DECRYPT_MODE, key, iv, buf);
    }

    private static byte[] cryptOneBlock(int mode, byte[] key, byte[] iv, byte[] src) throws GeneralSecurityException {
        SecretKeySpec secretKey = new SecretKeySpec(key, ALGORITHM_AES);
        Cipher cipher = Cipher.getInstance(CIPHER_AES, "BCFIPS");
        cipher.init(mode, secretKey, new IvParameterSpec(iv));
        return cipher.doFinal(src);
    }

    public static byte[] getChecksum(byte[] token, byte[] buf) throws GeneralSecurityException {
        return getChecksum(token, buf, 0, buf.length);
    }

    public static byte[] getChecksum(byte[] token, byte[] buf, int offset, int length) throws GeneralSecurityException {
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(length);

        SecretKeySpec secretKey = new SecretKeySpec(token, ALG_HMAC_SHA256);
        Mac mac = Mac.getInstance(ALG_HMAC_SHA256, "BCFIPS");
        mac.init(secretKey);
        mac.update(bb.array());
        mac.update(buf, offset, length);
        return mac.doFinal();
    }

    public static byte[] decompress(byte[] buf, int offset, int length) throws IOException {
        InflaterInputStream iis = new InflaterInputStream(new ByteArrayInputStream(buf, offset, length), new Inflater(true));
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        IOUtils.copy(iis, bos);
        iis.close();
        bos.close();
        return bos.toByteArray();
    }

    public static byte[] compress(byte[] buf, int offset, int length) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DeflaterOutputStream dos = new DeflaterOutputStream(bos, new Deflater(Deflater.DEFAULT_COMPRESSION, true));
        dos.write(buf, offset, length);
        dos.close();
        bos.close();
        return bos.toByteArray();
    }

    public static boolean isFlagSet(int flags, int mask) {
        return (flags & mask) == mask;
    }

    public static int setFlags(int flags, int mask, boolean on) {
        if (on) {
            return flags | mask;
        }
        return flags & ~mask;
    }

    public static String readNullTerminatedString(byte[] buf) {
        return readNullTerminatedString(buf, 0, buf.length, "UTF-8");
    }

    public static String readNullTerminatedString(byte[] buf, int offset, int len, String charset) {
        for (int i = offset + len - 1; i >= offset; --i) {
            if (buf[i] != 0) {
                return StringUtils.toStringQuietly(buf, offset, i + 1, charset);
            }
        }
        return "";
    }

    public static long getEncBlockStart(long start) {
        return (long)BLOCK_SIZE * (start / BLOCK_SIZE);
    }
}
