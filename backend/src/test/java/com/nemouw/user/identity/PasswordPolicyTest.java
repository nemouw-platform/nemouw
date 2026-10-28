package com.nemouw.user.identity;

import com.nemouw.common.exception.FieldError;
import com.nemouw.common.exception.FieldErrors;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static com.nemouw.user.AuthConstants.PASSWORD_MAX_LENGTH;
import static com.nemouw.user.AuthConstants.PASSWORD_MIN_LENGTH;
import static org.assertj.core.api.Assertions.assertThat;

class PasswordPolicyTest {

    private static final String FIELD = "password";
    private static final String FILLER = "vK9#mQ2$xL7!pR4@";
    private static final String EMOJI = "\uD83D\uDE00";

    private static String filler(int length) {
        return FILLER.repeat(length / FILLER.length() + 1).substring(0, length);
    }

    /** Puts a word in front of a filler that is long enough to pass the length rule on its own. */
    private static String padded(String prefix) {
        return prefix + filler(PASSWORD_MIN_LENGTH);
    }

    private static String strongPassword() {
        return filler(16);
    }

    private static List<FieldError> only(FieldError error) {
        return List.of(error);
    }

    @Nested
    class HappyPaths {

        @Test
        void acceptsAStrongPasswordWithUnrelatedPersonalInfo() {
            List<FieldError> errors = PasswordPolicy.check(FIELD, strongPassword(), "amira.haddad", "clinicmail");

            assertThat(errors).isEmpty();
        }

        @Test
        void acceptsAPassphraseWithoutUppercaseDigitsOrSymbols() {
            List<FieldError> errors = PasswordPolicy.check(FIELD, "mauve otter lantern quartz");

            assertThat(errors).isEmpty();
        }

        @Test
        void acceptsLeadingTrailingAndInternalSpaces() {
            assertThat(PasswordPolicy.check(FIELD, " " + strongPassword() + " ")).isEmpty();
            assertThat(PasswordPolicy.check(FIELD, "mauve otter  lantern quartz")).isEmpty();
        }

        @Test
        void acceptsACommonWordInsideALongerPassphrase() {
            List<FieldError> errors = PasswordPolicy.check(FIELD, "administrator" + filler(PASSWORD_MIN_LENGTH));

            assertThat(errors).isEmpty();
        }

        @Test
        void reportsErrorsUnderTheFieldNameItWasGiven() {
            List<FieldError> errors = PasswordPolicy.check("newPassword", "x");

            assertThat(errors).isEqualTo(only(FieldErrors.tooShort("newPassword", PASSWORD_MIN_LENGTH)));
        }
    }

    @Nested
    class BoundaryValues {

        @Test
        void treatsNullAndEmptyAsNoErrorsBecauseRequiredIsTheCallersJob() {
            assertThat(PasswordPolicy.check(FIELD, null)).isEmpty();
            assertThat(PasswordPolicy.check(FIELD, "")).isEmpty();
        }

        @Test
        void rejectsOneCharacterBelowTheMinimum() {
            List<FieldError> errors = PasswordPolicy.check(FIELD, filler(PASSWORD_MIN_LENGTH - 1));

            assertThat(errors).isEqualTo(only(FieldErrors.tooShort(FIELD, PASSWORD_MIN_LENGTH)));
        }

        @Test
        void acceptsExactlyTheMinimumAndExactlyTheMaximum() {
            assertThat(PasswordPolicy.check(FIELD, filler(PASSWORD_MIN_LENGTH))).isEmpty();
            assertThat(PasswordPolicy.check(FIELD, filler(PASSWORD_MAX_LENGTH))).isEmpty();
        }

        @Test
        void rejectsOneCharacterAboveTheMaximum() {
            List<FieldError> errors = PasswordPolicy.check(FIELD, filler(PASSWORD_MAX_LENGTH + 1));

            assertThat(errors).isEqualTo(only(FieldErrors.tooLong(FIELD, PASSWORD_MAX_LENGTH)));
        }

