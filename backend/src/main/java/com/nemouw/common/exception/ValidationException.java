package com.nemouw.common.exception;

import java.util.List;

/** 422 with one entry per invalid field in {@code errors[]}. */
public class ValidationException extends ApiException {
    private final List<FieldError> errors;

    public ValidationException(List<FieldError> errors) {
        super(ErrorCode.VALIDATION_ERROR);
        this.errors = List.copyOf(errors);
    }

    @Override
    public List<FieldError> fieldErrors() {
        return errors;
    }
}