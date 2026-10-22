package com.nemouw.validation;

import com.nemouw.error.ValidationException;
import com.nemouw.error.FieldError;
import com.nemouw.error.FieldErrorCode;
import com.nemouw.error.FieldErrors;
import com.nemouw.auth.policy.PasswordPolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class Validator {
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private final List<FieldError> errors = new ArrayList<>();

    public Validator required(String field, String v) {
        if ((v == null || v.isBlank())
                && errors.stream().noneMatch(e -> e.field().equals(field) && e.code() == FieldErrorCode.REQUIRED))
            errors.add(FieldErrors.required(field));
        return this;
    }

    public Validator email(String field, String v) {
        if (v != null && !v.isBlank() && !EMAIL.matcher(v.trim()).matches())
            errors.add(FieldErrors.mustBeEmail(field));
        return this;
    }

    public Validator requiredEmail(String field, String v) {
        if (v == null || v.isBlank()) {
            errors.add(FieldErrors.required(field));
            return this;
        }
        return email(field, v);
    }

    /**
     * Applies {@link PasswordPolicy}.
     *
     * @param personalInfo values the password must not contain (e.g. email local part)
     */
    public Validator password(String field, String v, String... personalInfo) {
        if (v == null || v.isEmpty()) return required(field, v);
        errors.addAll(PasswordPolicy.check(field, v, personalInfo));
        return this;
    }

    public Validator check(boolean ok, FieldError error) {
        if (!ok) errors.add(error);
        return this;
    }

    public void throwIfInvalid() {
        if (!errors.isEmpty()) throw new ValidationException(errors);
    }
}