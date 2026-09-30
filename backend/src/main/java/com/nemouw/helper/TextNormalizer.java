package com.nemouw.helper;

import java.text.Normalizer;

public class TextNormalizer {
    private TextNormalizer() {}

    /** The one canonical form used for validating AND hashing passwords. */
    public static String nfkc(String password) {
        return Normalizer.normalize(password, Normalizer.Form.NFKC);
    }
}
