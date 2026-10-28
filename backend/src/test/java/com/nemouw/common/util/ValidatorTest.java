
package com.nemouw.common.util;

import com.nemouw.common.exception.FieldError;
import com.nemouw.common.exception.FieldErrors;
import com.nemouw.common.exception.ValidationException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

class ValidatorTest {

    private static List<FieldError> errorsOf(Validator validator) {
        Throwable thrown = catchThrowable(validator::throwIfInvalid);

        assertThat(thrown).isInstanceOf(ValidationException.class);

        return ((ValidationException) thrown).fieldErrors();
    }

    private static String emailOfLength(int length) {
        String localPart = "a".repeat(64);
        int domainLength = length - localPart.length() - 1;

        String domain = "b".repeat(domainLength - ".com".length()) + ".com";

        return localPart + "@" + domain;
    }

    @Nested
    class HappyPaths {

        @Test
        void passesWhenEveryFieldIsValid() {
            Validator validator = new Validator()
                    .required("first_name", "Amira")
                    .maxLength("first_name", "Amira", 100)
                    .requiredEmail("email", "amira.haddad@clinicmail.org")
                    .required("password", "mauve otter lantern quartz");

            assertThatCode(validator::throwIfInvalid)
                    .doesNotThrowAnyException();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "amira.haddad@clinicmail.org",
                "layla+news@mail.example.co.uk",
                "omar_k@sub.domain.example.com",
                "  amira.haddad@clinicmail.org  "
        })
        void acceptsRealisticEmailAddresses(String email) {
            assertThatCode(() ->
                    new Validator()
                            .email("email", email)
                            .throwIfInvalid()
            ).doesNotThrowAnyException();
        }

        @Test
        void collectsEveryErrorInTheOrderTheChecksRan() {
            Validator validator = new Validator()
                    .required("first_name", " ")
                    .requiredEmail("email", "amira.haddad")
                    .required("password", null)
                    .maxLength("last_name", "a".repeat(101), 100);

            assertThat(errorsOf(validator)).containsExactly(
                    FieldErrors.required("first_name"),
                    FieldErrors.mustBeEmail("email"),
                    FieldErrors.required("password"),
                    FieldErrors.tooLong("last_name", 100)
            );
        }

