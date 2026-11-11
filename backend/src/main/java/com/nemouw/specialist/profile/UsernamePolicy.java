package com.nemouw.specialist.profile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Which usernames may not be taken. Matching ignores case and drops '.', '_', '-', so "a.d.m.i.n" is caught.
 * <ul>
 *   <li>Reserved words (reserved-usernames.txt): matched exactly, and also after decoding common leet digits
 *   ("adm1n"), but only for names of at least {@value #MIN_LEET_LENGTH} characters, so "m3" is not "me".</li>
 *   <li>The platform name "nemouw": blocked anywhere inside a name.</li>
 * </ul>
 * Format and length are the Validator's job, not this class's.
 */
final class UsernamePolicy {
    private static final String RESOURCE = "/reserved-usernames.txt";
    private static final int MIN_LEET_LENGTH = 4;
    private static final Set<String> PLATFORM_WORDS = Set.of("nemouw");
    private static final Set<String> RESERVED = load(RESOURCE);

    private UsernamePolicy() {
    }

    static boolean isReserved(String username) {
        if (username == null) return false;
        String stripped = strip(username);
        String decoded = deLeet(stripped);
        if (RESERVED.contains(stripped)) return true;
        if (decoded.length() >= MIN_LEET_LENGTH && RESERVED.contains(decoded)) return true;
        for (String word : PLATFORM_WORDS) {
            if (stripped.contains(word) || decoded.contains(word)) return true;
        }
        return false;
    }

    private static String strip(String s) {
        return s.trim().toLowerCase(Locale.ROOT).replaceAll("[._-]", "");
    }

    private static String deLeet(String s) {
        return s.replace('0', 'o').replace('1', 'i').replace('3', 'e')
                .replace('4', 'a').replace('5', 's').replace('7', 't');
    }

    static Set<String> load(String resource) {
        Set<String> set = new HashSet<>(Set.of("me", "byid"));   // the two words the database itself refuses
        try (InputStream in = UsernamePolicy.class.getResourceAsStream(resource)) {
            if (in == null) throw new IllegalStateException(resource + " is not on the classpath");
            try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    set.add(strip(line));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return Set.copyOf(set);
    }
}