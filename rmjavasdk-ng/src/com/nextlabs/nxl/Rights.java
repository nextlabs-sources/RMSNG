package com.nextlabs.nxl;

import com.nextlabs.common.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public enum Rights {
    VIEW(1),
    EDIT(1 << 1),
    PRINT(1 << 2),
    CLIPBOARD(1 << 3),
    SAVEAS(1 << 4),
    DECRYPT(1 << 5),
    SCREENCAP(1 << 6),
    SEND(1 << 7),
    CLASSIFY(1 << 8),
    SHARE(1 << 9),
    DOWNLOAD(1 << 10),
    ACCESS_PROJECT(1 << 11),
    WATERMARK(1 << 30);

    private int value;
    private static Set<Rights> allRights;
    public static final String ACTION_PREFIX = "RIGHT_";

    private static final Logger LOGGER = LogManager.getLogger(Rights.class);

    static {
        allRights = EnumSet.allOf(Rights.class);
        allRights = Collections.unmodifiableSet(allRights);
    }

    Rights(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static Rights[] fromInt(int desiredRights) {
        Rights[] values = values();
        List<Rights> list = new ArrayList<Rights>(values.length);
        for (Rights rights : values) {
            if ((desiredRights & rights.getValue()) == rights.getValue()) {
                list.add(rights);
            }
        }
        return list.toArray(new Rights[list.size()]);
    }

    public static Rights[] fromStrings(String[] rights) {
        Rights[] ret = new Rights[rights.length];
        for (int i = 0; i < rights.length; ++i) {
            ret[i] = Rights.valueOf(rights[i]);
        }
        return ret;
    }

    public static Rights[] fromStrings(Collection<String> rights) {
        List<Rights> ret = new ArrayList<>();
        for (String right : rights) {
            try {
                if (right.startsWith(ACTION_PREFIX)) {
                    Rights rightEnum = Rights.valueOf(StringUtils.substringAfter(right, ACTION_PREFIX));
                    ret.add(rightEnum);
                } else {
                    LOGGER.debug("{} is not a valid right in RMS", right);
                }
            } catch (IllegalArgumentException e) {
                LOGGER.debug("{} is not a valid right in RMS", right);
            }
        }
        return ret.toArray(new Rights[ret.size()]);
    }

    public static String[] toStrings(Rights... rights) {
        String[] values = new String[rights.length];
        for (int i = 0; i < rights.length; ++i) {
            values[i] = rights[i].toString();
        }
        return values;
    }

    public static int toInt(Rights... rights) {
        int value = 0;
        if (rights == null || rights.length == 0) {
            return value;
        }
        for (Rights v : rights) {
            value |= v.getValue();
        }
        return value;
    }

    public static Rights[] all() {
        return allRights.toArray(new Rights[allRights.size()]);
    }

    public static String toRightAction(String right) {
        return ACTION_PREFIX + right;
    }
}
