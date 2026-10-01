package com.nemouw.security;

import com.nemouw.error.FieldErrors;
import com.nemouw.error.ApiException.FieldError;

import static com.nemouw.Constants.*;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The single place where password rules live:
 * length, blocklist, and context-specific words. No mandatory composition rules.
 */
public final class PasswordPolicy {
    private PasswordPolicy() {
    }

    // Email fragments shorter than this are ignored.
    private static final int MIN_PERSONAL_PART = 4;

    // Passwords containing service name are rejected (e.g. "nemouw2026").
    private static final Set<String> SERVICE_WORDS = Set.of("nemouw");

    // Currently it's from 100k-most-used-passwords-NCSC.txt file available online
    private static final String BLOCKLIST_RESOURCE = "/common-passwords.txt";

    // Baseline replaces the resource file if missing
    private static final Set<String> COMMON = loadBlocklist();

    private static Set<String> loadBlocklist() {
        Set<String> set = new HashSet<>(Set.of(
                "password", "passwd", "qwerty", "qwertyuiop", "azerty", "letmein", "welcome", "admin",
                "administrator", "iloveyou", "monkey", "dragon", "football", "baseball", "sunshine",
                "princess", "master", "login", "changeme", "trustno", "abcdefgh", "secret", "superman",
                "batman", "shadow", "12345678", "123456789", "1234567890", "specialist", "patient"));
        try (InputStream in = PasswordPolicy.class.getResourceAsStream(BLOCKLIST_RESOURCE)) {
            if (in != null) {
                try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = r.readLine()) != null) {
                        line = line.trim().toLowerCase(Locale.ROOT);
                        if (!line.isEmpty()) set.add(line);
                    }
                }
            }
        } catch (Exception e) {
            // keep the baseline
        }
        return Set.copyOf(set);
    }

    /**
     * @param personalInfo values the password must not contain (email local part).
     *                     Nulls and blanks are ignored.
     */
    public static List<FieldError> check(String field, String password, String... personalInfo) {
        List<FieldError> errors = new ArrayList<>();
        if (password == null || password.isEmpty()) return errors;   // "required" is reported separately

        // NFKC so visually identical passwords count the same. Apply the same normalization before hashing.
        String pw = TextNormalizer.nfkc(password);

        if (pw.codePointCount(0, pw.length()) < PASSWORD_MIN_LENGTH)
            errors.add(FieldErrors.tooShort(field, PASSWORD_MIN_LENGTH));
        if (pw.getBytes(StandardCharsets.UTF_8).length > PASSWORD_MAX_BYTES)
            errors.add(FieldErrors.tooLong(field, PASSWORD_MAX_BYTES));

        String[] forms = normalizedForms(pw);

        if (isCommon(forms))
            errors.add(FieldErrors.tooCommon(field));
        if (containsContextWord(forms, personalInfo))
            errors.add(FieldErrors.containsPersonalInfo(field));

        return errors;
    }

    /**
     * lowercase, letters only ("Password1!" -> "password"), and leet-decoded ("P@ssw0rd" -> "password").
     */
    private static String[] normalizedForms(String password) {
        String lower = password.toLowerCase(Locale.ROOT);
        String lettersOnly = lower.replaceAll("[^\\p{L}]", "");
        String leetDecoded = lower.replace('@', 'a').replace('0', 'o').replace('3', 'e')
                .replace('$', 's').replace('5', 's').replace('7', 't').replace('1', 'i')
                .replaceAll("[^\\p{L}]", "");
        return new String[]{lower, lettersOnly, leetDecoded};
    }

    private static boolean isCommon(String[] forms) {
        for (String f : forms) if (COMMON.contains(f)) return true;
        return false;
    }

    private static boolean containsContextWord(String[] forms, String[] personalInfo) {
        Set<String> words = new HashSet<>(SERVICE_WORDS);
        if (personalInfo != null) {
            for (String part : personalInfo) {
                if (part == null) continue;
                String p = part.trim().toLowerCase(Locale.ROOT);
                if (p.length() >= MIN_PERSONAL_PART) words.add(p);
            }
        }
        for (String w : words)
            for (String f : forms)
                if (f.contains(w)) return true;
        return false;
    }
}