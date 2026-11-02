package com.nemouw.specialist.availability;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

/** A day of the week as the API and the database spell it: the lowercase English name, Monday first. */
public enum WeekDay {
    MONDAY("monday"),
    TUESDAY("tuesday"),
    WEDNESDAY("wednesday"),
    THURSDAY("thursday"),
    FRIDAY("friday"),
    SATURDAY("saturday"),
    SUNDAY("sunday");

    private final String code;

    WeekDay(String code) {
        this.code = code;
    }

    @JsonValue
    public String code() {
        return code;
    }

    /** Exact lowercase code, e.g. "monday"; surrounding whitespace is ignored. */
    public static Optional<WeekDay> tryFrom(String code) {
        if (code == null) return Optional.empty();
        String trimmed = code.trim();
        for (WeekDay v : values()) {
            if (v.code.equals(trimmed)) return Optional.of(v);
        }
        return Optional.empty();
    }

    public static WeekDay fromCode(String code) {
        return tryFrom(code).orElseThrow(() ->
                new IllegalArgumentException("Unknown WeekDay code: " + code));
    }

    /** "monday, tuesday, ..." for validation messages. */
    static String allCodes() {
        return Arrays.stream(values()).map(WeekDay::code).collect(Collectors.joining(", "));
    }
}