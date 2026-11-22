package com.nemouw.common.enums;

import com.nemouw.common.exception.FieldErrors;
import com.nemouw.common.util.Validator;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Lookups by code for any {@link Coded} enum, so each enum does not repeat them. */
public final class Codes {
    private Codes() {
    }

    /** Exact code; surrounding whitespace is ignored. */
    public static <E extends Enum<E> & Coded> Optional<E> tryFrom(Class<E> type, String code) {
        if (code == null) return Optional.empty();
        String trimmed = code.trim();
        return Arrays.stream(type.getEnumConstants()).filter(e -> e.code().equals(trimmed)).findFirst();
    }

    public static <E extends Enum<E> & Coded> E fromCode(Class<E> type, String code) {
        return tryFrom(type, code).orElseThrow(() ->
                new IllegalArgumentException("Unknown " + type.getSimpleName() + " code: " + code));
    }

    /** For nullable columns: null stays null. */
    public static <E extends Enum<E> & Coded> E orNull(Class<E> type, String code) {
        return code == null ? null : fromCode(type, code);
    }

    public static <E extends Enum<E> & Coded> List<String> all(Class<E> type) {
        return Arrays.stream(type.getEnumConstants()).map(Coded::code).toList();
    }

    /** Reads an enum key of a PATCH body. Null when the key is null or invalid (the error is recorded). */
    public static <E extends Enum<E> & Coded> E fromBody(Map<String, Object> body, String key, Class<E> type,
                                                         Validator v) {
        Object raw = body.get(key);
        if (raw == null) return null;
        Optional<E> value = raw instanceof String s ? tryFrom(type, s) : Optional.empty();
        v.check(value.isPresent(), FieldErrors.invalidFormat(key, "must be one of " + String.join(", ", all(type))));
        return value.orElse(null);
    }
}
