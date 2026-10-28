package com.nemouw.user.identity;

import com.nemouw.common.exception.FieldError;
import com.nemouw.common.exception.FieldErrors;
import com.nemouw.common.util.TextNormalizer;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 64;

    private static Set<String> loadBlocklist() {
        Set<String> set = new HashSet<>(Set.of("password", "passwd", "qwerty", "qwertyuiop", "azerty", "letmein", "welcome", "admin", "administrator", "iloveyou", "monkey", "dragon", "football", "baseball", "sunshine", "princess", "master", "login", "changeme", "trustno", "abcdefgh", "secret", "superman", "batman", "shadow", "12345678", "123456789", "1234567890", "specialist", "patient"));
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
        if (password == null || password.isEmpty()) return errors;

        if (password.isBlank()) {
            errors.add(FieldErrors.required(field));
            return errors;
        }

        int rawLength = password.codePointCount(0, password.length());
        if (rawLength > MAX_LENGTH) errors.add(FieldErrors.tooLong(field, MAX_LENGTH));
        if (rawLength < MIN_LENGTH) errors.add(FieldErrors.tooShort(field, MIN_LENGTH));

        if (!errors.isEmpty()) return errors; // fail early

        String normalizedPw = TextNormalizer.nfc(password);
        String[] forms = normalizedForms(normalizedPw);

        if (isCommon(forms)) errors.add(FieldErrors.tooCommon(field));
        if (containsContextWord(forms, personalInfo)) errors.add(FieldErrors.containsPersonalInfo(field));

        return errors;
    }

    /**
     * lowercase, letters only ("Password1!" -> "password"), and leet-decoded
     * ("P@ssw0rd" -> "password").
     */
    private static String[] normalizedForms(String password) {
        String lower = password.toLowerCase(Locale.ROOT);
        String lettersOnly = lower.replaceAll("[^\\p{L}]", "");
        String leetDecoded = lower.replace('@', 'a').replace('0', 'o').replace('3', 'e').replace('$', 's').replace('5', 's').replace('7', 't').replace('1', 'i').replaceAll("[^\\p{L}]", "");

        // Prevent validation anomalies with strings that strip out entirely into empty
        // values
        List<String> list = new ArrayList<>();
        list.add(lower);
        if (!lettersOnly.isEmpty()) list.add(lettersOnly);
        if (!leetDecoded.isEmpty() && !leetDecoded.equals(lettersOnly)) list.add(leetDecoded);

        return list.toArray(new String[0]);
    }

    private static boolean isCommon(String[] forms) {
        for (String f : forms)
            if (COMMON.contains(f)) return true;
        return false;
    }

    private static boolean containsContextWord(String[] forms, String[] personalInfo) {
        Set<String> words = new HashSet<>(SERVICE_WORDS);
        if (personalInfo != null) {
            for (String part : personalInfo) {
                if (part == null) continue;
                addPersonalWord(words, TextNormalizer.nfc(part.trim()).toLowerCase(Locale.ROOT));
            }
        }
        for (String w : words)
            for (String f : forms)
                if (f.contains(w)) return true;
        return false;
    }

    /**
     * Adds the value as written ("amira.haddad") and with separators removed ("amirahaddad"), because people
     * type the name of their mailbox without the dots, dashes, underscores or plus tags.
     */
    private static void addPersonalWord(Set<String> words, String value) {
        if (value.length() >= MIN_PERSONAL_PART) words.add(value);
        String compact = value.replaceAll("[^\\p{L}\\p{N}]", "");
        if (compact.length() >= MIN_PERSONAL_PART) words.add(compact);
    }
}