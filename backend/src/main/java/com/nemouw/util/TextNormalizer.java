package com.nemouw.util;

import java.text.Normalizer;
import java.util.Locale;

public class TextNormalizer {
    private TextNormalizer() {}

    /** NFC: used only to validate passwords (blocklist, context words). Never for hashing. */
    public static String nfc(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFC);
    }

    /** NFKC: for other normalizations (never for passwords). */
    public static String nfkc(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFKC);
    }


    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
