package com.nemouw.specialist.credential;

import com.nemouw.common.exception.FieldError;
import com.nemouw.common.exception.FieldErrors;
import com.nemouw.common.exception.ValidationException;
import com.nemouw.specialist.credential.CredentialDtos.CredentialWriteRequest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.List;

import static com.nemouw.common.util.ValidationLimits.TEXT_MAX;
import static org.assertj.core.api.Assertions.*;

class CredentialFieldsValidatorTest {

    private static final String TYPE = "degree";
    private static final String TITLE = "BSc Physiotherapy";
    private static final String ORG = "Lebanese University";
    private static final String EMOJI = "\uD83D\uDE00";

    private static CredentialWriteRequest request(String type, String title, String org, String number,
                                                  String issue, String expiry) {
        return new CredentialWriteRequest(type, title, org, number, issue, expiry);
    }

    private static CredentialWriteRequest withDates(String issue, String expiry) {
        return request(TYPE, TITLE, ORG, null, issue, expiry);
    }

    private static CredentialWriteRequest withField(String field, String value) {
        return switch (field) {
            case "title" -> request(TYPE, value, ORG, null, null, null);
            case "issuing_organization" -> request(TYPE, TITLE, value, null, null, null);
            case "credential_number" -> request(TYPE, TITLE, ORG, value, null, null);
            default -> throw new IllegalArgumentException(field);
        };
    }

    private static List<FieldError> errorsOf(CredentialWriteRequest request) {
        Throwable thrown = catchThrowable(() -> CredentialFieldsValidator.validated(request));
        assertThat(thrown).isInstanceOf(ValidationException.class);
        return ((ValidationException) thrown).fieldErrors();
    }

    private static FieldError invalidType() {
        return FieldErrors.invalidFormat("credential_type", "must be one of " + CredentialType.allCodes());
    }

    private static FieldError invalidDate(String field) {
        return FieldErrors.invalidFormat(field, "must be a date as YYYY-MM-DD");
    }

    private static FieldError expiryBeforeIssue() {
        return FieldErrors.invalidFormat("expiry_date", "must not be before issue_date");
    }

    @Nested
    class HappyPaths {

        @Test
        void returnsTrimmedAndParsedFieldsForAFullRequest() {
            CredentialFields fields = CredentialFieldsValidator.validated(request(
                    "degree", "  BSc Physiotherapy  ", "\tLebanese University\n", "  LU-2014-0382 ",
                    " 2010-09-01 ", "2014-06-30"));

            assertThat(fields).isEqualTo(new CredentialFields(CredentialType.DEGREE, "BSc Physiotherapy",
                    "Lebanese University", "LU-2014-0382", LocalDate.of(2010, 9, 1), LocalDate.of(2014, 6, 30)));
        }

        @Test
        void acceptsOnlyTheRequiredFieldsAndLeavesTheOptionalOnesNull() {
            CredentialFields fields = CredentialFieldsValidator.validated(request(TYPE, TITLE, ORG, null, null, null));

            assertThat(fields.credentialNumber()).isNull();
            assertThat(fields.issueDate()).isNull();
            assertThat(fields.expiryDate()).isNull();
        }

        @Test
        void turnsBlankOptionalFieldsIntoNull() {
            CredentialFields fields = CredentialFieldsValidator.validated(request(TYPE, TITLE, ORG, "   ", "", " "));

            assertThat(fields.credentialNumber()).isNull();
            assertThat(fields.issueDate()).isNull();
            assertThat(fields.expiryDate()).isNull();
        }

        @ParameterizedTest
        @EnumSource(CredentialType.class)
        void acceptsEveryCredentialType(CredentialType type) {
            CredentialFields fields = CredentialFieldsValidator.validated(
                    request(type.code(), TITLE, ORG, null, null, null));

            assertThat(fields.type()).isEqualTo(type);
        }

        @Test
        void trimsTheType() {
            CredentialFields fields = CredentialFieldsValidator.validated(
                    request(" degree ", TITLE, ORG, null, null, null));

            assertThat(fields.type()).isEqualTo(CredentialType.DEGREE);
        }

