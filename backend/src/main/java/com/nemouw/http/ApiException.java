package com.nemouw.http;

import java.util.List;

public class ApiException extends RuntimeException {

    public record FieldError(String field, String code, String message) {
    }

    private final int status;
    private final String code;
    private final List<FieldError> errors;

    private ApiException(int status, String code, String message, List<FieldError> errors) {
        super(message);
        this.status = status;
        this.code = code;
        this.errors = errors;
    }

    public static ApiException badRequest(String code, String msg) {
        return new ApiException(400, code, msg, List.of());
    }

    public static ApiException unauthorized(String code, String msg) {
        return new ApiException(401, code, msg, List.of());
    }

    public static ApiException forbidden(String code, String msg) {
        return new ApiException(403, code, msg, List.of());
    }

    public static ApiException notFound(String code, String msg) {
        return new ApiException(404, code, msg, List.of());
    }

    public static ApiException conflict(String code, String msg) {
        return new ApiException(409, code, msg, List.of());
    }

    public static ApiException validation(List<FieldError> errors) {
        return new ApiException(422, "VALIDATION_ERROR", "Validation failed", errors);
    }

    public int status() {
        return status;
    }

    public String code() {
        return code;
    }

    public List<FieldError> errors() {
        return errors;
    }
}