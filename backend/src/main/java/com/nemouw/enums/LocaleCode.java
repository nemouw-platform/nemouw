package com.nemouw.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Optional;

public enum LocaleCode {
    EN("en"),
    AR("ar"),
    FR("fr");

    private final String code;

    LocaleCode(String code) {
        this.code = code;
    }

    /** The exact value stored in the database (and used in JSON). */
    @JsonValue
    public String code() {
        return code;
    }

    /** Lookup by database value; empty if {@code code} is null or unknown. */
    public static Optional<LocaleCode> tryFrom(String code) {
        for (LocaleCode v : values()) {
            if (v.code.equals(code)) return Optional.of(v);
        }
        return Optional.empty();
    }

    /** Lookup by database value; throws if unknown. */
    public static LocaleCode fromCode(String code) {
        return tryFrom(code).orElseThrow(() ->
                new IllegalArgumentException("Unknown LocaleCode code: " + code));
    }

    /** Lookup by code, falling back to English for null or unknown values. */
    public static LocaleCode orDefault(String code) {
        return tryFrom(code).orElse(EN);
    }
}
