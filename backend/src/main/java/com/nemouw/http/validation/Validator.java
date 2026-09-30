package com.nemouw.http.validation;

import com.nemouw.http.ApiException;
import com.nemouw.http.ApiException.FieldError;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class Validator {
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private final List<FieldError> errors = new ArrayList<>();

    public Validator required(String field, String v) {
        if (v == null || v.isBlank()) errors.add(new FieldError(field, "REQUIRED", "is required"));
        return this;
    }

    public Validator email(String field, String v) {
        if (v != null && !v.isBlank() && !EMAIL.matcher(v.trim()).matches())
            errors.add(new FieldError(field, "MUST_BE_EMAIL", "must be a valid email address"));
        return this;
    }

    /**
     * Applies {@link PasswordPolicy}.
     *
     * @param personalInfo values the password must not contain (e.g. email local part)
     */
    public Validator password(String field, String v, String... personalInfo) {
        errors.addAll(PasswordPolicy.check(field, v, personalInfo));
        return this;
    }

    public Validator check(boolean ok, String field, String code, String message) {
        if (!ok) errors.add(new FieldError(field, code, message));
        return this;
    }

    public void throwIfInvalid() {
        if (!errors.isEmpty()) throw ApiException.validation(errors);
    }
}
