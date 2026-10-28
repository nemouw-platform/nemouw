package com.nemouw.common.exception;

public final class FieldErrors {
    private FieldErrors() {
    }

    public static FieldError required(String field) {
        return new FieldError(field, FieldErrorCode.REQUIRED, "is required");
    }

    public static FieldError mustBeEmail(String field) {
        return new FieldError(field, FieldErrorCode.MUST_BE_EMAIL, "must be a valid email address");
    }

    public static FieldError tooShort(String field, int minChars) {
        return new FieldError(field, FieldErrorCode.TOO_SHORT, "must be at least " + minChars + " characters");
    }

    public static FieldError tooLong(String field, int maxChars) {
        return new FieldError(field, FieldErrorCode.TOO_LONG, "must be at most " + maxChars + " characters");
    }

    public static FieldError tooCommon(String field) {
        return new FieldError(field, FieldErrorCode.TOO_COMMON, "is too common, choose something less guessable");
    }

    public static FieldError containsPersonalInfo(String field) {
        return new FieldError(field, FieldErrorCode.CONTAINS_PERSONAL_INFO,
                "must not contain your email or the service name");
    }

    public static FieldError invalidRole(String field) {
        return new FieldError(field, FieldErrorCode.INVALID_ROLE, "only 'specialist' can self-register");
    }

    /** Generic "wrong shape" (e.g. OTP digits). The caller supplies the human-readable rule. */
    public static FieldError invalidFormat(String field, String message) {
        return new FieldError(field, FieldErrorCode.INVALID_FORMAT, message);
    }
}