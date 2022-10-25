package com.nextlabs.nxl;

import com.nextlabs.common.io.ProxyOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.GeneralSecurityException;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

class EncryptOutputStream extends ProxyOutputStream {

    private int iv0;
    private int iv1;
    private int iv2;
    private int iv3;
    private ByteBuffer bb;
    private byte[] block;
    private int pos;
    private long ivOffset;
    private SecretKeySpec secretKey;
    private Cipher cipher;
    private long length;
    private boolean closed;

    public EncryptOutputStream(OutputStream os, byte[] cek, byte[] iv) throws GeneralSecurityException {
        super(os);
        block = new byte[NxlUtils.BLOCK_SIZE];
        secretKey = new SecretKeySpec(cek, NxlUtils.ALGORITHM_AES);
        cipher = Cipher.getInstance(NxlUtils.CIPHER_AES, "BCFIPS");

        bb = ByteBuffer.allocate(16);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.put(iv);
        iv0 = bb.getInt(0);
        iv1 = bb.getInt(4);
        iv2 = bb.getInt(8);
        iv3 = bb.getInt(12);
    }

    @Override
    public void write(int i) throws IOException {
        block[pos++] = (byte)i;
        if (pos == NxlUtils.BLOCK_SIZE) {
            encrypt(block, 0, NxlUtils.BLOCK_SIZE);
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(byte[] buf, int off, int len) throws IOException {
        length += len;
        int padding = NxlUtils.BLOCK_SIZE - pos;
        if (len < padding) {
            System.arraycopy(buf, off, block, pos, len);
            pos += len;
            return;
        }

        System.arraycopy(buf, off, block, pos, padding);
        encrypt(block, 0, NxlUtils.BLOCK_SIZE);

        off += padding;
        len -= padding;

        while (len >= NxlUtils.BLOCK_SIZE) {
            encrypt(buf, off, NxlUtils.BLOCK_SIZE);
            off += NxlUtils.BLOCK_SIZE;
            len -= NxlUtils.BLOCK_SIZE;
        }

        System.arraycopy(buf, off, block, 0, len);
        pos = len;
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        if (pos > 0) {
            int mod = pos % 16;
            int len;
            if (mod > 0) {
                len = pos + 16 - mod;
            } else {
                len = pos;
            }
            encrypt(block, 0, len);
        }
        int mod = (int)(length % (NxlUtils.BLOCK_SIZE));
        if (mod > 0) {
            int mod16 = (int)(length % 16);
            int padding = NxlUtils.BLOCK_SIZE - mod;
            if (mod16 > 0) {
                padding -= 16 - mod16;
            }
            if (padding > 0) {
                byte[] buf = new byte[padding];
                os.write(buf);
            }
        }
    }

    private void encrypt(byte[] buf, int offset, int len) throws IOException {
        try {
            if (ivOffset > 0) {
                bb.putLong(8, (ivOffset - 1) * 31);
                int offsetLow = bb.getInt(8);
                int offsetHi = bb.getInt(12);
                bb.putInt(0, iv0 ^ offsetLow);
                bb.putInt(4, iv1 ^ offsetHi);
                bb.putInt(8, iv2 ^ offsetLow);
                bb.putInt(12, iv3 ^ offsetHi);
            }

            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(bb.array()));
            cipher.doFinal(buf, offset, len, buf, offset);
            ivOffset += NxlUtils.BLOCK_SIZE;

            os.write(buf, offset, len);
            pos = 0;
        } catch (GeneralSecurityException e) {
            throw new IOException(e);
        }
    }
}
