package com.nemouw.security;

import at.favre.lib.crypto.bcrypt.BCrypt;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static com.nemouw.Constants.BCRYPT_COST;
import static com.nemouw.Constants.PASSWORD_MAX_BYTES;

public class PasswordHasher {
    /**
     * Hash of a throwaway string, used to burn the same CPU time when the user doesn't exist.
     */
    private static final String DUMMY_HASH = BCrypt.withDefaults().hashToString(BCRYPT_COST, "dummy-password-for-timing".toCharArray());

    public String hash(String password) {
        char[] pw = prepare(password);
        try {
            return BCrypt.withDefaults().hashToString(BCRYPT_COST, pw);
        } finally {
            Arrays.fill(pw, '\0');
        }
    }

    public boolean verify(String password, String hash) {
        if (password == null || hash == null) return false;
        char[] pw = prepare(password);
        try {
            // Over-long input can never match a stored hash (signup rejects it), and the bcrypt
            // library throws on it, which would turn a bad login into a 500.
            if (new String(pw).getBytes(StandardCharsets.UTF_8).length > PASSWORD_MAX_BYTES) return false;
            return BCrypt.verifyer().verify(pw, hash).verified;
        } finally {
            Arrays.fill(pw, '\0');
        }
    }

    /**
     * Call when the email isn't found, so response time doesn't reveal which accounts exist.
     */
    public void verifyDummy(String password) {
        verify(password, DUMMY_HASH);
    }

    /**
     * Must match exactly what PasswordPolicy validated.
     */
    private static char[] prepare(String password) {
        return TextNormalizer.nfkc(password).toCharArray();
    }
}