package com.nemouw.common.util;

import com.nemouw.common.exception.FieldError;
import com.nemouw.common.exception.FieldErrorCode;
import com.nemouw.common.exception.FieldErrors;
import com.nemouw.common.exception.ValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import static com.nemouw.common.util.ValidationLimits.EMAIL_MAX_LENGTH;

public final class Validator {
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern CODE = Pattern.compile("^[a-z][a-z0-9_]{1,49}$");      // specialty / condition codes
    private static final Pattern ISO_CODE = Pattern.compile("^[A-Za-z]{2}$");           // country, ISO 3166-1 alpha-2
    private static final Pattern CURRENCY_CODE = Pattern.compile("^[A-Za-z]{3}$");      // ISO 4217
    private static final Pattern PHONE_CODE = Pattern.compile("^\\+\\d{1,4}$");         // +961
    private static final Pattern USERNAME = Pattern.compile("^[a-z0-9._-]{3,30}$");

    private final List<FieldError> errors = new ArrayList<>();

    public Validator required(String field, String v) {
        if ((v == null || v.isBlank())
                && errors.stream().noneMatch(e -> e.field().equals(field) && e.code() == FieldErrorCode.REQUIRED))
            errors.add(FieldErrors.required(field));
        return this;
    }

    /**
     * For create-or-update request bodies. On create the value is mandatory. On update, null means "not sent"
     * and is accepted, but a blank value is rejected.
     */
    public Validator requiredForWrite(boolean create, String field, String v) {
        return (create || v != null) ? required(field, v) : this;
    }

    /**
     * Fails when the trimmed value is longer than {@code max} characters (code points). Null or blank is ignored.
     */
    public Validator maxLength(String field, String v, int max) {
        if (v != null && !v.isBlank() && tooLong(v, max))
            errors.add(FieldErrors.tooLong(field, max));
        return this;
    }

    private static boolean tooLong(String v, int max) {
        String t = v.trim();
        // Code points never outnumber chars, so a value of max chars or fewer can skip the count
        return t.length() > max && t.codePointCount(0, t.length()) > max;
    }

    public Validator notNull(String field, Object v) {
        if (v == null) errors.add(FieldErrors.required(field));
        return this;
    }

    // Format checks ignore null and blank: pair with required / requiredForWrite

    public Validator email(String field, String v) {
        if (v == null || v.isBlank()) return this;
        String trimmed = v.trim();
        // Length first: cheap, and keeps the regex away from oversized input
        if (tooLong(trimmed, EMAIL_MAX_LENGTH))
            errors.add(FieldErrors.tooLong(field, EMAIL_MAX_LENGTH));
        else if (!EMAIL.matcher(trimmed).matches())
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
     * Lowercase letters, digits and underscores, starting with a letter (case is normalized by the caller).
     */
    public Validator code(String field, String v) {
        return format(field, v == null ? null : v.toLowerCase(Locale.ROOT), CODE,
                "use lowercase letters, digits and underscores");
    }

    public Validator isoCode(String field, String v) {
        return format(field, v, ISO_CODE, "must be 2 letters");
    }

    public Validator currencyCode(String field, String v) {
        return format(field, v, CURRENCY_CODE, "must be 3 letters");
    }

    public Validator phoneCode(String field, String v) {
        return format(field, v, PHONE_CODE, "must look like +961");
    }

    public Validator username(String field, String v) {
        return format(field, v == null ? null : v.toLowerCase(Locale.ROOT), USERNAME,
                "use 3 to 30 characters: letters, digits, '.', '_' or '-'");
    }

    private Validator format(String field, String v, Pattern pattern, String rule) {
        if (v == null || v.isBlank()) return this;
        if (!pattern.matcher(v.trim()).matches())
            errors.add(FieldErrors.invalidFormat(field, rule));
        return this;
    }

    public Validator addAll(List<FieldError> more) {
        errors.addAll(more);
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