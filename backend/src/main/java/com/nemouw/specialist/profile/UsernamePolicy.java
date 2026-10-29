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
 * Which usernames may not be taken: the reserved list in reserved-usernames.txt, and anything containing the
 * platform name. Both are matched after dropping '.', '_', '-' and decoding common leet digits, so "a.d.m.i.n",
 * "adm1n" and "n3m0uw" do not slip through. Format and length are the Validator's job, not this class's.
 */
final class UsernamePolicy {
    private static final String RESOURCE = "/reserved-usernames.txt";

    // Matched anywhere inside the normalized username. Extend with other spellings of the platform name.
    private static final Set<String> PLATFORM_WORDS = Set.of("nemou", "nemow");

    private static final Set<String> RESERVED = load();

    private UsernamePolicy() {
    }

    static boolean isReserved(String username) {
        if (username == null) return false;
        String stripped = strip(username);
        String decoded = deLeet(stripped);
        if (RESERVED.contains(stripped) || RESERVED.contains(decoded)) return true;
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

    private static Set<String> load() {
        Set<String> set = new HashSet<>(Set.of("me", "byid"));   // the two words the database itself refuses
        try (InputStream in = UsernamePolicy.class.getResourceAsStream(RESOURCE)) {
            if (in == null) throw new IllegalStateException(RESOURCE + " is not on the classpath");
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