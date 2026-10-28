package com.nemouw.common.exception;

/** One invalid field in a 422 response. Serialized as {"field", "code", "message"}. */
public record FieldError(String field, FieldErrorCode code, String message) {
}