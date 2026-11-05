package com.nemouw.specialist.credential;

import com.nemouw.common.exception.FieldErrors;
import com.nemouw.common.util.Validator;
import com.nemouw.specialist.credential.CredentialDtos.CredentialWriteRequest;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import static com.nemouw.common.util.TextNormalizer.blankToNull;
import static com.nemouw.common.util.ValidationLimits.TEXT_MAX;

final class CredentialFieldsValidator {
    private CredentialFieldsValidator() {
    }

    static CredentialFields validated(CredentialWriteRequest r) {
        Validator v = new Validator()
                .required("credential_type", r.credentialType())
                .required("title", r.title())
                .required("issuing_organization", r.issuingOrganization())
                .maxLength("title", r.title(), TEXT_MAX)
                .maxLength("issuing_organization", r.issuingOrganization(), TEXT_MAX)
                .maxLength("credential_number", r.credentialNumber(), TEXT_MAX);

        CredentialType type = CredentialType.tryFrom(r.credentialType()).orElse(null);
        boolean typeGiven = r.credentialType() != null && !r.credentialType().isBlank();
        v.check(type != null || !typeGiven,
                FieldErrors.invalidFormat("credential_type", "must be one of " + CredentialType.allCodes()));

        LocalDate issued = date(v, "issue_date", r.issueDate());
        LocalDate expires = date(v, "expiry_date", r.expiryDate());
        v.check(issued == null || expires == null || !expires.isBefore(issued),
                FieldErrors.invalidFormat("expiry_date", "must not be before issue_date"));
        v.throwIfInvalid();

        return new CredentialFields(type, r.title().trim(), r.issuingOrganization().trim(),
                blankToNull(r.credentialNumber()), issued, expires);
    }

    private static LocalDate date(Validator v, String field, String text) {
        String value = blankToNull(text);
        if (value == null) return null;
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            v.check(false, FieldErrors.invalidFormat(field, "must be a date as YYYY-MM-DD"));
            return null;
        }
    }
}