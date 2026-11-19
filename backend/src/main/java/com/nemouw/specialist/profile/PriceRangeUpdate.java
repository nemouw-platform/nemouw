package com.nemouw.specialist.profile;

import com.nemouw.common.enums.Codes;
import com.nemouw.common.enums.Currency;
import com.nemouw.common.exception.FieldErrors;
import com.nemouw.common.util.Validator;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/** Parses the session price and currency keys of a profile PATCH into column changes. */
final class PriceRangeUpdate {
    private static final BigDecimal MAX_PRICE = new BigDecimal("99999999.99");

    private PriceRangeUpdate() {
    }

    static Map<String, Object> parse(Map<String, Object> body, SpecialistRow cur, Validator v) {
        Map<String, Object> changes = new LinkedHashMap<>();
        parsePrices(body, cur, v, changes);
        parseCurrency(body, v, changes);
        return changes;
    }

    private static void parsePrices(Map<String, Object> body, SpecialistRow cur, Validator v,
                                    Map<String, Object> changes) {
        BigDecimal min = cur.sessionPriceMin();
        BigDecimal max = cur.sessionPriceMax();
        if (body.containsKey("session_price_min")) {
            min = price(body, "session_price_min", v);
            changes.put("session_price_min", min);
        }
        if (body.containsKey("session_price_max")) {
            max = price(body, "session_price_max", v);
            changes.put("session_price_max", max);
        }
        if (max != null && min == null) {
            v.check(false, FieldErrors.invalidFormat("session_price_max", "requires session_price_min"));
        } else if (max != null && max.compareTo(min) < 0) {
            v.check(false, FieldErrors.invalidFormat("session_price_max",
                    "must be greater than or equal to session_price_min"));
        }
    }

    private static void parseCurrency(Map<String, Object> body, Validator v, Map<String, Object> changes) {
        if (!body.containsKey("price_currency")) return;
        Currency currency = Codes.fromBody(body, "price_currency", Currency.class, v);
        if (body.get("price_currency") == null) {
            v.check(false, FieldErrors.invalidFormat("price_currency", "must be one of " + String.join(", ", Codes.all(Currency.class))));
        }
        if (currency != null) changes.put("price_currency", currency.code());
    }

    private static BigDecimal price(Map<String, Object> body, String key, Validator v) {
        Object o = body.get(key);
        if (o == null) return null;
        if (!(o instanceof Number n)) {
            v.check(false, FieldErrors.invalidFormat(key, "must be a number"));
            return null;
        }
        BigDecimal value = new BigDecimal(n.toString());
        boolean ok = value.signum() >= 0 && value.scale() <= 2 && value.compareTo(MAX_PRICE) <= 0;
        v.check(ok, FieldErrors.invalidFormat(key, "must be between 0 and 99999999.99 with at most 2 decimals"));
        return ok ? value : null;
    }
}
