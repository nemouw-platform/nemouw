package com.nemouw.common.util;

import java.text.Normalizer;
import java.util.Locale;

public class TextNormalizer {
    private TextNormalizer() {
    }

    /**
     * NFC: used only to validate passwords (blocklist, context words). Never for hashing.
     */
    public static String nfc(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFC);
    }

    /**
     * NFKC: for other normalizations (never for passwords).
     */
    static String nfkc(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFKC);
    }


    public static String normalizeEmail(String email) {
        return strip(email).toLowerCase(Locale.ROOT);
    }

    public static String blankToNull(String value) {
        return isBlank(value) ? null : strip(value);
    }

    public static String emailLocalPart(String email) {
        if (email == null) return null;
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : null;
    }

    /**
     * True for null, empty, or text made only of whitespace, space separators (U+00A0, U+2003) and control characters.
     */
    public static boolean isBlank(String value) {
        if (value == null) return true;
        for (int i = 0; i < value.length(); i++) {
            if (!isInvisible(value.charAt(i))) return false;
        }
        return true;
    }

    /**
     * Like trim(), but also removes space separators and control characters at both ends. Null stays null.
     */
    public static String strip(String value) {
        if (value == null) return null;
        int start = 0;
        int end = value.length();
        while (start < end && isInvisible(value.charAt(start))) start++;
        while (end > start && isInvisible(value.charAt(end - 1))) end--;
        return value.substring(start, end);
    }

    private static boolean isInvisible(char c) {
        return Character.isWhitespace(c) || Character.isSpaceChar(c) || Character.isISOControl(c);
    }
}