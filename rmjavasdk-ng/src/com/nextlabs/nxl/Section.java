package com.nextlabs.nxl;

import com.nextlabs.common.util.StringUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.Arrays;

public class Section {

    public static final int FLAG_ENCRYPTED = 1;
    public static final int FLAG_COMPRESSED = 2;

    public static final int DEFAULT_SECTION_SIZE = 4096;

    private byte[] name;
    private int flags;
    private int startOffset;
    private int sectionSize;
    private short originalDatasize;
    private short compressedDataSize;
    private byte[] checksum;
    private byte[] data;

    public Section(String name) {
        this.name = new byte[16];
        setName(name);
        sectionSize = DEFAULT_SECTION_SIZE;
    }

    public Section(ByteBuffer bb) {
        name = new byte[16];
        checksum = new byte[32];
        bb.get(name);
        flags = bb.getInt();
        startOffset = bb.getInt();
        sectionSize = bb.getInt();
        originalDatasize = bb.getShort();
        compressedDataSize = bb.getShort();
        bb.get(checksum);
    }

    public boolean isValid(byte[] token) throws GeneralSecurityException {
        return Arrays.equals(checksum, NxlUtils.getChecksum(token, data));
    }

    public void writeTo(ByteBuffer bb) {
        bb.put(name);
        bb.putInt(flags);
        bb.putInt(startOffset);
        bb.putInt(sectionSize);
        bb.putShort(originalDatasize);
        bb.putShort(compressedDataSize);
        bb.put(checksum);
    }

    public final void setName(String text) {
        byte[] buf = StringUtils.toBytesQuietly(text);
        if (buf.length >= name.length) {
            throw new IllegalArgumentException("Sction name length should be less then 244");
        }
        System.arraycopy(buf, 0, name, 0, buf.length);
    }

    public String getName() {
        return NxlUtils.readNullTerminatedString(name, 0, 16, "US-ASCII");
    }

    public void setEncrypted(boolean encrypted) {
        flags = NxlUtils.setFlags(flags, FLAG_ENCRYPTED, encrypted);
    }

    public boolean isEncrypted() {
        return NxlUtils.isFlagSet(flags, FLAG_ENCRYPTED);
    }

    public void setCompressed(boolean compressed) {
        flags = NxlUtils.setFlags(flags, FLAG_COMPRESSED, compressed);
    }

    public boolean isCompressed() {
        return NxlUtils.isFlagSet(flags, FLAG_COMPRESSED);
    }

    public void setStartOffset(int startOffset) {
        this.startOffset = startOffset;
    }

    public int getStartOffset() {
        return startOffset;
    }

    public void setSectionSize(int sectionSize) {
        this.sectionSize = sectionSize;
    }

    public int getSectionSize() {
        return sectionSize;
    }

    public void setOriginalDatasize(short originalDatasize) {
        this.originalDatasize = originalDatasize;
    }

    public short getOriginalDatasize() {
        return originalDatasize;
    }

    public void setCompressedDataSize(short compressedDataSize) {
        this.compressedDataSize = compressedDataSize;
    }

    public short getCompressedDataSize() {
        return compressedDataSize;
    }

    public void setChecksum(byte[] checksum) {
        this.checksum = checksum;
    }

    public byte[] getChecksum() {
        return checksum;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }

    public byte[] getDecodedData(byte[] token, byte[] iv) throws GeneralSecurityException, IOException, NxlException {
        byte[] buf;
        if (isEncrypted()) {
            if (!isValid(token)) {
                throw new NxlException("Section data is tempered.");
            }
            buf = NxlUtils.decrypt(token, iv, data, data.length);
        } else {
            buf = data;
        }

        if (isCompressed()) {
            buf = NxlUtils.decompress(buf, 0, compressedDataSize);
        }
        return buf;
    }
}
