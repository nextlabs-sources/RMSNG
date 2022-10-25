/*
 * Created on Apr 25, 2005
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc.,
 * Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved
 * worldwide.
 */
package com.bluejungle.framework.utils;

import com.bluejungle.framework.patterns.EnumBase;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * A collection of various string utilities
 *
 * @author sasha
 * @author sergey
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/framework/src/java/main/com/bluejungle/framework/utils/StringUtils.java#1 $:
 */

public class StringUtils {

    private static final int[] CHARACTER_CLASS = new int[256];

    private static final int CHAR_ANY = 0x0001;
    private static final int CHAR_UPPER = 0x0002;
    private static final int CHAR_LOWER = 0x0004;
    private static final int CHAR_ALPHA = 0x0008;
    private static final int CHAR_DIGIT = 0x0010;
    private static final int CHAR_BLANK = 0x0020;
    private static final int CHAR_NOT_SEPARATOR = 0x0040;
    private static final int CHAR_NOT_UPPER = 0x0080;
    private static final int CHAR_NOT_LOWER = 0x0100;
    private static final int CHAR_NOT_ALPHA = 0x0200;
    private static final int CHAR_NOT_DIGIT = 0x0400;
    private static final int CHAR_NOT_BLANK = 0x0800;

    // We use this character in the string (not the pattern) to terminate the regular expression
    // matching (if we match up to this point then we match).  This is used by Policy Studio when
    // doing the resource preview.  Before we recurse into a directory we want to know if it is even
    // possible for it to match, so we terminate the resource string at the directory with '|'
    // This will work as long as a resource name never contains a vertical bar
    public static final char REGEXP_SEARCH_TERMINATOR = '|';

    static {
        for (int i = 0; i != 256; i++) {
            char ch = (char)i;
            CHARACTER_CLASS[i] = CHAR_ANY;
            if (Character.isUpperCase(ch)) {
                CHARACTER_CLASS[i] |= CHAR_UPPER;
            } else {
                CHARACTER_CLASS[i] |= CHAR_NOT_UPPER;
            }
            if (Character.isLowerCase(ch)) {
                CHARACTER_CLASS[i] |= CHAR_LOWER;
            } else {
                CHARACTER_CLASS[i] |= CHAR_NOT_LOWER;
            }
            if (Character.isLetter(ch)) {
                CHARACTER_CLASS[i] |= CHAR_ALPHA;
            } else {
                CHARACTER_CLASS[i] |= CHAR_NOT_ALPHA;
            }
            if (Character.isDigit(ch)) {
                CHARACTER_CLASS[i] |= CHAR_DIGIT;
            } else {
                CHARACTER_CLASS[i] |= CHAR_NOT_DIGIT;
            }
            if (Character.isWhitespace(ch)) {
                CHARACTER_CLASS[i] |= CHAR_BLANK;
            } else {
                CHARACTER_CLASS[i] |= CHAR_NOT_BLANK;
            }
            if (ch != '/') {
                CHARACTER_CLASS[i] |= CHAR_NOT_SEPARATOR;
            }
        }
    }

