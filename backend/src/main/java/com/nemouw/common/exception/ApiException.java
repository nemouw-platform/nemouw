package com.nemouw.common.exception;

import org.springframework.http.HttpHeaders;

import java.util.List;

/**
 * Base class for every error that is meant to reach the client. Throw it (or a subclass) from anywhere;
 * GlobalExceptionHandler turns it into the JSON error body.
 * <p>
 * Subclasses exist only where an error carries extra data in the response:
 * {@link ValidationException} (field errors) and {@link TooManyRequestsException} (Retry-After).
 * Create the common ones through {@link Errors}.
 */
public class ApiException extends RuntimeException {
    private final ErrorCode errorCode;

    public ApiException(ErrorCode errorCode) {
        this(errorCode, errorCode.defaultMessage());
    }

    public ApiException(ErrorCode errorCode, String message) {
        // No stack trace: these are expected outcomes (wrong password, rate limit), not bugs,
        // and filling a stack trace on every one is wasted work.
        super(message, null, false, false);
        this.errorCode = errorCode;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public int status() {
        return errorCode.status();
    }

    public String code() {
        return errorCode.name();
    }

    /** Extra field-level details for the response body. Empty unless a subclass has some. */
    public List<FieldError> fieldErrors() {
        return List.of();
    }

    /** Extra response headers. Empty unless a subclass needs some. */
    public HttpHeaders responseHeaders() {
        return HttpHeaders.EMPTY;
    }
}