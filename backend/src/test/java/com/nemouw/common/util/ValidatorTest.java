package com.nemouw.common.util;

import com.nemouw.common.exception.FieldError;
import com.nemouw.common.exception.FieldErrors;
import com.nemouw.common.exception.ValidationException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static com.nemouw.support.Fixtures.PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidatorTest {

    private static List<FieldError> errorsOf(Validator validator) {
        return ((ValidationException) org.assertj.core.api.Assertions.catchThrowable(validator::throwIfInvalid)).fieldErrors();
    }

    private static String emailOfLength(int length) {
        return "a".repeat(length - "@example.com".length()) + "@example.com";
    }

    @Nested
    class HappyPaths {

        @Test
        void passesWhenEveryFieldIsValid() {
            Validator validator = new Validator()
                    .required("first_name", "Amira")
                    .maxLength("first_name", "Amira", 100)
                    .requiredEmail("email", "amira.haddad@clinicmail.org")
                    .password("password", PASSWORD, "amira.haddad");

            assertThatCode(validator::throwIfInvalid).doesNotThrowAnyException();
        }

        @ParameterizedTest
        @ValueSource(strings = {"amira.haddad@clinicmail.org", "layla+news@mail.example.co.uk", "omar_k@sub.domain.example.com",
                "  amira.haddad@clinicmail.org  "})
        void acceptsRealisticEmailAddresses(String email) {
            assertThatCode(() -> new Validator().email("email", email).throwIfInvalid()).doesNotThrowAnyException();
        }

        @Test
        void collectsEveryErrorInTheOrderTheChecksRan() {
            Validator validator = new Validator()
                    .required("first_name", " ")
                    .requiredEmail("email", "amira.haddad")
                    .password("password", "short");

            assertThat(errorsOf(validator)).containsExactly(
                    FieldErrors.required("first_name"),
                    FieldErrors.mustBeEmail("email"),
                    FieldErrors.tooShort("password", 8));
        }
    }

    @Nested
    class BoundaryValues {

        @Test
        void reportsAFieldAsRequiredOnlyOnceEvenWhenCheckedTwice() {
            Validator validator = new Validator().required("first_name", null).required("first_name", " ");

            assertThat(errorsOf(validator)).containsExactly(FieldErrors.required("first_name"));
        }

        @Test
        void countsTheMaximumLengthInCodePointsAfterTrimming() {
            String hundredEmoji = "\uD83D\uDE00".repeat(100);

            assertThatCode(() -> new Validator().maxLength("name", "  " + hundredEmoji + "  ", 100).throwIfInvalid())
                    .doesNotThrowAnyException();
            assertThat(errorsOf(new Validator().maxLength("name", hundredEmoji + "\uD83D\uDE00", 100)))
                    .containsExactly(FieldErrors.tooLong("name", 100));
        }

        @Test
        void ignoresNullAndBlankValuesInMaxLengthBecauseRequiredIsASeparateCheck() {
            assertThatCode(() -> new Validator().maxLength("name", null, 3).maxLength("name", "   ", 3).throwIfInvalid())
                    .doesNotThrowAnyException();
        }

        @Test
        void acceptsAnEmailOfExactly254CharactersAndRejectsOneOf255AsTooLong() {
            assertThatCode(() -> new Validator().email("email", emailOfLength(254)).throwIfInvalid())
                    .doesNotThrowAnyException();
            assertThat(errorsOf(new Validator().email("email", emailOfLength(255))))
                    .containsExactly(FieldErrors.tooLong("email", 254));
        }

        @Test
        void skipsTheEmailFormatCheckForABlankValueSoOnlyRequiredEmailComplains() {
            assertThatCode(() -> new Validator().email("email", "  ").throwIfInvalid()).doesNotThrowAnyException();
            assertThat(errorsOf(new Validator().requiredEmail("email", "  ")))
                    .containsExactly(FieldErrors.required("email"));
        }
    }

    @Nested
    class ErrorPaths {

        @ParameterizedTest
        @ValueSource(strings = {"amira.haddad", "amira@", "@clinicmail.org", "amira haddad@clinicmail.org",
                "amira@@clinicmail.org", "amira@clinicmail", "amira@clinic mail.org"})
        void rejectsMalformedEmailAddresses(String email) {
            assertThat(errorsOf(new Validator().email("email", email))).containsExactly(FieldErrors.mustBeEmail("email"));
        }

        @Test
        void treatsANullPasswordAndAnEmptyOneAsRequired() {
            assertThat(errorsOf(new Validator().password("password", null))).containsExactly(FieldErrors.required("password"));
            assertThat(errorsOf(new Validator().password("password", ""))).containsExactly(FieldErrors.required("password"));
        }

        @Test
        void treatsAPasswordOfOnlySpacesAsRequiredRatherThanTooShort() {
            assertThat(errorsOf(new Validator().password("password", "          ")))
                    .containsExactly(FieldErrors.required("password"));
        }

        @Test
        void passesPersonalInfoOnToThePasswordPolicy() {
            assertThat(errorsOf(new Validator().password("password", "amirahaddad2026!!", "amira.haddad")))
                    .containsExactly(FieldErrors.containsPersonalInfo("password"));
        }

        @Test
        void throwsAValidationExceptionThatReportsStatus422AndTheCollectedErrors() {
            assertThatThrownBy(() -> new Validator().required("token", null).throwIfInvalid())
                    .isInstanceOfSatisfying(ValidationException.class, e -> {
                        assertThat(e.status()).isEqualTo(422);
                        assertThat(e.code()).isEqualTo("VALIDATION_ERROR");
                        assertThat(e.fieldErrors()).containsExactly(FieldErrors.required("token"));
                    });
        }

        @Test
        void addsACustomErrorOnlyWhenTheConditionFails() {
            Validator validator = new Validator()
                    .check(true, FieldErrors.invalidRole("role"))
                    .check(false, FieldErrors.invalidFormat("code", "must be 6 digits"));

            assertThat(errorsOf(validator)).containsExactly(FieldErrors.invalidFormat("code", "must be 6 digits"));
        }
    }
}
