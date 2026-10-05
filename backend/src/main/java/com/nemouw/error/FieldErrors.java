package com.nemouw.error;

import com.nemouw.error.ApiException.FieldError;

/**
 * Every field-level validation error the API can return in the 422 `errors[]` array.
 * The code strings are part of the API contract: the frontend maps them to messages.
 */
public final class FieldErrors {
    private FieldErrors() {
    }

    public static final String REQUIRED = "REQUIRED";
    public static final String MUST_BE_EMAIL = "MUST_BE_EMAIL";
    public static final String TOO_SHORT = "TOO_SHORT";
    public static final String TOO_LONG = "TOO_LONG";
    public static final String TOO_COMMON = "TOO_COMMON";
    public static final String CONTAINS_PERSONAL_INFO = "CONTAINS_PERSONAL_INFO";
    public static final String INVALID_ROLE = "INVALID_ROLE";
    public static final String INVALID_FORMAT = "INVALID_FORMAT";

    public static FieldError required(String field) {
        return new FieldError(field, REQUIRED, "is required");
    }

    public static FieldError mustBeEmail(String field) {
        return new FieldError(field, MUST_BE_EMAIL, "must be a valid email address");
    }

    public static FieldError tooShort(String field, int minChars) {
        return new FieldError(field, TOO_SHORT, "must be at least " + minChars + " characters");
    }

    public static FieldError tooLong(String field, int maxChars) {
        return new FieldError(field, TOO_LONG, "must be at most " + maxChars + " characters");
    }

    public static FieldError tooCommon(String field) {
        return new FieldError(field, TOO_COMMON, "is too common, choose something less guessable");
    }

    public static FieldError containsPersonalInfo(String field) {
        return new FieldError(field, CONTAINS_PERSONAL_INFO, "must not contain your email or the service name");
    }

    public static FieldError invalidRole(String field) {
        return new FieldError(field, INVALID_ROLE, "only 'specialist' can self-register");
    }

    /**
     * Generic "wrong shape" (e.g. OTP digits). The caller supplies the human-readable rule.
     */
    public static FieldError invalidFormat(String field, String message) {
        return new FieldError(field, INVALID_FORMAT, message);
    }
}