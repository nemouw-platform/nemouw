package com.nemouw.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Optional;

public enum SpecialistCredentialType {
    DEGREE("degree"),
    LICENSE("license"),
    CERTIFICATION("certification"),
    MEMBERSHIP("membership");

    private final String code;

    SpecialistCredentialType(String code) {
        this.code = code;
    }

    /** The exact value stored in the database (and used in JSON). */
    @JsonValue
    public String code() {
        return code;
    }

    /** Lookup by database value; empty if {@code code} is null or unknown. */
    public static Optional<SpecialistCredentialType> tryFrom(String code) {
        for (SpecialistCredentialType v : values()) {
            if (v.code.equals(code)) return Optional.of(v);
        }
        return Optional.empty();
    }

    /** Lookup by database value; throws if unknown. */
    public static SpecialistCredentialType fromCode(String code) {
        return tryFrom(code).orElseThrow(() ->
                new IllegalArgumentException("Unknown SpecialistCredentialType code: " + code));
    }
}
