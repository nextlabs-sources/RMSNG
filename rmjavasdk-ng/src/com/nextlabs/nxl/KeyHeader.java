package com.nextlabs.nxl;

import java.nio.ByteBuffer;

class KeyHeader {

    public static final int MODE_CLIENT_TOKEN = 1;
    public static final int MODE_SERVER_TOKEN = 2;
    public static final int MODE_SPLIT_TOKEN = 3;

    public static final int FLAG_RECOVERY_KEY = 1;

    private int flags;
    private byte[] iv;
    private byte[] ecek;
    private byte[] cekHmac;
    private byte[] recoveryKey;
    private byte[] recoveryKeyHmac;
    private byte[] rootAgreement;
    private byte[] icaAgreement;
    private int maintenanceLevel;
    private int extendedDataOffset;

    public KeyHeader() {
        flags = MODE_SERVER_TOKEN << 24;
    }

    public KeyHeader(ByteBuffer bb) {
        iv = new byte[16];
        ecek = new byte[32];
        cekHmac = new byte[32];
        recoveryKey = new byte[32];
        recoveryKeyHmac = new byte[32];
        rootAgreement = new byte[256];
        icaAgreement = new byte[256];
        flags = bb.getInt();
        bb.get(iv);
        bb.get(ecek);
        bb.get(cekHmac);
        bb.get(recoveryKey);
        bb.get(recoveryKeyHmac);
        bb.get(rootAgreement);
        bb.get(icaAgreement);
        maintenanceLevel = bb.getInt();
        extendedDataOffset = bb.getInt();
    }

    public void writeTo(ByteBuffer bb) {
        bb.putInt(flags);
        bb.put(iv);
        bb.put(ecek);
        bb.put(cekHmac);
        if (recoveryKey == null) {
            bb.position(bb.position() + 64);
        } else {
            bb.put(recoveryKey);
            bb.put(recoveryKeyHmac);
        }
        int padding = 256 - rootAgreement.length;
        if (padding > 0) {
            bb.position(bb.position() + padding);
        }
        if (rootAgreement.length > 256) {
            bb.put(rootAgreement, 1, 256);
        } else {
            bb.put(rootAgreement);
        }
        if (icaAgreement == null) {
            bb.position(bb.position() + 256);
        } else {
            padding = 256 - icaAgreement.length;
            if (padding > 0) {
                bb.position(bb.position() + padding);
            }
            if (icaAgreement.length > 256) {
                bb.put(icaAgreement, 1, 256);
            } else {
                bb.put(icaAgreement);
            }
        }
        bb.putInt(maintenanceLevel);
        bb.putInt(extendedDataOffset);
    }

    public int getProtectionMode() {
        return flags >>> 24;
    }

    public void setProtectionMode(int mode) {
        flags = mode << 24 | flags & 0xffffff;
    }

    public boolean isRecoveryKeyEnabled() {
        return NxlUtils.isFlagSet(flags, FLAG_RECOVERY_KEY);
    }

    public void setIv(byte[] iv) {
        this.iv = iv;
    }

    public byte[] getIv() {
        return iv;
    }

    public void setEcek(byte[] ecek) {
        this.ecek = ecek;
    }

    public byte[] getEcek() {
        return ecek;
    }

    public void setCekHmac(byte[] cekHmac) {
        this.cekHmac = cekHmac;
    }

    public byte[] getCekHmac() {
        return cekHmac;
    }

    public void setRecoveryKey(byte[] recoveryKey) {
        this.recoveryKey = recoveryKey;
    }

    public byte[] getRecoveryKey() {
        return recoveryKey;
    }

    public void setRecoveryKeyHmac(byte[] recoveryKeyHmac) {
        this.recoveryKeyHmac = recoveryKeyHmac;
    }

    public byte[] getRecoveryKeyHmac() {
        return recoveryKeyHmac;
    }

    public void setRootAgreement(byte[] rootAgreement) {
        this.rootAgreement = rootAgreement;
    }

    public byte[] getRootAgreement() {
        return rootAgreement;
    }

    public void setIcaAgreement(byte[] icaAgreement) {
        this.icaAgreement = icaAgreement;
    }

    public byte[] getIcaAgreement() {
        return icaAgreement;
    }

    public void setMaintenanceLevel(int maintenanceLevel) {
        this.maintenanceLevel = maintenanceLevel;
    }

    public int getMaintenanceLevel() {
        return maintenanceLevel;
    }

    public void setExtendedDataOffset(int extendedDataOffset) {
        this.extendedDataOffset = extendedDataOffset;
    }

    public int getExtendedDataOffset() {
        return extendedDataOffset;
    }
}
