package com.nextlabs.nxl;

import com.nextlabs.common.util.StringUtils;

import java.nio.ByteBuffer;

class Signature {

    private static final long MAGIC_CODE = 18107089378105422L;

    private long magicCode;
    private short major;
    private short minor;
    private byte[] message;

    public Signature() {
        magicCode = MAGIC_CODE;
        major = 3;
        message = new byte[244];
    }

    public Signature(ByteBuffer buf) {
        magicCode = buf.getLong();
        minor = buf.getShort();
        major = buf.getShort();
        message = new byte[244];
        buf.get(message);
    }

    public boolean isValid() {
        return magicCode == MAGIC_CODE;
    }

    public void writeTo(ByteBuffer bb) {
        bb.putLong(magicCode);
        bb.putShort(minor);
        bb.putShort(major);
        bb.put(message);
    }

    public void setMajor(short major) {
        this.major = major;
    }

    public short getMajor() {
        return major;
    }

    public void setMinor(short minor) {
        this.minor = minor;
    }

    public short getMinor() {
        return minor;
    }

    public void setMessage(String text) {
        byte[] buf = StringUtils.toBytesQuietly(text);
        if (buf.length >= message.length) {
            throw new IllegalArgumentException("message length should be less then 244");
        }
        System.arraycopy(buf, 0, message, 0, buf.length);
    }

    public String getMessage() {
        return NxlUtils.readNullTerminatedString(message);
    }
}
