package com.nextlabs.common.util;

import com.nextlabs.common.shared.RegularExpressions;

import java.util.Collection;
import java.util.regex.Matcher;

public final class EmailUtils {

    private EmailUtils() {
    }

    public static boolean validateEmail(String address) {
        Matcher m = RegularExpressions.EMAIL_PATTERN.matcher(address);
        return m.matches();
    }

    public static boolean validateEmails(Collection<String> addresses) {
        for (String address : addresses) {
            if (!validateEmail(address)) {
                return false;
            }
        }

        return true;
    }
}