        @Test
        void acceptsAnExpiryOnTheSameDayAsTheIssue() {
            CredentialFields fields = CredentialFieldsValidator.validated(withDates("2026-01-31", "2026-01-31"));

            assertThat(fields.expiryDate()).isEqualTo(fields.issueDate());
        }

        @Test
        void acceptsEitherDateOnItsOwn() {
            assertThat(CredentialFieldsValidator.validated(withDates("2026-01-31", null)).issueDate())
                    .isEqualTo(LocalDate.of(2026, 1, 31));
            assertThat(CredentialFieldsValidator.validated(withDates(null, "2026-01-31")).expiryDate())
                    .isEqualTo(LocalDate.of(2026, 1, 31));
        }

        @Test
        void acceptsALeapDay() {
            CredentialFields fields = CredentialFieldsValidator.validated(withDates("2024-02-29", null));

            assertThat(fields.issueDate()).isEqualTo(LocalDate.of(2024, 2, 29));
        }
    }

    @Nested
    class BoundaryValues {

        @ParameterizedTest
        @ValueSource(strings = {"title", "issuing_organization", "credential_number"})
        void acceptsExactlyTheMaximumLength(String field) {
            assertThatCode(() -> CredentialFieldsValidator.validated(withField(field, "a".repeat(TEXT_MAX))))
                    .doesNotThrowAnyException();
        }

        @ParameterizedTest
        @ValueSource(strings = {"title", "issuing_organization", "credential_number"})
        void rejectsOneCharacterAboveTheMaximumUnderTheRightFieldName(String field) {
            assertThat(errorsOf(withField(field, "a".repeat(TEXT_MAX + 1))))
                    .containsExactly(FieldErrors.tooLong(field, TEXT_MAX));
        }

        @Test
        void doesNotCountSurroundingWhitespaceTowardTheMaximum() {
            CredentialFields fields = CredentialFieldsValidator.validated(
                    withField("title", "  " + "a".repeat(TEXT_MAX) + "\t"));

            assertThat(fields.title()).isEqualTo("a".repeat(TEXT_MAX));
        }

        @Test
        void countsCodePointsSoAnEmojiIsOneCharacter() {
            assertThatCode(() -> CredentialFieldsValidator.validated(withField("title", EMOJI.repeat(TEXT_MAX))))
                    .doesNotThrowAnyException();
            assertThat(errorsOf(withField("title", EMOJI.repeat(TEXT_MAX + 1))))
                    .containsExactly(FieldErrors.tooLong("title", TEXT_MAX));
        }

        @Test
        void acceptsOneCharacterLessThanTheMaximumPlusATrailingEmoji() {
            String title = "a".repeat(TEXT_MAX - 1) + EMOJI;

            assertThat(title.length()).isEqualTo(TEXT_MAX + 1);
            assertThatCode(() -> CredentialFieldsValidator.validated(withField("title", title)))
                    .doesNotThrowAnyException();
        }

        @Test
        void rejectsTheMaximumPlusATrailingEmoji() {
            assertThat(errorsOf(withField("title", "a".repeat(TEXT_MAX) + EMOJI)))
                    .containsExactly(FieldErrors.tooLong("title", TEXT_MAX));
        }
    }

