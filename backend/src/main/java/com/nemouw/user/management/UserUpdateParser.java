package com.nemouw.user.management;

import com.nemouw.common.enums.Codes;
import com.nemouw.common.enums.LocaleCode;
import com.nemouw.common.exception.FieldErrors;
import com.nemouw.common.util.Validator;
import com.nemouw.country.CountryLookup;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.nemouw.common.util.TextNormalizer.strip;
import static com.nemouw.common.util.ValidationLimits.NAME_MAX_LENGTH;

/**
 * Turns a PATCH body into column changes. A key that is absent is left alone; a key sent as null clears the
 * field, except the names, which are mandatory.
 */
@Component
class UserUpdateParser {
    private final CountryLookup countries;

    UserUpdateParser(CountryLookup countries) {
        this.countries = countries;
    }

    Map<String, Object> parse(Map<String, Object> body) {
        Validator v = new Validator();
        Map<String, Object> changes = new LinkedHashMap<>();
        putName(body, "first_name", v, changes);
        putName(body, "last_name", v, changes);
        if (body.containsKey("country_id")) changes.put("country_id", countryId(body, v));
        if (body.containsKey("preferred_language")) changes.put("preferred_language", language(body, v));
        v.throwIfInvalid();
        return changes;
    }

    private static void putName(Map<String, Object> body, String key, Validator v, Map<String, Object> changes) {
        if (!body.containsKey(key)) return;
        Object raw = body.get(key);
        if (raw instanceof String s) {
            v.required(key, s).maxLength(key, s, NAME_MAX_LENGTH);
            changes.put(key, strip(s));
        } else {
            v.check(false, raw == null ? FieldErrors.required(key) : FieldErrors.invalidFormat(key, "must be a string"));
        }
    }

    private UUID countryId(Map<String, Object> body, Validator v) {
        Object raw = body.get("country_id");
        if (raw == null) return null;
        Optional<UUID> id = raw instanceof String s ? parseUuid(s) : Optional.empty();
        v.check(id.isPresent(), FieldErrors.invalidFormat("country_id", "must be a valid UUID"));
        id.ifPresent(value -> v.check(countries.existsById(value),
                FieldErrors.invalidFormat("country_id", "unknown country")));
        return id.orElse(null);
    }

    private static String language(Map<String, Object> body, Validator v) {
        LocaleCode code = Codes.fromBody(body, "preferred_language", LocaleCode.class, v);
        return code == null ? null : code.code();
    }

    private static Optional<UUID> parseUuid(String s) {
        try {
            return Optional.of(UUID.fromString(s.trim()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}