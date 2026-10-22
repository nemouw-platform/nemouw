package com.nemouw.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * The JSON error contract, same shape as before:
 * {"status", "code", "message", "trace_id", "errors"?}. snake_case comes from the global Jackson setting.
 */
public record ErrorBody(int status, String code, String message, String traceId,
                        @JsonInclude(JsonInclude.Include.NON_EMPTY) List<FieldError> errors) {
}