// GENERATED FILE - DO NOT EDIT.
// Source: lookup table user_roles
// Regenerate: mvn compile exec:java -Dexec.mainClass=com.nemouw.tools.GenerateEnums
package com.nemouw.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Optional;

public enum UserRole {
    ADMIN("admin"),
    PATIENT("patient"),
    SPECIALIST("specialist"),
    SYNDICATE_STAFF("syndicate_staff");

    private final String code;

    UserRole(String code) {
        this.code = code;
    }

    /** The exact value stored in the database (and used in JSON). */
    @JsonValue
    public String code() {
        return code;
    }

    /** Lookup by database value; empty if {@code code} is null or unknown. */
    public static Optional<UserRole> tryFrom(String code) {
        for (UserRole v : values()) {
            if (v.code.equals(code)) return Optional.of(v);
        }
        return Optional.empty();
    }

    /** Lookup by database value; throws if unknown. */
    public static UserRole fromCode(String code) {
        return tryFrom(code).orElseThrow(() ->
                new IllegalArgumentException("Unknown UserRole code: " + code));
    }
}
