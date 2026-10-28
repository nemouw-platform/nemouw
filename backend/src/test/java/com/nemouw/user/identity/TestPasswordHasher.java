package com.nemouw.user.identity;

import com.nemouw.support.ShippedProperties;

/**
 * Builds the production Argon2 hasher for tests outside this package.
 * PasswordConfig is package-private, so other packages' tests cannot construct it directly.
 */
public final class TestPasswordHasher {
    private TestPasswordHasher() {
    }

    public static PasswordHasher real() {
        return new PasswordHasher(new PasswordConfig().passwordEncoder(shippedProps()));
    }

    static PasswordProperties shippedProps() {
        return ShippedProperties.bind("app.user.password", PasswordProperties.class);
    }
}