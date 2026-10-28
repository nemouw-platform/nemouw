package com.nemouw.common.enums;

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

    @JsonValue
    public String code() {
        return code;
    }

    public static Optional<UserRole> tryFrom(String code) {
        for (UserRole v : values()) {
            if (v.code.equals(code)) return Optional.of(v);
        }
        return Optional.empty();
    }

    public static UserRole fromCode(String code) {
        return tryFrom(code).orElseThrow(() ->
                new IllegalArgumentException("Unknown UserRole code: " + code));
    }
}