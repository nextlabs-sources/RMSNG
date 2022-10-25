/**
 *
 */
package com.nextlabs.common.util;

import org.apache.commons.lang3.text.translate.UnicodeEscaper;
import org.apache.commons.lang3.text.translate.UnicodeUnescaper;

/**
 * @author nnallagatla
 *
 */
public final class EscapeUtils {

    private static final UnicodeEscaper NON_ASCII_ESCAPER = UnicodeEscaper.above(127);
    private static final UnicodeUnescaper NON_ASCII_UNESCAPER = new UnicodeUnescaper();

    public static String escapeNonASCIICharacters(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return NON_ASCII_ESCAPER.translate(str);
    }

    public static String unescapeNonASCIICharacters(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return NON_ASCII_UNESCAPER.translate(str);
    }

    private EscapeUtils() {
    }
}