        @Test
        void countsLengthInCodePointsSoAnEmojiIsOneCharacter() {
            assertThat(PasswordPolicy.check(FIELD, EMOJI.repeat(PASSWORD_MIN_LENGTH - 1)))
                    .isEqualTo(only(FieldErrors.tooShort(FIELD, PASSWORD_MIN_LENGTH)));
            assertThat(PasswordPolicy.check(FIELD, EMOJI.repeat(PASSWORD_MAX_LENGTH))).isEmpty();
            assertThat(PasswordPolicy.check(FIELD, EMOJI.repeat(PASSWORD_MAX_LENGTH + 1)))
                    .isEqualTo(only(FieldErrors.tooLong(FIELD, PASSWORD_MAX_LENGTH)));
        }

        @Test
        void countsSpacesTowardTheMinimumLength() {
            List<FieldError> errors = PasswordPolicy.check(FIELD, filler(PASSWORD_MIN_LENGTH - 1) + " ");

            assertThat(errors).isEmpty();
        }

        @Test
        void skipsTheBlocklistWhenTheLengthIsAlreadyWrong() {
            List<FieldError> errors = PasswordPolicy.check(FIELD, "nemouw");

            assertThat(errors).isEqualTo(only(FieldErrors.tooShort(FIELD, PASSWORD_MIN_LENGTH)));
        }

        @Test
        void doesNotFlagAPasswordMadeOnlyOfSymbols() {
            List<FieldError> errors = PasswordPolicy.check(FIELD, "#".repeat(PASSWORD_MIN_LENGTH));

            assertThat(errors).isEmpty();
        }

        @Test
        void ignoresPersonalInfoOfThreeCharactersButChecksFour() {
            assertThat(PasswordPolicy.check(FIELD, padded("bob"), "bob")).isEmpty();
            assertThat(PasswordPolicy.check(FIELD, padded("anna"), "anna"))
                    .isEqualTo(only(FieldErrors.containsPersonalInfo(FIELD)));
        }

        @Test
        void ignoresNullAndBlankPersonalInfo() {
            assertThat(PasswordPolicy.check(FIELD, strongPassword(), null, "", "   ")).isEmpty();
            assertThat(PasswordPolicy.check(FIELD, strongPassword(), (String[]) null)).isEmpty();
        }
    }

    @Nested
    class ErrorPaths {

        @ParameterizedTest
        @ValueSource(strings = {" ", "\t", "\n"})
        void reportsRequiredForAPasswordOfOnlyWhitespace(String whitespace) {
            List<FieldError> errors = PasswordPolicy.check(FIELD, whitespace.repeat(PASSWORD_MIN_LENGTH));

            assertThat(errors).isEqualTo(only(FieldErrors.required(FIELD)));
        }

        @Test
        void reportsRequiredRatherThanTooLongForAVeryLongWhitespaceOnlyPassword() {
            List<FieldError> errors = PasswordPolicy.check(FIELD, " ".repeat(PASSWORD_MAX_LENGTH * 2));

            assertThat(errors).isEqualTo(only(FieldErrors.required(FIELD)));
        }

        @ParameterizedTest
        @ValueSource(strings = {"administrator", "ADMINISTRATOR", "Administrator123!", "@dm1n1str@t0r"})
        void rejectsCommonPasswordsHiddenByCaseDigitsSymbolsOrLeetSpeak(String password) {
            assertThat(PasswordPolicy.check(FIELD, password)).isEqualTo(only(FieldErrors.tooCommon(FIELD)));
        }

        @ParameterizedTest
        @ValueSource(strings = {" administrator ", "admin istrator", "administrator     "})
        void rejectsCommonPasswordsHiddenBySpaces(String password) {
            assertThat(PasswordPolicy.check(FIELD, password)).isEqualTo(only(FieldErrors.tooCommon(FIELD)));
        }

        @ParameterizedTest
        @ValueSource(strings = {"qwertyuiop!!!", "Superman12345", "Letmein123456", "Trustno1!!!!!"})
        void rejectsWordsFromTheBlocklist(String password) {
            assertThat(PasswordPolicy.check(FIELD, password)).isEqualTo(only(FieldErrors.tooCommon(FIELD)));
        }

        @ParameterizedTest
        @ValueSource(strings = {"password", "baseball", "football", "sunshine", "princess", "changeme", "abcdefgh",
                "superman", "12345678", "specialist", "Patient2026", "Passwd1234"})
        void rejectsTheBuiltInBaselineWords(String password) {
            assertThat(PasswordPolicy.check(FIELD, password)).isEqualTo(only(FieldErrors.tooCommon(FIELD)));
        }

