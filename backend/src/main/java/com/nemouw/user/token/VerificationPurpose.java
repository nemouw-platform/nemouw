package com.nemouw.user.token;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Optional;

public enum VerificationPurpose {
    SIGNUP_OTP("signup_otp"),
    PASSWORD_RESET("password_reset");

    private final String code;

    VerificationPurpose(String code) {
        this.code = code;
    }

    @JsonValue
    public String code() {
        return code;
    }

    public static Optional<VerificationPurpose> tryFrom(String code) {
        for (VerificationPurpose v : values()) {
            if (v.code.equals(code)) return Optional.of(v);
        }
        return Optional.empty();
    }

    static VerificationPurpose fromCode(String code) {
        return tryFrom(code).orElseThrow(() ->
                new IllegalArgumentException("Unknown VerificationPurpose code: " + code));
    }
}