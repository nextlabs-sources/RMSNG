package com.nextlabs.rms.util;

import java.net.SocketException;
import java.net.SocketTimeoutException;

public final class Errors {

    private Errors() {
    }

    public static boolean isSocketException(Throwable t) {
        if (t == null) {
            return false;
        }
        if (t instanceof SocketTimeoutException || t instanceof SocketException) {
            return true;
        }
        return isSocketException(t.getCause());
    }
}
