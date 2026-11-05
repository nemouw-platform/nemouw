package com.nemouw.support;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Binds a properties record from the application.properties that ships, so tests use the shipped values.
 * A placeholder with a default, such as ${DEV_FIXED_OTP:}, resolves to that default, as it does when no env var is set.
 */
public final class ShippedProperties {
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{[^}:]+(?::([^}]*))?}");

    private ShippedProperties() {
    }

    public static <T> T bind(String prefix, Class<T> type) {
        Properties p = new Properties();
        try (InputStream in = ShippedProperties.class.getResourceAsStream("/application.properties")) {
            if (in == null) throw new IllegalStateException("application.properties is not on the test classpath");
            p.load(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        for (String name : p.stringPropertyNames()) {
            p.setProperty(name, resolveDefaults(p.getProperty(name)));
        }
        return new Binder(new MapConfigurationPropertySource(p)).bind(prefix, type)
                .orElseThrow(() -> new IllegalStateException("No properties under " + prefix));
    }

    private static String resolveDefaults(String value) {
        return PLACEHOLDER.matcher(value).replaceAll(m ->
                Matcher.quoteReplacement(m.group(1) != null ? m.group(1) : m.group()));   // no default: leave as written
    }
}