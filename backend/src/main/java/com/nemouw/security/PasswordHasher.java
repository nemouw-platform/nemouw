package com.nemouw.security;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;

import java.nio.charset.StandardCharsets;

import static com.nemouw.Constants.*;

public class PasswordHasher {
    private static final Argon2 ARGON2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id);

    /**
     * Hash of a throwaway string, used to burn the same CPU time when the user doesn't exist.
     */
    private static final String DUMMY_HASH = hashRaw("dummy-password-for-timing");

    private static String hashRaw(String password) {
        char[] pw = password.toCharArray();
        try {
            return ARGON2.hash(ARGON2_ITERATIONS, ARGON2_MEMORY_KB, ARGON2_PARALLELISM, pw, StandardCharsets.UTF_8);
        } finally {
            ARGON2.wipeArray(pw);
        }
    }

    /** Hashes the password exactly as given (no normalization). */
    public String hash(String password) {
        return hashRaw(password);
    }

    public boolean verify(String password, String hash) {
        if (password == null || hash == null) return false;
        // Over-long input can never match a stored hash (signup rejects it); bail out before spending memory/CPU on it.
        if (password.codePointCount(0, password.length()) > PASSWORD_MAX_LENGTH) return false;
        char[] pw = password.toCharArray();
        try {
            return ARGON2.verify(hash, pw, StandardCharsets.UTF_8);
        } finally {
            ARGON2.wipeArray(pw);
        }
    }

    /**
     * Call when the email isn't found, so response time doesn't reveal which accounts exist.
     */
    public void verifyDummy(String password) {
        verify(password, DUMMY_HASH);
    }
}