        @ParameterizedTest
        @ValueSource(strings = {"nemouw2026", "NeMoUw", "n3m0uw", "my nemouw login"})
        void rejectsPasswordsContainingTheServiceName(String word) {
            assertThat(PasswordPolicy.check(FIELD, padded(word)))
                    .isEqualTo(only(FieldErrors.containsPersonalInfo(FIELD)));
        }

        @Test
        void rejectsAPasswordContainingTheEmailPartAsWritten() {
            List<FieldError> errors = PasswordPolicy.check(FIELD, padded("john.smith!2026"), "john.smith");

            assertThat(errors).isEqualTo(only(FieldErrors.containsPersonalInfo(FIELD)));
        }

        @ParameterizedTest
        @CsvSource(delimiter = '|', value = {
                "amira.haddad    | amirahaddad2026!!",
                "karim_nassar    | KarimNassar-2026!",
                "layla-khoury    | layla.khoury.2026",
                "omar+newsletter | omarnewsletter#9"
        })
        void rejectsAPasswordContainingTheEmailPartWithItsSeparatorsRemoved(String emailPart, String password) {
            List<FieldError> errors = PasswordPolicy.check(FIELD, password, emailPart);

            assertThat(errors).isEqualTo(only(FieldErrors.containsPersonalInfo(FIELD)));
        }

        @Test
        void rejectsAPasswordEqualToTheEmailPart() {
            List<FieldError> errors = PasswordPolicy.check(FIELD, "zorvaxque2026", "zorvaxque2026");

            assertThat(errors).isEqualTo(only(FieldErrors.containsPersonalInfo(FIELD)));
        }

        @Test
        void rejectsAnEmailPartInTheMiddleOfThePassword() {
            List<FieldError> errors = PasswordPolicy.check(FIELD, "vK9#mQ2$johnsmithxL7!pR4@", "johnsmith");

            assertThat(errors).isEqualTo(only(FieldErrors.containsPersonalInfo(FIELD)));
        }

        @Test
        void matchesPersonalInfoIgnoringCaseAndSurroundingSpaces() {
            List<FieldError> errors = PasswordPolicy.check(FIELD, padded("JOHNSMITH"), "  johnsmith ");

            assertThat(errors).isEqualTo(only(FieldErrors.containsPersonalInfo(FIELD)));
        }

        @Test
        void matchesPersonalInfoThroughSpacesInThePassword() {
            List<FieldError> errors = PasswordPolicy.check(FIELD, padded("john smith"), "johnsmith");

            assertThat(errors).isEqualTo(only(FieldErrors.containsPersonalInfo(FIELD)));
        }

        @Test
        void matchesPersonalInfoThroughLeetSpeak() {
            List<FieldError> errors = PasswordPolicy.check(FIELD, padded("j0hnsm1th"), "johnsmith");

            assertThat(errors).isEqualTo(only(FieldErrors.containsPersonalInfo(FIELD)));
        }

        @Test
        void matchesAComposedPasswordLetterAgainstADecomposedPersonalValue() {
            List<FieldError> errors = PasswordPolicy.check(FIELD, padded("caf\u00E9"), "cafe\u0301");

            assertThat(errors).isEqualTo(only(FieldErrors.containsPersonalInfo(FIELD)));
        }

        @Test
        void matchesADecomposedPasswordLetterAgainstAComposedPersonalValue() {
            List<FieldError> errors = PasswordPolicy.check(FIELD, padded("cafe\u0301"), "caf\u00E9");

            assertThat(errors).isEqualTo(only(FieldErrors.containsPersonalInfo(FIELD)));
        }

        @Test
        void triggersWhenAnyOneOfSeveralPersonalValuesMatches() {
            List<FieldError> errors = PasswordPolicy.check(FIELD, padded("smith"), "john", "smith");

            assertThat(errors).isEqualTo(only(FieldErrors.containsPersonalInfo(FIELD)));
        }

        @Test
        void reportsCommonAndPersonalInfoTogetherInThatOrder() {
            List<FieldError> errors = PasswordPolicy.check(FIELD, "administrator", "administrator");

            assertThat(errors).containsExactly(
                    FieldErrors.tooCommon(FIELD),
                    FieldErrors.containsPersonalInfo(FIELD));
        }
    }
}
