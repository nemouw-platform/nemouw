package com.nemouw.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Optional;

public enum UserStatus {
    PENDING("pending"),
    ACTIVE("active"),
    SUSPENDED("suspended"),
    DELETED("deleted");

    private final String code;

    UserStatus(String code) {
        this.code = code;
    }

    /** The exact value stored in the database (and used in JSON). */
    @JsonValue
    public String code() {
        return code;
    }

    /** Lookup by database value; empty if {@code code} is null or unknown. */
    public static Optional<UserStatus> tryFrom(String code) {
        for (UserStatus v : values()) {
            if (v.code.equals(code)) return Optional.of(v);
        }
        return Optional.empty();
    }

    /** Lookup by database value; throws if unknown. */
    public static UserStatus fromCode(String code) {
        return tryFrom(code).orElseThrow(() ->
                new IllegalArgumentException("Unknown UserStatus code: " + code));
    }
}
