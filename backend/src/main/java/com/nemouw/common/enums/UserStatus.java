package com.nemouw.common.enums;

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

    @JsonValue
    public String code() {
        return code;
    }

    public static Optional<UserStatus> tryFrom(String code) {
        for (UserStatus v : values()) {
            if (v.code.equals(code)) return Optional.of(v);
        }
        return Optional.empty();
    }

    public static UserStatus fromCode(String code) {
        return tryFrom(code).orElseThrow(() ->
                new IllegalArgumentException("Unknown UserStatus code: " + code));
    }
}