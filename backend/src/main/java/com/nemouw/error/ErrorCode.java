package com.nemouw.error;

/**
 * The catalog of every error the API can return: one place that owns the code string, the HTTP status
 * and the default message. The constant name IS the code sent to clients, so renaming a constant
 * changes the API contract.
 */
public enum ErrorCode {
    // 400
    BAD_REQUEST(400, "Bad request"),
    MALFORMED_JSON(400, "Request body is not valid JSON"),
    INVALID_OR_EXPIRED_TOKEN(400, "The code is invalid or has expired"),

    // 401
    UNAUTHENTICATED(401, "Authentication required"),
    INVALID_TOKEN(401, "Invalid or expired access token"),
    INVALID_CREDENTIALS(401, "Invalid email or password"),

    // 403
    FORBIDDEN(403, "You do not have permission to perform this action"),
    ACCOUNT_NOT_VERIFIED(403, "Your account is not verified yet. Please verify your email to log in."),
    ACCOUNT_SUSPENDED(403, "This account has been suspended for violating our terms of service. "
            + "Please contact support if you believe this is a mistake."),
    CSRF_BLOCKED(403, "Origin not allowed"),

    // 404
    NOT_FOUND(404, "Route not found"),

    // 409
    EMAIL_TAKEN(409, "Email is already registered"),

    // 422
    VALIDATION_ERROR(422, "Validation failed"),

    // 429
    TOO_MANY_REQUESTS(429, "Too many requests"),

    // 500
    INTERNAL_ERROR(500, "Something went wrong");

    private final int status;
    private final String defaultMessage;

    ErrorCode(int status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public int status() {
        return status;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}