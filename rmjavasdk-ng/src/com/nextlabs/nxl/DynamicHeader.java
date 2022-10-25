package com.nextlabs.nxl;

import java.nio.ByteBuffer;

class DynamicHeader {

    private byte[] checksum;
    private long contentLength;

    public DynamicHeader() {
    }

    public DynamicHeader(ByteBuffer bb) {
        checksum = new byte[32];
        bb.get(checksum);
        contentLength = bb.getLong();
    }

    public void writeTo(ByteBuffer bb) {
        bb.put(checksum);
        bb.putLong(contentLength);
    }

    public void setChecksum(byte[] checksum) {
        this.checksum = checksum;
    }

    public byte[] getChecksum() {
        return checksum;
    }

    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }

    public long getContentLength() {
        return contentLength;
    }
}
