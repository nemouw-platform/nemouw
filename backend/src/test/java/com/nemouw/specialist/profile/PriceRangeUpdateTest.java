package com.nemouw.specialist.profile;

import com.nemouw.common.exception.ValidationException;
import com.nemouw.common.util.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PriceRangeUpdateTest {
    @Test
    void pricesAreStoredAsDecimals() {
        assertThat(parse(Map.of("session_price_min", 30, "session_price_max", 50.5), row(null, null)))
                .containsEntry("session_price_min", new BigDecimal("30"))
                .containsEntry("session_price_max", new BigDecimal("50.5"));
    }

    @Test
    void maxMustNotBeBelowTheStoredMin() {
        assertInvalid(Map.of("session_price_max", 10), row(new BigDecimal("20"), null));
    }

    @Test
    void maxNeedsAMin() {
        assertInvalid(Map.of("session_price_max", 10), row(null, null));
    }

    @Test
    void clearingMinWhileMaxStaysIsRejected() {
        Map<String, Object> body = new HashMap<>();
        body.put("session_price_min", null);

        assertInvalid(body, row(new BigDecimal("5"), new BigDecimal("9")));
    }

    @Test
    void clearingBothIsAllowed() {
        Map<String, Object> body = new HashMap<>();
        body.put("session_price_min", null);
        body.put("session_price_max", null);

        assertThat(parse(body, row(new BigDecimal("5"), new BigDecimal("9"))))
                .containsEntry("session_price_min", null)
                .containsEntry("session_price_max", null);
    }

    @Test
    void rejectsNegativeAndOverPreciseAmounts() {
        assertInvalid(Map.of("session_price_min", -1), row(null, null));
        assertInvalid(Map.of("session_price_min", 10.123), row(null, null));
    }

    @Test
    void currencyIsThreeUppercaseLetters() {
        assertThat(parse(Map.of("price_currency", "LBP"), row(null, null))).containsEntry("price_currency", "LBP");
        assertInvalid(Map.of("price_currency", "usd"), row(null, null));
    }

    private static Map<String, Object> parse(Map<String, Object> body, SpecialistRow cur) {
        Validator v = new Validator();
        Map<String, Object> changes = PriceRangeUpdate.parse(body, cur, v);
        v.throwIfInvalid();
        return changes;
    }

    private static void assertInvalid(Map<String, Object> body, SpecialistRow cur) {
        assertThatThrownBy(() -> parse(body, cur)).isInstanceOf(ValidationException.class);
    }

    private static SpecialistRow row(BigDecimal min, BigDecimal max) {
        return new SpecialistRow(null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, "pending", null, null, "active", min, max, "USD", null, null);
    }
}
