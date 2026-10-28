package com.nemouw.common.exception;

/**
 * Named factories for the errors services and filters raise, so call sites read
 * {@code throw Errors.emailTaken()}. Codes, statuses and messages live in {@link ErrorCode}.
 */
public final class Errors {
    private Errors() {
    }

    // ---- User Auth ----
    public static ApiException invalidCredentials() {
        return new ApiException(ErrorCode.INVALID_CREDENTIALS);
    }

    public static ApiException invalidOrExpiredCode() {
        return new ApiException(ErrorCode.INVALID_OR_EXPIRED_TOKEN);
    }

    public static ApiException emailTaken() {
        return new ApiException(ErrorCode.EMAIL_TAKEN);
    }

    public static ApiException accountNotVerified() {
        return new ApiException(ErrorCode.ACCOUNT_NOT_VERIFIED);
    }

    public static ApiException accountSuspended() {
        return new ApiException(ErrorCode.ACCOUNT_SUSPENDED);
    }

    public static ApiException invalidRefreshToken() {
        return new ApiException(ErrorCode.INVALID_TOKEN, "Invalid or expired refresh token");
    }

    // ---- Request ----
    static ApiException malformedJson(String message) {
        return new ApiException(ErrorCode.MALFORMED_JSON, message);
    }

    public static TooManyRequestsException tooManyRequests(int retryAfterSeconds) {
        return new TooManyRequestsException(retryAfterSeconds);
    }

    // ---- Security / middleware ----
    public static ApiException missingAccessToken() {
        return new ApiException(ErrorCode.UNAUTHENTICATED);
    }

    public static ApiException invalidAccessToken() {
        return new ApiException(ErrorCode.INVALID_TOKEN);
    }

    public static ApiException forbidden() {
        return new ApiException(ErrorCode.FORBIDDEN);
    }
}