package com.nemouw.common.exception;

/**
 * Codes for the 422 {@code errors[]} entries. The frontend maps these to messages,
 * so the constant names are part of the API contract.
 */
public enum FieldErrorCode {
    REQUIRED,
    MUST_BE_EMAIL,
    TOO_SHORT,
    TOO_LONG,
    TOO_COMMON,
    CONTAINS_PERSONAL_INFO,
    INVALID_ROLE,
    INVALID_FORMAT
}