package com.nemouw.support;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

/** Binds a properties record from the application.properties that ships, so tests use the shipped values. */
public final class ShippedProperties {
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
        return new Binder(new MapConfigurationPropertySource(p)).bind(prefix, type)
                .orElseThrow(() -> new IllegalStateException("No properties under " + prefix));
    }
}