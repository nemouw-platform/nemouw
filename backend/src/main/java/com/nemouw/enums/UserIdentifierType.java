package com.nemouw.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Optional;

public enum UserIdentifierType {
    EMAIL("email"),
    PHONE("phone"),
    USERNAME("username");

    private final String code;

    UserIdentifierType(String code) {
        this.code = code;
    }

    /** The exact value stored in the database (and used in JSON). */
    @JsonValue
    public String code() {
        return code;
    }

    /** Lookup by database value; empty if {@code code} is null or unknown. */
    public static Optional<UserIdentifierType> tryFrom(String code) {
        for (UserIdentifierType v : values()) {
            if (v.code.equals(code)) return Optional.of(v);
        }
        return Optional.empty();
    }

    /** Lookup by database value; throws if unknown. */
    public static UserIdentifierType fromCode(String code) {
        return tryFrom(code).orElseThrow(() ->
                new IllegalArgumentException("Unknown UserIdentifierType code: " + code));
    }
}