        @Test
        void acceptsValidCustomChecks() {
            Validator validator = new Validator()
                    .check(true, FieldErrors.invalidRole("role"))
                    .check(true, FieldErrors.required("email"));

            assertThatCode(validator::throwIfInvalid)
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    class BoundaryValues {

        @Test
        void reportsAFieldAsRequiredOnlyOnceEvenWhenCheckedTwice() {
            Validator validator = new Validator()
                    .required("first_name", null)
                    .required("first_name", " ");

            assertThat(errorsOf(validator))
                    .containsExactly(FieldErrors.required("first_name"));
        }

        @Test
        void countsTheMaximumLengthInCodePointsAfterTrimming() {
            String hundredEmoji = "\uD83D\uDE00".repeat(100);

            assertThatCode(() ->
                    new Validator()
                            .maxLength("name", "  " + hundredEmoji + "  ", 100)
                            .throwIfInvalid()
            ).doesNotThrowAnyException();

            assertThat(errorsOf(
                    new Validator().maxLength(
                            "name",
                            hundredEmoji + "\uD83D\uDE00",
                            100
                    )
            )).containsExactly(FieldErrors.tooLong("name", 100));
        }

        @Test
        void ignoresNullAndBlankValuesInMaxLengthBecauseRequiredIsSeparate() {
            assertThatCode(() ->
                    new Validator()
                            .maxLength("name", null, 3)
                            .maxLength("name", "   ", 3)
                            .throwIfInvalid()
            ).doesNotThrowAnyException();
        }

        @Test
        void acceptsAnEmailOfExactly254CharactersAndRejects255() {
            assertThatCode(() ->
                    new Validator()
                            .email("email", emailOfLength(254))
                            .throwIfInvalid()
            ).doesNotThrowAnyException();

            assertThat(errorsOf(
                    new Validator().email("email", emailOfLength(255))
            )).containsExactly(FieldErrors.tooLong("email", 254));
        }

        @Test
        void skipsEmailFormatValidationForBlankOptionalValues() {
            assertThatCode(() ->
                    new Validator()
                            .email("email", "  ")
                            .throwIfInvalid()
            ).doesNotThrowAnyException();

            assertThat(errorsOf(
                    new Validator().requiredEmail("email", "  ")
            )).containsExactly(FieldErrors.required("email"));
        }

        @Test
        void acceptsANameAtTheExactMaximumLength() {
            String name = "a".repeat(100);

            assertThatCode(() ->
                    new Validator()
                            .maxLength("first_name", name, 100)
                            .throwIfInvalid()
            ).doesNotThrowAnyException();
        }

        @Test
        void rejectsANameOneCharacterOverTheMaximum() {
            String name = "a".repeat(101);

            assertThat(errorsOf(
                    new Validator().maxLength("first_name", name, 100)
            )).containsExactly(FieldErrors.tooLong("first_name", 100));
        }
    }

    @Nested
    class ErrorPaths {

        @ParameterizedTest
        @ValueSource(strings = {
                "amira.haddad",
                "amira@",
                "@clinicmail.org",
                "amira haddad@clinicmail.org",
                "amira@@clinicmail.org",
                "amira@clinicmail",
                "amira@clinic mail.org"
        })
        void rejectsMalformedEmailAddresses(String email) {
            assertThat(errorsOf(
                    new Validator().email("email", email)
            )).containsExactly(FieldErrors.mustBeEmail("email"));
        }

        @Test
        void requiresMissingFields() {
            assertThat(errorsOf(
                    new Validator().required("token", null)
            )).containsExactly(FieldErrors.required("token"));

            assertThat(errorsOf(
                    new Validator().required("token", "")
            )).containsExactly(FieldErrors.required("token"));
        }

        @Test
        void treatsWhitespaceOnlyValuesAsMissing() {
            assertThat(errorsOf(
                    new Validator().required("token", "    ")
            )).containsExactly(FieldErrors.required("token"));
        }

        @Test
        void validatesRequiredEmailWithoutDuplicatingErrors() {
            assertThat(errorsOf(
                    new Validator().requiredEmail("email", null)
            )).containsExactly(FieldErrors.required("email"));

            assertThat(errorsOf(
                    new Validator().requiredEmail("email", "invalid")
            )).containsExactly(FieldErrors.mustBeEmail("email"));
        }

        @Test
        void throwsValidationExceptionWithStatus422AndCollectedErrors() {
            assertThatThrownBy(() ->
                    new Validator()
                            .required("token", null)
                            .throwIfInvalid()
            ).isInstanceOfSatisfying(
                    ValidationException.class,
                    e -> {
                        assertThat(e.status()).isEqualTo(422);
                        assertThat(e.code()).isEqualTo("VALIDATION_ERROR");
                        assertThat(e.fieldErrors()).containsExactly(
                                FieldErrors.required("token")
                        );
                    }
            );
        }

        @Test
        void addsCustomErrorsOnlyWhenConditionsFail() {
            Validator validator = new Validator()
                    .check(true, FieldErrors.invalidRole("role"))
                    .check(false, FieldErrors.invalidFormat(
                            "code",
                            "must be 6 digits"
                    ));

            assertThat(errorsOf(validator)).containsExactly(
                    FieldErrors.invalidFormat("code", "must be 6 digits")
            );
        }

        @Test
        void addAllPreservesTheOrderOfCustomErrors() {
            List<FieldError> errors = List.of(
                    FieldErrors.required("first_name"),
                    FieldErrors.invalidRole("role")
            );

            Validator validator = new Validator().addAll(errors);

            assertThat(errorsOf(validator)).containsExactlyElementsOf(errors);
        }
    }
}
