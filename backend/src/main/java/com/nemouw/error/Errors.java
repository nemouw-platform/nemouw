package com.nemouw.error;

/**
 * Every client-facing error the API can raise, named, in one place. Codes here are part of the API contract.
 */
public final class Errors {
    private Errors() {
    }

    public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    public static final String INVALID_OR_EXPIRED_TOKEN = "INVALID_OR_EXPIRED_TOKEN";
    public static final String EMAIL_TAKEN = "EMAIL_TAKEN";
    public static final String ACCOUNT_NOT_VERIFIED = "ACCOUNT_NOT_VERIFIED";
    public static final String ACCOUNT_SUSPENDED = "ACCOUNT_SUSPENDED";
    public static final String INVALID_TOKEN = "INVALID_TOKEN";
    public static final String MALFORMED_JSON = "MALFORMED_JSON";
    public static final String FORBIDDEN = "FORBIDDEN";
    public static final String UNAUTHENTICATED = "UNAUTHENTICATED";

    // ---- Auth ----
    public static ApiException invalidCredentials() {
        return ApiException.unauthorized(INVALID_CREDENTIALS, "Invalid credentials");
    }

    public static ApiException invalidOrExpiredCode() {
        return ApiException.badRequest(INVALID_OR_EXPIRED_TOKEN, "Code is invalid, expired, or locked");
    }

    public static ApiException emailTaken() {
        return ApiException.conflict(EMAIL_TAKEN, "Email is already registered");
    }

    public static ApiException accountNotVerified() {
        return ApiException.forbidden(ACCOUNT_NOT_VERIFIED, "Verify your email to continue");
    }

    public static ApiException accountSuspended() {
        return ApiException.forbidden(ACCOUNT_SUSPENDED, "This account is suspended");
    }

    public static ApiException invalidRefreshToken() {
        return ApiException.unauthorized(INVALID_TOKEN, "Invalid or expired refresh token");
    }

    // ---- Request ----
    public static ApiException malformedJson(String message) {
        return ApiException.badRequest(MALFORMED_JSON, message);
    }

    public static ApiException tooManyRequests(int retryAfterSeconds) {
        return ApiException.tooManyRequests(MALFORMED_JSON, "Too many requests", retryAfterSeconds);
    }

    // ---- Middleware ----
    public static ApiException missingAccessToken() {
        return ApiException.unauthorized(UNAUTHENTICATED, "Authentication required");
    }
    public static ApiException invalidAccessToken() {
        return ApiException.unauthorized(INVALID_TOKEN, "Invalid or expired access token");
    }
    public static ApiException forbidden() {
        return ApiException.forbidden(FORBIDDEN, "You do not have permission to perform this action");
    }
}