// GENERATED FILE - DO NOT EDIT.
// Source: check constraint specialists.verification_status
// Regenerate: mvn compile exec:java -Dexec.mainClass=com.nemouw.tools.GenerateEnums
package com.nemouw.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Optional;

public enum SpecialistVerificationStatus {
    PENDING("pending"),
    ACTIVE("active"),
    REJECTED("rejected");

    private final String code;

    SpecialistVerificationStatus(String code) {
        this.code = code;
    }

    /** The exact value stored in the database (and used in JSON). */
    @JsonValue
    public String code() {
        return code;
    }

    /** Lookup by database value; empty if {@code code} is null or unknown. */
    public static Optional<SpecialistVerificationStatus> tryFrom(String code) {
        for (SpecialistVerificationStatus v : values()) {
            if (v.code.equals(code)) return Optional.of(v);
        }
        return Optional.empty();
    }

    /** Lookup by database value; throws if unknown. */
    public static SpecialistVerificationStatus fromCode(String code) {
        return tryFrom(code).orElseThrow(() ->
                new IllegalArgumentException("Unknown SpecialistVerificationStatus code: " + code));
    }
}
