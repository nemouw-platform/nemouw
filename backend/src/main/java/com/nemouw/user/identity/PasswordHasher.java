package com.nemouw.user.identity;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import static com.nemouw.user.AuthConstants.PASSWORD_MAX_LENGTH;

@Component
public class PasswordHasher {
    private final PasswordEncoder encoder;

    /** Hash of a throwaway string, used to burn the same CPU time when the user doesn't exist. */
    private final String dummyHash;

    PasswordHasher(PasswordEncoder encoder) {
        this.encoder = encoder;
        this.dummyHash = encoder.encode("dummy-password-for-timing");
    }

    /** Hashes the password exactly as given (no normalization). */
    public String hash(String password) {
        return encoder.encode(password);
    }

    public boolean verify(String password, String hash) {
        if (password == null || hash == null) return false;
        // Over-long input can never match a stored hash (signup rejects it); bail out before spending memory/CPU on it.
        if (password.codePointCount(0, password.length()) > PASSWORD_MAX_LENGTH) return false;
        return encoder.matches(password, hash);
    }

    /** Call when the email isn't found, so response time doesn't reveal which accounts exist. */
    public void verifyDummy(String password) {
        verify(password, dummyHash);
    }
}