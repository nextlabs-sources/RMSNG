package com.nextlabs.nxl;

import com.nextlabs.common.util.StringUtils;

import java.nio.ByteBuffer;

class FileHeader {

    public static final int NXL_FLAG_NONE = 0;

    public static final int ALGORITHM_AES_128 = 1;
    public static final int ALGORITHM_AES_256 = 2;

    public static final int CIPHER_BLOCK_SIZE = 512;

    private byte[] duid;
    private int flags;
    private int alignment;
    private int algorithm;
    private int cipherBlockSize;
    private int contentOffset;
    private byte[] owner;
    private int extendedDataOffset;

    public FileHeader() {
        alignment = 4096;
        algorithm = ALGORITHM_AES_256;
        cipherBlockSize = CIPHER_BLOCK_SIZE;
        owner = new byte[256];
    }

    public FileHeader(ByteBuffer bb) {
        duid = new byte[16];
        owner = new byte[256];
        bb.get(duid);
        flags = bb.getInt();
        alignment = bb.getInt();
        algorithm = bb.getInt();
        cipherBlockSize = bb.getInt();
        contentOffset = bb.getInt();
        bb.get(owner);
        extendedDataOffset = bb.getInt();
    }

    public void writeTo(ByteBuffer bb) {
        bb.put(duid);
        bb.putInt(flags);
        bb.putInt(alignment);
        bb.putInt(algorithm);
        bb.putInt(cipherBlockSize);
        bb.putInt(contentOffset);
        bb.put(owner);
        bb.putInt(extendedDataOffset);
    }

    public void setDuid(byte[] duid) {
        this.duid = duid;
    }

    public byte[] getDuid() {
        return duid;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    public int getFlags() {
        return flags;
    }

    public void setAlignment(int alignment) {
        this.alignment = alignment;
    }

    public int getAlignment() {
        return alignment;
    }

    public void setAlgorithm(int algorithm) {
        this.algorithm = algorithm;
    }

    public int getAlgorithm() {
        return algorithm;
    }

    public void setCipherBlockSize(int cipherBlockSize) {
        this.cipherBlockSize = cipherBlockSize;
    }

    public int getCipherBlockSize() {
        return cipherBlockSize;
    }

    public void setContentOffset(int contentOffset) {
        this.contentOffset = contentOffset;
    }

    public int getContentOffset() {
        return contentOffset;
    }

    public void setOwner(String text) {
        byte[] buf = StringUtils.toBytesQuietly(text);
        if (buf.length >= owner.length) {
            throw new IllegalArgumentException("owner length should be less then 256");
        }
        System.arraycopy(buf, 0, owner, 0, buf.length);
    }

    public String getOwner() {
        return NxlUtils.readNullTerminatedString(owner);
    }

    public void setExtendedDataOffset(int extendedDataOffset) {
        this.extendedDataOffset = extendedDataOffset;
    }

    public int getExtendedDataOffset() {
        return extendedDataOffset;
    }
}
