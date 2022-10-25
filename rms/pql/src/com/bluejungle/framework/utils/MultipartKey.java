package com.bluejungle.framework.utils;

/*
 * Created on Apr 22, 2005
 *
 * All sources, binaries and HTML pages (C) copyright 2007 by NextLabs Inc.,
 * San Mateo CA, Ownership remains with NextLabs Inc, All rights reserved worldwide.
 */

import java.io.Serializable;

/**
 * @author sasha
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/framework/src/java/main/com/bluejungle/framework/utils/MultipartKey.java#1 $:
 */

public class MultipartKey implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Object[] parts;
    private int hashCode = -1;
    private boolean isCacheable = true;

    public MultipartKey(Object... parts) {
        if (parts == null) {
            throw new NullPointerException("parts");
        }
        if (parts.length == 0) {
            throw new IllegalArgumentException("parts must not be empty.");
        }
        this.parts = new Object[parts.length];
        for (int i = 0; i != parts.length; i++) {
            if (parts[i] == null) {
                throw new NullPointerException("parts[" + i + "]");
            }
            this.parts[i] = parts[i];
        }
    }

    public final int hashCode() {
        if (hashCode < 0) {
            int hc = 0;
            for (int i = parts.length - 1; i >= 0; i--) {
                hc += parts[i].hashCode();
            }
            hashCode = hc;
        }
        return hashCode;
    }

    public boolean equals(Object other) {
        if (other == null || !(other instanceof MultipartKey)) {
            return false;
        }
        MultipartKey mp = (MultipartKey)other;
        if (mp.parts.length != parts.length) {
            return false;
        }
        for (int i = 0; i != parts.length; i++) {
            if (!parts[i].equals(mp.parts[i])) {
                return false;
            }
        }
        return true;
    }

    public String toString() {
        StringBuffer res = new StringBuffer();
        res.append("{");
        for (int i = 0; i != parts.length; i++) {
            if (i != 0) {
                res.append(", ");
            }
            res.append(parts[i]);
        }
        res.append("}");
        return res.toString();
    }

    public boolean isCacheable() {
        return isCacheable;
    }

    public void setCacheable(boolean isCacheable) {
        this.isCacheable = isCacheable;
    }

}