    /**
     * Escapes quotes and slashes in a <code>String</code> making it possible
     * to form a properly quoted value from the result.
     * @param s <code>String</code> to escape.
     * @return escaped <code>String</code>.
     */
    public static String escape(String s) {
        // Initially, the buffer is sized for the worst case.
        StringBuilder res = new StringBuilder(2 * s.length());
        for (int i = 0; i != s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '\n':
                    res.append("\\n");
                    break;
                case '\r':
                    res.append("\\r");
                    break;
                case '"':
                    res.append("\\\"");
                    break;
                case '\\':
                    res.append("\\\\");
                    break;
                default:
                    res.append(ch);
            }
        }
        return res.toString();
    }

    /**
     * Unescapes quotes and slashes in a <code>String</code>
     * making the string usable in an unparsed format.
     * @param s <code>String</code> to unescape.
     * @return unescaped <code>String</code>.
     */
    public static String unescape(String s) {
        StringBuilder res = new StringBuilder(s.length());
        boolean slash = false;
        for (int i = 0; i != s.length(); i++) {
            char ch = s.charAt(i);
            if (ch != '\\') {
                if (slash && (ch == 'n' || ch == 'r')) {
                    res.append(ch == 'n' ? '\n' : '\r');
                } else {
                    res.append(ch);
                }
                slash = false;
            } else {
                if (slash) {
                    res.append('\\');
                    slash = false;
                } else {
                    slash = true;
                }
            }
        }
        if (slash) {
            res.append('\\');
        }
        return res.toString();
    }

    private static class CaseSensitive extends EnumBase {

        public CaseSensitive(String name) {
            super(name);
        }

        boolean compare(char a, char b) {
            return false;
        }
    }

    public static final CaseSensitive CASE_SENSITIVE = new CaseSensitive("SENSITIVE") {

        boolean compare(char a, char b) {
            return a == b;
        }
    };

    public static final CaseSensitive CASE_INSENSITIVE = new CaseSensitive("INSENSITIVE") {

        // toLowerCase can be quite expensive.  Let's keep a table around so that the
        // common European languages will work faster
        private char[] lowerCaseMapping = new char[256];

        {
            for (int i = 0; i < 256; i++) {
                lowerCaseMapping[(char)i] = Character.toLowerCase((char)i);
            }
        }

        boolean compare(char a, char b) {
            if (a < 256 && b < 256) {
                return (lowerCaseMapping[a] == lowerCaseMapping[b]);
            } else {
                // Why do both toUpper and toLower?  Blame classical Georgian.  Classical
                // Georgian had an upper case, but modern usage frowns upon it.  toLower
                // will convert both upper and lower to lower case, but toUpper will leave
                // the characters alone.  Neato!
                return (Character.toUpperCase(a) == Character.toUpperCase(b) ||
                Character.toLowerCase(a) == Character.toLowerCase(b));
            }
        }
    };

    /**
     * Given a string and a wildcard pattern with ?, *, and **,
     * returns true when the string matches the pattern, and false
     * when the string does not match the pattern.  Matching is
     * case-sensitive.
     *
     * @param pattern the pattern with ?, *, and **
     * @param str the string to be matched.
     * @return true when the string matches the pattern, and false
     * when the string does not match the pattern.
     */

    public static boolean isMatch(String pattern, String str) {
        return isMatch(pattern, str, CASE_INSENSITIVE);
    }

    public static boolean isMatch(String pattern, String str, CaseSensitive comparisonType) {
        if (pattern == null) {
            throw new NullPointerException("pattern");
        }
        if (str == null) {
            throw new NullPointerException("string");
        }
        if (pattern.length() == 0) {
            return str.length() == 0;
        }
        int patternLength = pattern.length();
        int patternLast = patternLength - 1;
        int[] expected = new int[patternLength];
        boolean[] zeroOrMore = new boolean[patternLength];
        char[] normPat = new char[patternLength];

        int pp = 0; // pattern position
        for (int i = 0; i != patternLength; i++, pp++) {
            char ch = pattern.charAt(i);
            if ((ch == '?' || ch == '!') && i != patternLast) {
                char next = pattern.charAt(++i);
                switch (next) {
                    case 'U':
                    case 'u':
                        pp = addToPattern(
                                (ch == '?') ? CHAR_UPPER : CHAR_NOT_UPPER
                                , Character.isUpperCase(next)
                                , pp
                                , expected
                                , zeroOrMore);
                        break;
                    case 'L':
                    case 'l':
                        pp = addToPattern(
                                (ch == '?') ? CHAR_LOWER : CHAR_NOT_LOWER
                                , Character.isUpperCase(next)
                                , pp
                                , expected
                                , zeroOrMore);
                        break;
                    case 'S':
                    case 's':
                        pp = addToPattern(
                                (ch == '?') ? CHAR_BLANK : CHAR_NOT_BLANK
                                , Character.isUpperCase(next)
                                , pp
                                , expected
                                , zeroOrMore);
                        break;
                    case 'A':
                    case 'a':
                        pp = addToPattern(
                                (ch == '?') ? CHAR_ALPHA : CHAR_NOT_ALPHA
                                , Character.isUpperCase(next)
                                , pp
                                , expected
                                , zeroOrMore);
                        break;
                    case 'D':
                    case 'd':
                        pp = addToPattern(
                                (ch == '?') ? CHAR_DIGIT : CHAR_NOT_DIGIT
                                , Character.isUpperCase(next)
                                , pp
                                , expected
                                , zeroOrMore);
                        break;
                    case 'C':
                    case 'c':
                        pp = addToPattern(
                                (ch == '?') ? CHAR_NOT_SEPARATOR : CHAR_ANY
                                , Character.isUpperCase(next)
                                , pp
                                , expected
                                , zeroOrMore);
                        break;
                    default:
                        normPat[pp] = next;
                }
            } else if (ch == '*') {
                boolean multistar = false;
                while (i != patternLast && pattern.charAt(i + 1) == '*') {
                    i++;
                    multistar = true;
                }
                zeroOrMore[pp] = true;
                expected[pp] = multistar ? CHAR_ANY : CHAR_NOT_SEPARATOR;
            } else {
                normPat[pp] = ch;
            }
        }

        int normPatLength = pp;

        boolean[][] state = new boolean[2][normPatLength + 1];
        int pos = 1;
        state[0][0] = true;
        for (int i = 0; i != normPatLength && zeroOrMore[i]; i++) {
            state[0][pos++] = true;
        }
        char[] strCA = str.toCharArray();
        for (int i = 0; i != strCA.length; i++) {
            char c = strCA[i];
            int charClass = (c < 256) ? CHARACTER_CLASS[c] : CHAR_ANY;
            if (c != REGEXP_SEARCH_TERMINATOR) {
                // Process a regular character
                for (int j = 0; j < normPatLength; j++) {
                    if (state[0][j]) {
                        boolean match;
                        if (expected[j] != 0) {
                            match = (charClass & expected[j]) != 0;
                        } else {
                            match = comparisonType.compare(normPat[j], c);
                        }
                        if (match) {
                            pos = (zeroOrMore[j]) ? j : j + 1;
                            state[1][pos++] = true;

                            if (pos <= normPatLength) {
                                if (zeroOrMore[pos - 1]) {
                                    state[1][pos++] = true;
                                }
                            }
                        }
                    }
                }
            } else {
                boolean seenTrue = false;
                for (int j = normPatLength; j >= 0; j--) {
                    state[1][j] = seenTrue ? state[0][j] : true;
                    seenTrue |= state[0][j];
                }
            }
            boolean haveTrue = false;
            for (int j = 0; j != state[0].length; j++) {
                haveTrue |= (state[0][j] = state[1][j]);
                state[1][j] = false;
            }
            // If the entire state is false, we will not get a match
            if (!haveTrue) {
                return false;
            }
        }
        return state[0][normPatLength];
    }

    private static int addToPattern(int charClass, boolean plural, int pos, int[] expected, boolean[] zeroOrMore) {
        if (plural) {
            expected[pos++] = charClass;
            zeroOrMore[pos] = true;
        }
        expected[pos] = charClass;
        return pos;
    }

    /**
     * Counts the number of times <code>ch</code> occurs in <code>str</code>.
     * @param str the String the characters in which are to be counted.
     * @param ch the character to be counted.
     * @return the number of times <code>ch</code> occurs in <code>str</code>.
     */
    public static int count(String str, char ch) {
        int res = 0;
        for (int pos = str.indexOf(ch); pos != -1; pos = str.indexOf(ch, pos + 1), res++)
            ;
        return res;
    }

    /**
     * Mapping of the user input to the boolean values
     */
    private static final Map<String, Boolean> STRING_2_BOOLEAN = new HashMap<String, Boolean>();

    static {
        StringUtils.STRING_2_BOOLEAN.put("true", new Boolean(true));
        StringUtils.STRING_2_BOOLEAN.put("t", new Boolean(true));
        StringUtils.STRING_2_BOOLEAN.put("false", new Boolean(false));
        StringUtils.STRING_2_BOOLEAN.put("f", new Boolean(false));
        StringUtils.STRING_2_BOOLEAN.put("yes", new Boolean(true));
        StringUtils.STRING_2_BOOLEAN.put("y", new Boolean(true));
        StringUtils.STRING_2_BOOLEAN.put("no", new Boolean(false));
        StringUtils.STRING_2_BOOLEAN.put("n", new Boolean(false));
    }

    public static Boolean stringToBoolean(String str) {
        if (str == null) {
            return null;
        }
        return StringUtils.STRING_2_BOOLEAN.get(str.trim().toLowerCase());
    }

    public static boolean stringToBoolean(String str, boolean defaultValue) {
        if (str == null) {
            return defaultValue;
        }
        Boolean b = StringUtils.STRING_2_BOOLEAN.get(str.trim().toLowerCase());
        return b != null ? b : defaultValue;
    }

    public static String join(Iterable<String> strings, String separator) {
        if (strings == null) {
            return "";
        }

        if (separator == null) {
            separator = "";
        }

        StringBuilder buf = new StringBuilder();
        boolean firstString = true;

        for (String s : strings) {
            if (firstString) {
                firstString = false;
            } else {
                buf.append(separator);
            }
            buf.append(s);
        }

        return buf.toString();
    }

    // Arrays don't implement Iterable
    public static String join(String[] strings, String separator) {
        return StringUtils.join(Arrays.asList(strings), separator);
    }

    /**
     * Limit the length of the input string.
     * If the input string is longer than then the length, the ending will be replaced with "..."
     * If the input length is less than 3, no action will be done.
     * This method is designed for display a very long string. So three charactors doesn't make sense.
     *
     * @param input
     * @param length must be greater than 3
     * @return
     */
    public static String limitLength(String input, int maxLength) {
        if (maxLength <= 3) {
            throw new IllegalArgumentException("maxLength must be greater than 3");
        }

        if (input == null) {
            return null;
        }

        int length = input.length();
        if (length <= 3 || length <= maxLength) {
            return input;
        }

        input = input.substring(0, maxLength - 3) + "...";

        return input;
    }
}
