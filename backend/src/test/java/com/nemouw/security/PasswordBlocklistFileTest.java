package com.nemouw.security;

import com.nemouw.error.FieldErrors;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.nemouw.Constants.PASSWORD_MAX_LENGTH;
import static com.nemouw.Constants.PASSWORD_MIN_LENGTH;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordBlocklistFileTest {

    private static final String RESOURCE = "/common-passwords.txt";
    private static final String FIELD = "password";

    private static List<String> loadEntries() throws Exception {
        try (InputStream in = PasswordPolicy.class.getResourceAsStream(RESOURCE)) {
            assertNotNull(in, RESOURCE + " is not on the classpath, so only the built-in baseline list is used");
            List<String> entries = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String entry = line.trim().toLowerCase(Locale.ROOT);
                    if (!entry.isEmpty())
                        entries.add(entry);
                }
            }
            return entries;
        }
    }

    @Test
    void blocklistFileIsOnTheClasspathAndHasTheFullList() throws Exception {
        assertTrue(loadEntries().size() > 50_000, "blocklist file looks too small to be the full list");
    }

    @Test
    void everyEntryThatPassesTheLengthRuleIsRejectedAsCommon() throws Exception {
        List<String> notRejected = new ArrayList<>();
        int checked = 0;

        for (String entry : loadEntries()) {
            int length = entry.codePointCount(0, entry.length());
            boolean ascii = entry.chars().allMatch(c -> c < 128);
            if (!ascii || length < PASSWORD_MIN_LENGTH || length > PASSWORD_MAX_LENGTH)
                continue;

            checked++;
            if (!PasswordPolicy.check(FIELD, entry).contains(FieldErrors.tooCommon(FIELD))) {
                notRejected.add(entry);
            }
        }

        assertTrue(checked > 0, "no blocklist entry is within the allowed length range");
        assertTrue(notRejected.isEmpty(), "not rejected: " + notRejected.subList(0, Math.min(10, notRejected.size())));
    }
}
