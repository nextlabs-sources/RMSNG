package com.nextlabs.nxl;

import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.io.ProxyInputStream;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.GeneralSecurityException;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

class DecryptInputStream extends ProxyInputStream {

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
    private long total;

    public DecryptInputStream(InputStream is, byte[] cek, byte[] iv, long total) throws GeneralSecurityException {
        super(is instanceof BufferedInputStream ? (BufferedInputStream)is : new BufferedInputStream(is));
        this.total = total;
        pos = NxlUtils.BLOCK_SIZE;
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

    public void setIvOffset(long offset) {
        ivOffset = offset;
    }

    @Override
    public int read() throws IOException {
        if (pos < NxlUtils.BLOCK_SIZE) {
            if (++ivOffset > total) {
                return -1;
            }
            return block[pos++] & 0xff;
        }

        int read = in.read(block);
        if (read < 0) {
            return -1;
        }

        if (read < NxlUtils.BLOCK_SIZE) {
            int more = NxlUtils.BLOCK_SIZE - read;
            read += IOUtils.read(in, block, read, more);
            if (read == 0) {
                return -1;
            }
            decrypt(block, 0, read);
        } else {
            decrypt(block, 0, read);
        }
        ++ivOffset;
        return block[0] & 0xff;
    }

    private void decrypt(byte[] buf, int offset, int len) throws IOException {
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

            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(bb.array()));
            cipher.doFinal(buf, offset, len, buf, offset);
            pos = 1;
        } catch (GeneralSecurityException e) {
            throw new IOException(e);
        }
    }
}
