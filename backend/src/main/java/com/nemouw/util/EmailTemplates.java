package com.nemouw.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EmailTemplates {
    // {{{name}}} = raw, {{name}} = HTML-escaped
    private static final Pattern PLACEHOLDER =
            Pattern.compile("\\{\\{\\{(\\w+)\\}\\}\\}|\\{\\{(\\w+)\\}\\}");
    private static final Map<String, String> CACHE = new ConcurrentHashMap<>();

    private EmailTemplates() {}

    /** Renders email-templates/{name}.html inside email-templates/layout.html. */
    public static String html(String name, Map<String, String> vars) {
        String content = fill(load(name + ".html"), vars, true);
        return fill(load("layout.html"), Map.of("content", content), true);
    }

    /** Renders email-templates/{name}.txt (no escaping). */
    public static String text(String name, Map<String, String> vars) {
        return fill(load(name + ".txt"), vars, false);
    }

    private static String fill(String template, Map<String, String> vars, boolean escape) {
        return PLACEHOLDER.matcher(template).replaceAll(m -> {
            boolean raw = m.group(1) != null;
            String key = raw ? m.group(1) : m.group(2);
            String value = vars.get(key);
            if (value == null) {
                throw new IllegalArgumentException("Missing email template variable: " + key);
            }
            return Matcher.quoteReplacement(raw || !escape ? value : escapeHtml(value));
        });
    }

    private static String load(String file) {
        return CACHE.computeIfAbsent(file, f -> {
            try (InputStream in = EmailTemplates.class.getResourceAsStream("/email-templates/" + f)) {
                if (in == null) throw new IllegalStateException("Email template not found: " + f);
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    private static String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}