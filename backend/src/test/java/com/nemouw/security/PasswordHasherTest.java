package com.nemouw.security;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.nemouw.Constants.PASSWORD_MAX_LENGTH;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {

    private static final PasswordHasher HASHER = new PasswordHasher();
    private static final String PASSWORD = "vK9#mQ2$xL7!pR4@";

    private static String storedHash;

    @BeforeAll
    static void hashOnce() {
        storedHash = HASHER.hash(PASSWORD);
    }

    @Test
    void hashIsArgon2idAndDoesNotContainThePassword() {
        assertTrue(storedHash.startsWith("$argon2id$"));
        assertFalse(storedHash.contains(PASSWORD));
    }

    @Test
    void samePasswordGivesDifferentHashes() {
        assertNotEquals(storedHash, HASHER.hash(PASSWORD));
    }

    @Test
    void verifiesCorrectPassword() {
        assertTrue(HASHER.verify(PASSWORD, storedHash));
    }

    @Test
    void rejectsWrongPassword() {
        assertFalse(HASHER.verify(PASSWORD + "x", storedHash));
    }

    @Test
    void emptyPasswordDoesNotVerify() {
        assertFalse(HASHER.verify("", storedHash));
    }

    @Test
    void passwordsAreCaseSensitive() {
        assertFalse(HASHER.verify(PASSWORD.toLowerCase(), storedHash));
        assertFalse(HASHER.verify(PASSWORD.toUpperCase(), storedHash));
    }

    @Test
    void whitespaceIsPartOfThePassword() {
        assertFalse(HASHER.verify(" " + PASSWORD, storedHash));
        assertFalse(HASHER.verify(PASSWORD + " ", storedHash));
        assertFalse(HASHER.verify(PASSWORD.replace("#", "# "), storedHash));
    }

    @Test
    void nullInputReturnsFalse() {
        assertFalse(HASHER.verify(null, storedHash));
        assertFalse(HASHER.verify(PASSWORD, null));
    }

    @Test
    void passwordAtMaxLengthVerifies() {
        String password = "a".repeat(PASSWORD_MAX_LENGTH);
        assertTrue(HASHER.verify(password, HASHER.hash(password)));
    }

    @Test
    void overLongPasswordIsRejectedEvenWhenItMatchesTheHash() {
        String password = "a".repeat(PASSWORD_MAX_LENGTH + 1);
        assertFalse(HASHER.verify(password, HASHER.hash(password)));
    }

    @Test
    void maxLengthIsCountedInCodePoints() {
        String password = "\uD83D\uDE00".repeat(PASSWORD_MAX_LENGTH);
        assertTrue(HASHER.verify(password, HASHER.hash(password)));
    }

    @Test
    void unicodePasswordVerifies() {
        String password = "p\u00E4ssw\u00F6rd-\u0645\u0631\u062D\u0628\u0627-\u65E5\u672C\u8A9E";
        assertTrue(HASHER.verify(password, HASHER.hash(password)));
    }

    @Test
    void doesNotNormalizeUnicode() {
        String composed = "caf\u00E9-vK9#mQ2$";
        String decomposed = "cafe\u0301-vK9#mQ2$";
        assertFalse(HASHER.verify(decomposed, HASHER.hash(composed)));
    }

    @Test
    void verifyDummyDoesNotThrow() {
        assertDoesNotThrow(() -> HASHER.verifyDummy(PASSWORD));
        assertDoesNotThrow(() -> HASHER.verifyDummy(null));
    }
}