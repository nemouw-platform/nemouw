package com.nemouw.specialist.credential;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

public enum CredentialType {
    DEGREE("degree"),
    LICENSE("license"),
    CERTIFICATION("certification"),
    MEMBERSHIP("membership");

    private final String code;

    CredentialType(String code) {
        this.code = code;
    }

    @JsonValue
    public String code() {
        return code;
    }

    static Optional<CredentialType> tryFrom(String code) {
        if (code == null) return Optional.empty();
        String trimmed = code.trim();
        for (CredentialType v : values()) {
            if (v.code.equals(trimmed)) return Optional.of(v);
        }
        return Optional.empty();
    }

    static CredentialType fromCode(String code) {
        return tryFrom(code).orElseThrow(() ->
                new IllegalArgumentException("Unknown CredentialType code: " + code));
    }

    static String allCodes() {
        return Arrays.stream(values()).map(CredentialType::code).collect(Collectors.joining(", "));
    }
}