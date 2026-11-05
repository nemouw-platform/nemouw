package com.nemouw.specialist.credential;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

public enum CredentialVerificationStatus {
    PENDING("pending"),
    VERIFIED("verified"),
    REJECTED("rejected");

    private final String code;

    CredentialVerificationStatus(String code) {
        this.code = code;
    }

    @JsonValue
    public String code() {
        return code;
    }

    static Optional<CredentialVerificationStatus> tryFrom(String code) {
        if (code == null) return Optional.empty();
        String trimmed = code.trim();
        for (CredentialVerificationStatus v : values()) {
            if (v.code.equals(trimmed)) return Optional.of(v);
        }
        return Optional.empty();
    }

    static CredentialVerificationStatus fromCode(String code) {
        return tryFrom(code).orElseThrow(() ->
                new IllegalArgumentException("Unknown CredentialVerificationStatus code: " + code));
    }

    static String allCodes() {
        return Arrays.stream(values()).map(CredentialVerificationStatus::code).collect(Collectors.joining(", "));
    }
}