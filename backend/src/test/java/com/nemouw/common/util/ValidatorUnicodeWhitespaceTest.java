package com.nemouw.common.util;

import com.nemouw.common.exception.FieldError;
import com.nemouw.common.exception.FieldErrors;
import com.nemouw.common.exception.ValidationException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class ValidatorUnicodeWhitespaceTest {

    private static List<FieldError> run(Consumer<Validator> rules) {
        Throwable thrown = catchThrowable(() -> {
            Validator validator = new Validator();
            rules.accept(validator);
            validator.throwIfInvalid();
        });
        if (thrown == null) return List.of();
        assertThat(thrown).isInstanceOf(ValidationException.class);
        return ((ValidationException) thrown).fieldErrors();
    }

    @Nested
    class HappyPaths {

        @Test
        void acceptsTextWithAVisibleCharacter() {
            assertThat(run(v -> v.required("title", "\u00A0Dr\u00A0"))).isEmpty();
        }

        @Test
        void ignoresInvisibleOnlyValuesInLengthChecksJustLikeBlankOnes() {
            assertThat(run(v -> v.maxLength("title", "\u00A0".repeat(200), 10))).isEmpty();
        }

        @Test
        void doesNotCountInvisibleCharactersAtTheEndsTowardTheMaximum() {
            assertThat(run(v -> v.maxLength("title", "\u00A0" + "a".repeat(10) + "\u2003", 10))).isEmpty();
        }

        @Test
        void acceptsAnEmailWithInvisibleCharactersAroundIt() {
            assertThat(run(v -> v.requiredEmail("email", "\u00A0amira@clinicmail.org\u2003"))).isEmpty();
        }
    }

    @Nested
    class ErrorPaths {

        @ParameterizedTest
        @ValueSource(strings = {"\u00A0", "\u2003", "\u0001", "\u00A0\u2003 \t"})
        void reportsRequiredForInvisibleOnlyValues(String value) {
            assertThat(run(v -> v.required("title", value))).containsExactly(FieldErrors.required("title"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"\u00A0", "\u2003", "\u0001"})
        void reportsRequiredForAnInvisibleOnlyEmail(String value) {
            assertThat(run(v -> v.requiredEmail("email", value))).containsExactly(FieldErrors.required("email"));
        }

        @Test
        void countsVisibleCharactersAfterStrippingTheEnds() {
            assertThat(run(v -> v.maxLength("title", "\u00A0" + "a".repeat(11) + "\u2003", 10)))
                    .containsExactly(FieldErrors.tooLong("title", 10));
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "amira@clinic\u00A0mail.org",
                "ami\u00A0ra@clinicmail.org",
                "amira@clinicmail.\u2003org",
                "amira@clinic\u0001mail.org"
        })
        void rejectsAnInvisibleCharacterInsideAnEmail(String email) {
            assertThat(run(v -> v.requiredEmail("email", email))).containsExactly(FieldErrors.mustBeEmail("email"));
        }
    }

    @Nested
    class BoundaryValues {

        @Test
        void staysStrictForFormatCheckedCodesSoANonBreakingSpaceIsRejected() {
            assertThat(run(v -> v.isoCode("iso_code", "\u00A0US")))
                    .containsExactly(FieldErrors.invalidFormat("iso_code", "must be 2 letters"));
            assertThat(run(v -> v.isoCode("iso_code", "\u00A0")))
                    .containsExactly(FieldErrors.invalidFormat("iso_code", "must be 2 letters"));
        }
    }
}