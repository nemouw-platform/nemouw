package com.nemouw.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Optional;

public enum VerificationPurpose {
    SIGNUP_OTP("signup_otp"),
    PASSWORD_RESET("password_reset");

    private final String code;

    VerificationPurpose(String code) {
        this.code = code;
    }

    /** The exact value stored in the database (and used in JSON). */
    @JsonValue
    public String code() {
        return code;
    }

    /** Lookup by database value; empty if {@code code} is null or unknown. */
    public static Optional<VerificationPurpose> tryFrom(String code) {
        for (VerificationPurpose v : values()) {
            if (v.code.equals(code)) return Optional.of(v);
        }
        return Optional.empty();
    }

    /** Lookup by database value; throws if unknown. */
    public static VerificationPurpose fromCode(String code) {
        return tryFrom(code).orElseThrow(() ->
                new IllegalArgumentException("Unknown VerificationPurpose code: " + code));
    }
}
