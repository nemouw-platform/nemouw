package com.nemouw.specialist.profile;

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

    @JsonValue
    public String code() {
        return code;
    }

    static Optional<SpecialistVerificationStatus> tryFrom(String code) {
        for (SpecialistVerificationStatus v : values()) {
            if (v.code.equals(code)) return Optional.of(v);
        }
        return Optional.empty();
    }

    static SpecialistVerificationStatus fromCode(String code) {
        return tryFrom(code).orElseThrow(() ->
                new IllegalArgumentException("Unknown SpecialistVerificationStatus code: " + code));
    }
}