package com.nemouw.user.identity;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.crypto.password.PasswordEncoder;

import static com.nemouw.user.AuthConstants.ARGON2_ITERATIONS;
import static com.nemouw.user.AuthConstants.ARGON2_MEMORY_KB;
import static com.nemouw.user.AuthConstants.ARGON2_PARALLELISM;
import static com.nemouw.user.AuthConstants.PASSWORD_MAX_LENGTH;
import static com.nemouw.support.Fixtures.PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PasswordHasherTest {

    /** The production encoder, so the Argon2 parameters under test are the ones that ship. */
    private static final PasswordEncoder ENCODER = new PasswordConfig().passwordEncoder();
    private static PasswordHasher hasher;
    private static String storedHash;

    @BeforeAll
    static void hashOnce() {
        hasher = new PasswordHasher(ENCODER);
        storedHash = hasher.hash(PASSWORD);
    }

    @Nested
    class HappyPaths {

        @Test
        void producesAnArgon2idHashWithTheConfiguredCostParameters() {
            assertThat(storedHash)
                    .startsWith("$argon2id$")
                    .contains("m=" + ARGON2_MEMORY_KB + ",t=" + ARGON2_ITERATIONS + ",p=" + ARGON2_PARALLELISM);
        }

        @Test
        void neverStoresThePasswordInTheHash() {
            assertThat(storedHash).doesNotContain(PASSWORD);
        }

        @Test
        void givesTheSamePasswordADifferentHashEachTime() {
            String second = hasher.hash(PASSWORD);

            assertThat(second).isNotEqualTo(storedHash);
        }

        @Test
        void verifiesTheCorrectPassword() {
            assertThat(hasher.verify(PASSWORD, storedHash)).isTrue();
        }

        @Test
        void verifiesAUnicodePasswordInArabicJapaneseAndAccentedScripts() {
            String password = "p\u00E4ssw\u00F6rd-\u0645\u0631\u062D\u0628\u0627-\u65E5\u672C\u8A9E";
            String hash = hasher.hash(password);

            assertThat(hasher.verify(password, hash)).isTrue();
        }

        @Test
        void verifyDummyDoesNotThrowForARealOrANullPassword() {
            assertThatCode(() -> hasher.verifyDummy(PASSWORD)).doesNotThrowAnyException();
            assertThatCode(() -> hasher.verifyDummy(null)).doesNotThrowAnyException();
        }
    }

    @Nested
    class BoundaryValues {

        @Test
        void verifiesAPasswordOfExactlyTheMaximumLength() {
            String password = "a".repeat(PASSWORD_MAX_LENGTH);
            String hash = hasher.hash(password);

            assertThat(hasher.verify(password, hash)).isTrue();
        }

        @Test
        void verifiesAMaximumLengthPasswordMadeOfEmojiBecauseLengthIsCountedInCodePoints() {
            String password = "\uD83D\uDE00".repeat(PASSWORD_MAX_LENGTH);
            String hash = hasher.hash(password);

            assertThat(hasher.verify(password, hash)).isTrue();
        }

        @Test
        void rejectsAPasswordOneCharacterOverTheMaximumEvenWhenItMatchesTheHash() {
            String password = "a".repeat(PASSWORD_MAX_LENGTH + 1);
            String hash = hasher.hash(password);

            assertThat(hasher.verify(password, hash)).isFalse();
        }

        @Test
        void doesNotNormalizeUnicodeSoComposedAndDecomposedLettersAreDifferentPasswords() {
            String composed = "caf\u00E9-vK9#mQ2$";
            String decomposed = "cafe\u0301-vK9#mQ2$";
            String hash = hasher.hash(composed);

            assertThat(hasher.verify(decomposed, hash)).isFalse();
        }
    }

    @Nested
    class ErrorPaths {

        @Test
        void rejectsAWrongPassword() {
            assertThat(hasher.verify(PASSWORD + "x", storedHash)).isFalse();
        }

        @Test
        void rejectsAnEmptyPassword() {
            assertThat(hasher.verify("", storedHash)).isFalse();
        }

        @Test
        void isCaseSensitive() {
            assertThat(hasher.verify(PASSWORD.toLowerCase(), storedHash)).isFalse();
            assertThat(hasher.verify(PASSWORD.toUpperCase(), storedHash)).isFalse();
        }

        @Test
        void treatsWhitespaceAsPartOfThePassword() {
            assertThat(hasher.verify(" " + PASSWORD, storedHash)).isFalse();
            assertThat(hasher.verify(PASSWORD + " ", storedHash)).isFalse();
            assertThat(hasher.verify(PASSWORD.replace("#", "# "), storedHash)).isFalse();
        }

        @Test
        void returnsFalseForANullPasswordOrANullHash() {
            assertThat(hasher.verify(null, storedHash)).isFalse();
            assertThat(hasher.verify(PASSWORD, null)).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {"not-a-hash", "$argon2id$truncated"})
        void returnsFalseInsteadOfThrowingForAMalformedStoredHash(String malformedHash) {
            assertThat(hasher.verify(PASSWORD, malformedHash)).isFalse();
        }
    }

    @Nested
    class DependencyFailures {

        private final PasswordEncoder encoder = mock(PasswordEncoder.class);

        @Test
        void failsFastWhenTheEncoderCannotHashTheTimingDummyAtStartup() {
            when(encoder.encode(any())).thenThrow(new IllegalStateException("Argon2 could not allocate 19 MiB"));

            assertThatThrownBy(() -> new PasswordHasher(encoder)).isInstanceOf(IllegalStateException.class);
        }

        @Test
        void propagatesAnEncoderFailureDuringVerificationInsteadOfSayingTheCredentialsAreFine() {
            when(encoder.encode(any())).thenReturn("$argon2id$dummy");
            when(encoder.matches(any(), anyString())).thenThrow(new IllegalStateException("Argon2 out of memory"));
            PasswordHasher failing = new PasswordHasher(encoder);

            assertThatThrownBy(() -> failing.verify(PASSWORD, "$argon2id$stored"))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        void doesNotSpendArgon2WorkOnAnOverLongPassword() {
            when(encoder.encode(any())).thenReturn("$argon2id$dummy");
            PasswordHasher guarded = new PasswordHasher(encoder);

            boolean verified = guarded.verify("a".repeat(PASSWORD_MAX_LENGTH + 1), "$argon2id$stored");

            assertThat(verified).isFalse();
            verify(encoder, never()).matches(any(), anyString());
        }

        @Test
        void verifyDummyRunsOneRealHashComparisonSoUnknownAccountsCostTheSameTime() {
            when(encoder.encode(any())).thenReturn("$argon2id$dummy");
            PasswordHasher timing = new PasswordHasher(encoder);

            timing.verifyDummy(PASSWORD);

            verify(encoder).matches(PASSWORD, "$argon2id$dummy");
        }
    }
}
