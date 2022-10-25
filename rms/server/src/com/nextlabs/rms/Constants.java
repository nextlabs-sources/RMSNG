package com.nextlabs.rms;

public final class Constants {

    public static final String API_INPUT = "API-input";

    public static final String FILE = "file";

    public static final String PARAM_EXPIRY = "expiry";

    public static final String PARAM_WATERMARK = "watermark";

    public static final String MEMBERSHIP_MODEL_SUFFIX = "_abac_membership";

    public static final long MAX_FILE_NAME_LENGTH = 128;

    public static final String ENCODE_CHAR = "UTF-8";

    private Constants() {
        throw new UnsupportedOperationException();
    }
}
