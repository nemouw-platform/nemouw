package com.nemouw.syndicate.management;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Optional;

public enum SyndicateStatus {
    ACTIVE("active");

    private final String code;

    SyndicateStatus(String code) {
        this.code = code;
    }

    @JsonValue
    public String code() {
        return code;
    }

    static Optional<SyndicateStatus> tryFrom(String code) {
        for (SyndicateStatus v : values()) {
            if (v.code.equals(code)) return Optional.of(v);
        }
        return Optional.empty();
    }

    static SyndicateStatus fromCode(String code) {
        return tryFrom(code).orElseThrow(() ->
                new IllegalArgumentException("Unknown SyndicateStatus code: " + code));
    }
}