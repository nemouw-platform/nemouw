package com.nemouw.common.enums;

import com.nemouw.common.exception.ValidationException;
import com.nemouw.common.util.Validator;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CodesTest {
    @Test
    void lookupIsExactAndTrimsWhitespace() {
        assertThat(Codes.tryFrom(Gender.class, " male ")).contains(Gender.MALE);
        assertThat(Codes.tryFrom(Gender.class, "Male")).isEmpty();
        assertThat(Codes.tryFrom(Gender.class, null)).isEmpty();
        assertThat(Codes.all(Gender.class)).containsExactly("male", "female");
    }

    @Test
    void nullableColumnsKeepNull() {
        assertThat(Codes.orNull(LocaleCode.class, null)).isNull();
        assertThat(Codes.orNull(LocaleCode.class, "ar")).isEqualTo(LocaleCode.AR);
    }

    @Test
    void bodyKeyIsReadOrReported() {
        assertThat(read(Map.of("gender", "female"))).isEqualTo(Gender.FEMALE);
        Map<String, Object> cleared = new HashMap<>();
        cleared.put("gender", null);
        assertThat(read(cleared)).isNull();
        assertThatThrownBy(() -> read(Map.of("gender", "other"))).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> read(Map.of("gender", 1))).isInstanceOf(ValidationException.class);
    }

    @Test
    void countryCurrencyFallsBackToTheDefault() {
        assertThat(Currency.forCountry("USD")).isEqualTo(Currency.USD);
        assertThat(Currency.forCountry("LBP")).isEqualTo(Currency.USD);
        assertThat(Currency.forCountry(null)).isEqualTo(Currency.USD);
    }

    private static Gender read(Map<String, Object> body) {
        Validator v = new Validator();
        Gender g = Codes.fromBody(body, "gender", Gender.class, v);
        v.throwIfInvalid();
        return g;
    }
}
