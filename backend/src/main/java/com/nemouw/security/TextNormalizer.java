package com.nemouw.security;

import java.text.Normalizer;
import java.util.Locale;

public class TextNormalizer {
    private TextNormalizer() {}

    /** nfkc is used for validating AND hashing passwords. */
    public static String nfkc(String password) {
        return Normalizer.normalize(password, Normalizer.Form.NFKC);
    }

    public static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
