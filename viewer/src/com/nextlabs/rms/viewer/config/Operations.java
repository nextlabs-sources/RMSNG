package com.nextlabs.rms.viewer.config;

public enum Operations {
    VIEW_FILE_INFO(1),
    PRINT(1 << 1),
    PROTECT(1 << 2),
    SHARE(1 << 3),
    DOWNLOAD(1 << 4),
    DECRYPT(1 << 5);

    public static final int ALL_OPERATIONS;
    private final int value;
    static {
        Operations[] values = values();
        ALL_OPERATIONS = toInt(values);
    }

    private Operations(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static int toInt(Operations... ops) {
        int value = 0;
        for (Operations o : ops) {
            value |= o.getValue();
        }
        return value;
    }

    public static boolean isFlagSet(int flags, int mask) {
        return (flags & mask) == mask;
    }
}
