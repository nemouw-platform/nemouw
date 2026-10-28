package com.nemouw.user.identity;

import com.nemouw.common.exception.FieldErrors;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

import static com.nemouw.user.AuthConstants.PASSWORD_MAX_LENGTH;
import static com.nemouw.user.AuthConstants.PASSWORD_MIN_LENGTH;
import static org.assertj.core.api.Assertions.assertThat;

/** Guards the NCSC list shipped in src/main/resources: if it is dropped, the policy would silently fall back to ~30 words. */
class PasswordBlocklistFileTest {

    private static final String RESOURCE = "/common-passwords.txt";
    private static final String FIELD = "password";

    private static List<String> loadEntries() throws IOException {
        try (InputStream in = PasswordPolicy.class.getResourceAsStream(RESOURCE)) {
            assertThat(in)
                    .as(RESOURCE + " is not on the classpath, so only the built-in baseline list would be used")
                    .isNotNull();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            return reader.lines()
                    .map(line -> line.trim().toLowerCase(Locale.ROOT))
                    .filter(entry -> !entry.isEmpty())
                    .toList();
        }
    }

    private static boolean isWithinPolicyLengthAndAscii(String entry) {
        int length = entry.codePointCount(0, entry.length());
        return entry.chars().allMatch(c -> c < 128)
                && length >= PASSWORD_MIN_LENGTH
                && length <= PASSWORD_MAX_LENGTH;
    }

    @Test
    void blocklistFileIsOnTheClasspathAndHoldsTheFullNcscList() throws Exception {
        List<String> entries = loadEntries();

        assertThat(entries).hasSizeGreaterThan(90_000);
    }

    @Test
    void everyAsciiEntryWithinTheAllowedLengthIsRejectedAsCommon() throws Exception {
        List<String> eligible = loadEntries().stream()
                .filter(PasswordBlocklistFileTest::isWithinPolicyLengthAndAscii)
                .toList();

        List<String> notRejected = eligible.stream()
                .filter(entry -> !PasswordPolicy.check(FIELD, entry).contains(FieldErrors.tooCommon(FIELD)))
                .limit(10)
                .toList();

        assertThat(eligible).as("no blocklist entry is within the allowed length range").isNotEmpty();
        assertThat(notRejected).as("first entries that were not rejected").isEmpty();
    }
}
