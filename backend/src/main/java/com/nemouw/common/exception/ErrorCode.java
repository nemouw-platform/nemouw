package com.nemouw.common.exception;

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
    ONBOARDING_REQUIRED(403, "Complete onboarding first"),

    // 404
    NOT_FOUND(404, "Resource not found"),
    SPECIALIST_NOT_FOUND(404, "Specialist not found, complete onboarding first"),

    // 409
    EMAIL_TAKEN(409, "Email is already registered"),
    ISO_CODE_TAKEN(409, "A country with this ISO code already exists"),
    COUNTRY_IN_USE(409, "The country is still in use and cannot be deleted"),
    SPECIALTY_CODE_TAKEN(409, "A specialty with this code already exists"),
    CONDITION_CODE_TAKEN(409, "A condition with this code already exists"),
    CONDITION_IN_USE(409, "The condition is still in use and cannot be deleted"),
    SYNDICATE_EXISTS(409, "A syndicate for this specialty and country already exists"),
    OWNER_CANNOT_BE_REMOVED(409, "The syndicate owner cannot be removed"),
    ALREADY_OWNER(409, "This staff member is already the owner"),
    ACCOUNT_ALREADY_ACTIVE(409, "This account is already active, so its email cannot be changed"),
    USERNAME_TAKEN(409, "Username is already taken"),
    MEMBER_NUMBER_TAKEN(409, "This member number is already registered with the syndicate"),
    PROFILE_ALREADY_EXISTS(409, "A specialist profile already exists for this account"),
    SYNDICATE_LOCKED(409, "The syndicate cannot be changed after verification"),

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

    String defaultMessage() {
        return defaultMessage;
    }
}