    @Nested
    class ErrorPaths {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        void reportsRequiredNotInvalidFormatForAMissingOrBlankType(String type) {
            assertThat(errorsOf(request(type, TITLE, ORG, null, null, null)))
                    .containsExactly(FieldErrors.required("credential_type"));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        void requiresATitle(String title) {
            assertThat(errorsOf(request(TYPE, title, ORG, null, null, null)))
                    .containsExactly(FieldErrors.required("title"));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        void requiresAnIssuingOrganization(String org) {
            assertThat(errorsOf(request(TYPE, TITLE, org, null, null, null)))
                    .containsExactly(FieldErrors.required("issuing_organization"));
        }

        @Test
        void reportsEveryMissingRequiredFieldForAnAllNullRequestWithoutThrowingANullPointerException() {
            assertThat(errorsOf(request(null, null, null, null, null, null))).containsExactlyInAnyOrder(
                    FieldErrors.required("credential_type"),
                    FieldErrors.required("title"),
                    FieldErrors.required("issuing_organization"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"diploma", "degrees", "Degree", "DEGREE", "deg ree"})
        void rejectsAnUnknownOrDifferentlyCasedType(String type) {
            assertThat(errorsOf(request(type, TITLE, ORG, null, null, null)))
                    .containsExactly(invalidType());
        }

        @ParameterizedTest
        @ValueSource(strings = {"2026/01/31", "31-01-2026", "2026-1-3", "2026-01-31T10:00", "20260131", "tomorrow"})
        void rejectsDatesThatAreNotYyyyMmDd(String date) {
            assertThat(errorsOf(withDates(date, null))).containsExactly(invalidDate("issue_date"));
            assertThat(errorsOf(withDates(null, date))).containsExactly(invalidDate("expiry_date"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"2026-02-30", "2025-02-29", "2026-13-01", "2026-00-10", "2026-04-31"})
        void rejectsDatesThatDoNotExist(String date) {
            assertThat(errorsOf(withDates(date, null))).containsExactly(invalidDate("issue_date"));
            assertThat(errorsOf(withDates(null, date))).containsExactly(invalidDate("expiry_date"));
        }

        @Test
        void rejectsNonAsciiDigitsInADate() {
            String arabicIndic = "\u0662\u0660\u0662\u0666-\u0660\u0661-\u0663\u0661";

            assertThat(errorsOf(withDates(arabicIndic, null))).containsExactly(invalidDate("issue_date"));
        }

        @Test
        void rejectsAnExpiryBeforeTheIssue() {
            assertThat(errorsOf(withDates("2026-01-31", "2026-01-30"))).containsExactly(expiryBeforeIssue());
        }

        @Test
        void skipsTheOrderCheckWhenADateCannotBeParsed() {
            assertThat(errorsOf(withDates("not-a-date", "2026-01-01"))).containsExactly(invalidDate("issue_date"));
            assertThat(errorsOf(withDates("2026-01-31", "not-a-date"))).containsExactly(invalidDate("expiry_date"));
        }

        @Test
        void reportsErrorsFromDifferentFieldsTogether() {
            CredentialWriteRequest request = request("Degree", "   ", "o".repeat(TEXT_MAX + 1),
                    "n".repeat(TEXT_MAX + 1), "bad", "2026-02-30");

            assertThat(errorsOf(request)).containsExactlyInAnyOrder(
                    invalidType(),
                    FieldErrors.required("title"),
                    FieldErrors.tooLong("issuing_organization", TEXT_MAX),
                    FieldErrors.tooLong("credential_number", TEXT_MAX),
                    invalidDate("issue_date"),
                    invalidDate("expiry_date"));
        }
    }

    @Nested
    class UnicodeWhitespace {

        @ParameterizedTest
        @ValueSource(strings = {"\u00A0", "\u2003", "\u0001", "\u00A0\u2003 \t"})
        void treatsATitleOfOnlyInvisibleCharactersAsMissing(String title) {
            assertThat(errorsOf(withField("title", title))).containsExactly(FieldErrors.required("title"));
        }

        @Test
        void reportsRequiredNotInvalidFormatForATypeOfOnlyANonBreakingSpace() {
            assertThat(errorsOf(request("\u00A0", TITLE, ORG, null, null, null)))
                    .containsExactly(FieldErrors.required("credential_type"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"\u00A0Dr\u00A0", "\u2003Dr\u2003", "\u0001Dr\u0001", " \u00A0Dr \u2003"})
        void stripsInvisibleCharactersAroundATitle(String title) {
            assertThat(CredentialFieldsValidator.validated(withField("title", title)).title()).isEqualTo("Dr");
        }

        @Test
        void keepsAnInvisibleCharacterInsideATitle() {
            assertThat(CredentialFieldsValidator.validated(withField("title", "Dr\u00A0Smith")).title())
                    .isEqualTo("Dr\u00A0Smith");
        }

        @Test
        void turnsACredentialNumberOfOnlyANonBreakingSpaceIntoNull() {
            assertThat(CredentialFieldsValidator.validated(withField("credential_number", "\u00A0"))
                    .credentialNumber()).isNull();
        }

        @Test
        void treatsADateOfOnlyANonBreakingSpaceAsAbsent() {
            assertThat(CredentialFieldsValidator.validated(withDates("\u00A0", null)).issueDate()).isNull();
        }
    }
}