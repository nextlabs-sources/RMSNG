package com.nextlabs.rms.shared;

public final class Nvl {

    private Nvl() {
    }

    public static <T> T nvl(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }

    public static String nvl(String value) {
        return nvl(value, "");
    }

    public static Integer nvl(Integer value) {
        return nvl(value, 0);
    }

    public static Boolean nvl(Boolean value) {
        return nvl(value, Boolean.FALSE);
    }
}
