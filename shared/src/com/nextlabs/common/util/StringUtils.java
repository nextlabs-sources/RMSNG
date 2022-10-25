package com.nextlabs.common.util;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

public final class StringUtils {

    private StringUtils() {

    }

    public static String normalize(String orig) {
        if (!hasText(orig)) {
            return orig;
        }
        int off = 0;
        int length = orig.length();
        char[] in = orig.toCharArray();
        char aChar;
        StringBuilder builder = new StringBuilder();
        while (off < length) {
            aChar = in[off++];
            if (aChar == '\\' && off < length) {
                aChar = in[off++];
                if (aChar == 'n') {
                    aChar = '\n';
                } else if (aChar == 't') {
                    aChar = '\t';
                } else if (aChar == 'r') {
                    aChar = '\r';
                } else if (aChar == 'f') {
                    aChar = '\f';
                }
            }
            builder.append(aChar);
        }
        return builder.toString();
    }

    /**
     * Check whether the given value has contains at least one non-whitespace character.
     *
     * @param str
     * @return
     */
    public static boolean hasText(CharSequence str) {
        if (!hasLength(str)) {
            return false;
        }
        int strLen = str.length();
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Compares two String, returning true if they represent equal sequences of characters, ignoring case.
     *
     * @param s1
     * @param s2
     * @return
     */
    public static boolean equalsIgnoreCase(String s1, String s2) {
        return nullSafeEquals(s1, s2, true);
    }

    /**
     * Compares two String, returning true if they represent equal sequences of characters, case sensitive.
     *
     * @param s1
     * @param s2
     * @return
     */
    public static boolean equals(String s1, String s2) {
        return nullSafeEquals(s1, s2, false);
    }

    private static boolean nullSafeEquals(String s1, String s2, boolean ignoreCase) {
        if (s1 == null || s2 == null) {
            return s1 == null && s2 == null;
        } else if (s1.equals(s2)) {
            return true;
        } else if (s1.length() != s2.length()) {
            return false;
        } else {
            return ignoreCase ? s1.equalsIgnoreCase(s2) : s1.equals(s2);
        }
    }

    /**
     * Check whether the given String ends with the specified suffix, ignoring upper/lower case.
     *
     * @param str
     * @param suffix
     * @return
     */
    public static boolean endsWithIgnoreCase(String str, String suffix) {
        return endsWith(str, suffix, true);
    }

    /**
     * Check whether the given String ends with the specified suffix, case sensitive.
     *
     * @param str
     * @param suffix
     * @return
     */
    public static boolean endsWith(String str, String suffix) {
        return endsWith(str, suffix, false);
    }

    private static boolean endsWith(String str, String suffix, boolean ignoreCase) {
        if (str == null || suffix == null) {
            return false;
        }
        if (str.endsWith(suffix)) {
            return true;
        }
        if (str.length() < suffix.length()) {
            return false;
        }
        String substring = str.substring(str.length() - suffix.length());
        return ignoreCase ? substring.equalsIgnoreCase(suffix) : substring.equals(suffix);
    }

    /**
     * Check whether the given String starts with the specified suffix, ignoring upper/lower case.
     *
     * @param str
     * @param prefix
     * @return
     */
    public static boolean startsWithIgnoreCase(String str, String prefix) {
        return startsWith(str, prefix, true);
    }

    /**
     * Check whether the given String starts with the specified prefix, case sensitive.
     *
     * @param str
     * @param prefix
     * @return
     */
    public static boolean startsWith(String str, String prefix) {
        return startsWith(str, prefix, false);
    }

    private static boolean startsWith(String str, String prefix, boolean ignoreCase) {
        if (str == null || prefix == null) {
            return false;
        }
        if (str.startsWith(prefix)) {
            return true;
        }
        if (str.length() < prefix.length()) {
            return false;
        }
        String substring = str.substring(0, prefix.length());
        return ignoreCase ? substring.equalsIgnoreCase(prefix) : substring.equals(prefix);
    }

    /**
     * Check whether the given array contains the given element.
     *
     * @param array
     * @param str
     * @param ignoreCase
     * @return
     */
    public static boolean containsElement(String[] array, String str, boolean ignoreCase) {
        if (array == null) {
            return false;
        }
        for (String s : array) {
            if (nullSafeEquals(s, str, ignoreCase)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check whether the given list contains the given element.
     *
     * @param list
     * @param str
     * @param ignoreCase
     * @return
     */
    public static boolean containsElement(List<String> list, String str, boolean ignoreCase) {
        if (list == null) {
            return false;
        }
        for (String s : list) {
            if (nullSafeEquals(s, str, ignoreCase)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check that the given CharSequence is neither null nor of length 0.
     *
     * @param str
     * @return
     */
    public static boolean hasLength(CharSequence str) {
        return str != null && str.length() > 0;
    }

    public static String trim(String str) {
        return str == null ? null : str.trim();
    }

    public static String trimToNull(String str) {
        String ts = trim(str);
        return !hasLength(ts) ? null : ts;
    }

    public static String trimToEmpty(String str) {
        return str == null ? "" : str.trim();
    }

    public static String join(Collection<String> items) {
        return join(items, ",");
    }

    public static String stripLeading(String str, String strip, boolean ignoreCase) {
        if (!hasLength(str)) {
            return str;
        }
        boolean startWith = startsWith(str, strip, ignoreCase);
        if (startWith) {
            return str.substring(strip.length());
        }
        return str;
    }

    public static String stripLeading(String str, String strip) {
        return stripLeading(str, strip, false);
    }

    public static String stripTrailing(String str, String strip, boolean ignoreCase) {
        if (!hasLength(str)) {
            return str;
        }
        boolean endsWith = endsWith(str, strip, ignoreCase);
        if (endsWith) {
            return str.substring(0, str.length() - strip.length());
        }
        return str;
    }

    public static String stripTrailing(String str, String strip) {
        return stripTrailing(str, strip, false);
    }

    public static String join(Collection<String> items, String separator) {
        if (items == null) {
            return null;
        }
        StringBuilder result = new StringBuilder();
        Iterator<String> it = items.iterator();
        int idx = 0;
        while (it.hasNext()) {
            if (idx > 0) {
                result.append(separator);
            }
            result.append(it.next());
            idx++;
        }
        return result.toString();
    }

    public static String toStringQuietly(byte[] data) {
        return toStringQuietly(data, "UTF-8");
    }

    public static String toStringQuietly(byte[] data, String encoding) {
        return toStringQuietly(data, 0, data.length, encoding);
    }

    public static String toStringQuietly(byte[] data, int offset, int length, String encoding) {
        try {
            return new String(data, offset, length, encoding);
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    public static String substringAfter(String data, String delimeter) {
        int i;
        if ((i = data.indexOf(delimeter)) >= 0) {
            return data.substring(i + delimeter.length());
        }
        return data;
    }

    public static String substringBefore(String data, String delimeter) {
        int i;
        if ((i = data.indexOf(delimeter)) >= 0) {
            return data.substring(0, i);
        }
        return data;
    }

    public static String substringAfterLast(String str, String separator) {
        if (!hasText(str)) {
            return str;
        }
        if (!hasText(separator)) {
            return "";
        }
        int pos = str.lastIndexOf(separator);
        if (pos == -1 || pos == (str.length() - separator.length())) {
            return "";
        }
        return str.substring(pos + separator.length());
    }

    public static byte[] toBytesQuietly(String data) {
        return toBytesQuietly(data, "UTF-8");
    }

    public static byte[] toBytesQuietly(String data, String encoding) {
        try {
            return data.getBytes(encoding);
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    public static boolean isAscii(String data) {
        CharsetEncoder asciiEncoder = Charset.forName("US-ASCII").newEncoder();
        return asciiEncoder.canEncode(data);
    }

    public static List<String> tokenize(String str, String delimiters) {
        return tokenize(str, delimiters, true, true);
    }

    public static List<String> tokenize(String str, String delimiters, boolean trimTokens, boolean ignoreEmptyTokens) {
        if (str == null) {
            return null;
        }
        StringTokenizer st = new StringTokenizer(str, delimiters);
        List<String> tokens = new ArrayList<String>();
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            if (trimTokens) {
                token = token.trim();
            }
            if (!ignoreEmptyTokens || token.length() > 0) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    public static String getParentPath(String filePath) {
        if (endsWith(filePath, "/")) {
            filePath = filePath.substring(0, filePath.lastIndexOf('/'));
        }
        return filePath == null ? null : filePath.substring(0, filePath.lastIndexOf('/') + 1);
    }

    public static String getMd5Hex(String plainText) {
        if (plainText == null) {
            return null;
        }
        StringBuilder hashtext;
        try {
            hashtext = new StringBuilder(Hex.toHexString(AuthUtils.md5(plainText)).toLowerCase());
            // Now we need to zero pad it if you actually want the full 32 chars.
            while (hashtext.length() < 32) {
                hashtext.insert(0, Integer.toString(0));
            }
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            return null;
        }
        return hashtext.toString();
    }
}
