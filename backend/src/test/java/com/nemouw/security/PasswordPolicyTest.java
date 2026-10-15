package com.nemouw.security;

import com.nemouw.error.FieldErrors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static com.nemouw.Constants.PASSWORD_MAX_LENGTH;
import static com.nemouw.Constants.PASSWORD_MIN_LENGTH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordPolicyTest {

    private static final String FIELD = "password";
    private static final String FILLER = "vK9#mQ2$xL7!pR4@";
    private static final String EMOJI = "\uD83D\uDE00";

    private static String filler(int length) {
        StringBuilder sb = new StringBuilder();
        while (sb.length() < length)
            sb.append(FILLER);
        sb.setLength(length);
        return sb.toString();
    }

    private static String padded(String prefix) {
        return prefix + filler(PASSWORD_MIN_LENGTH);
    }

    private static String validPassword() {
        return filler(Math.max(PASSWORD_MIN_LENGTH, 16));
    }

    // Length

    @Test
    void nullOrEmptyPasswordHasNoErrors() {
        assertTrue(PasswordPolicy.check(FIELD, null).isEmpty());
        assertTrue(PasswordPolicy.check(FIELD, "").isEmpty());
    }

    @Test
    void rejectsPasswordShorterThanMinimum() {
        var errors = PasswordPolicy.check(FIELD, filler(PASSWORD_MIN_LENGTH - 1));
        assertEquals(List.of(FieldErrors.tooShort(FIELD, PASSWORD_MIN_LENGTH)), errors);
    }

    @Test
    void acceptsPasswordAtMinimumAndMaximumLength() {
        assertTrue(PasswordPolicy.check(FIELD, filler(PASSWORD_MIN_LENGTH)).isEmpty());
        assertTrue(PasswordPolicy.check(FIELD, filler(PASSWORD_MAX_LENGTH)).isEmpty());
    }

    @Test
    void rejectsPasswordLongerThanMaximum() {
        var errors = PasswordPolicy.check(FIELD, filler(PASSWORD_MAX_LENGTH + 1));
        assertEquals(List.of(FieldErrors.tooLong(FIELD, PASSWORD_MAX_LENGTH)), errors);
    }

    @Test
    void lengthIsCountedInCodePoints() {
        var tooShort = PasswordPolicy.check(FIELD, EMOJI.repeat(PASSWORD_MIN_LENGTH - 1));
        assertEquals(List.of(FieldErrors.tooShort(FIELD, PASSWORD_MIN_LENGTH)), tooShort);

        assertTrue(PasswordPolicy.check(FIELD, EMOJI.repeat(PASSWORD_MAX_LENGTH)).isEmpty());
    }

    @Test
    void lengthErrorSkipsOtherChecks() {
        var errors = PasswordPolicy.check(FIELD, "nemouw");
        assertEquals(List.of(FieldErrors.tooShort(FIELD, PASSWORD_MIN_LENGTH)), errors);
    }

    @Test
    void errorsUseTheGivenFieldName() {
        var errors = PasswordPolicy.check("newPassword", "x");
        assertEquals(List.of(FieldErrors.tooShort("newPassword", PASSWORD_MIN_LENGTH)), errors);
    }

    // Blocklist

    @ParameterizedTest
    @ValueSource(strings = {
            "administrator",
            "ADMINISTRATOR",
            "Administrator123!",
            "@dm1n1str@t0r"
    })
    void rejectsCommonPasswordVariants(String password) {
        assertEquals(List.of(FieldErrors.tooCommon(FIELD)), PasswordPolicy.check(FIELD, password));
    }

    @ParameterizedTest
    @ValueSource(strings = { "qwertyuiop!!!", "Superman12345", "Letmein123456", "Trustno1!!!!!" })
    void rejectsBuiltInBlocklistWords(String password) {
        assertEquals(List.of(FieldErrors.tooCommon(FIELD)), PasswordPolicy.check(FIELD, password));
    }

    @Test
    void allowsCommonWordInsideALongerPassphrase() {
        assertTrue(PasswordPolicy.check(FIELD, "administrator" + filler(PASSWORD_MIN_LENGTH)).isEmpty());
    }

    @Test
    void passwordWithoutLettersIsNotFlagged() {
        assertTrue(PasswordPolicy.check(FIELD, "#".repeat(PASSWORD_MIN_LENGTH)).isEmpty());
    }

    // Whitespace

    @Test
    void spacesCountTowardLength() {
        assertTrue(PasswordPolicy.check(FIELD, filler(PASSWORD_MIN_LENGTH - 1) + " ").isEmpty());
    }

    @Test
    void allowsLeadingTrailingAndInternalSpaces() {
        assertTrue(PasswordPolicy.check(FIELD, " " + validPassword() + " ").isEmpty());
        assertTrue(PasswordPolicy.check(FIELD, "mauve otter  lantern quartz").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = { " administrator ", "admin istrator", "administrator     " })
    void spacesDoNotHideCommonPassword(String password) {
        assertEquals(List.of(FieldErrors.tooCommon(FIELD)), PasswordPolicy.check(FIELD, password));
    }

    @Test
    void spacesDoNotHidePersonalInfo() {
        var errors = PasswordPolicy.check(FIELD, padded("john smith"), "johnsmith");
        assertEquals(List.of(FieldErrors.containsPersonalInfo(FIELD)), errors);
    }

    @Test
    void rejectsWhitespaceOnlyPassword() {
        assertFalse(PasswordPolicy.check(FIELD, " ".repeat(PASSWORD_MIN_LENGTH)).isEmpty());
        assertFalse(PasswordPolicy.check(FIELD, "\t".repeat(PASSWORD_MIN_LENGTH)).isEmpty());
    }

    // Service name and personal info

    @ParameterizedTest
    @ValueSource(strings = { "nemouw2026", "NeMoUw", "n3m0uw" })
    void rejectsServiceName(String word) {
        var errors = PasswordPolicy.check(FIELD, padded(word));
        assertEquals(List.of(FieldErrors.containsPersonalInfo(FIELD)), errors);
    }

    @Test
    void rejectsPasswordContainingEmailPart() {
        var errors = PasswordPolicy.check(FIELD, padded("john.smith!2026"), "john.smith");
        assertEquals(List.of(FieldErrors.containsPersonalInfo(FIELD)), errors);
    }

    @Test
    void personalInfoMatchIgnoresCaseAndSurroundingSpaces() {
        var errors = PasswordPolicy.check(FIELD, padded("JOHNSMITH"), "  johnsmith ");
        assertEquals(List.of(FieldErrors.containsPersonalInfo(FIELD)), errors);
    }

    @Test
    void rejectsPasswordEqualToEmailPart() {
        var errors = PasswordPolicy.check(FIELD, "zorvaxque2026", "zorvaxque2026");
        assertEquals(List.of(FieldErrors.containsPersonalInfo(FIELD)), errors);
    }

    @Test
    void rejectsEmailPartInTheMiddleOfPassword() {
        var errors = PasswordPolicy.check(FIELD, "vK9#mQ2$johnsmithxL7!pR4@", "johnsmith");
        assertEquals(List.of(FieldErrors.containsPersonalInfo(FIELD)), errors);
    }

    @Test
    void anyOfSeveralPersonalValuesTriggersTheError() {
        var errors = PasswordPolicy.check(FIELD, padded("smith"), "john", "smith");
        assertEquals(List.of(FieldErrors.containsPersonalInfo(FIELD)), errors);
    }

    @Test
    void unrelatedPersonalInfoDoesNotTrigger() {
        assertTrue(PasswordPolicy.check(FIELD, validPassword(), "john.smith", "example").isEmpty());
    }

    @Test
    void personalInfoMatchSeesThroughLeetSpeak() {
        var errors = PasswordPolicy.check(FIELD, padded("j0hnsm1th"), "johnsmith");
        assertEquals(List.of(FieldErrors.containsPersonalInfo(FIELD)), errors);
    }

    @Test
    void personalInfoMatchUsesNfcNormalization() {
        var errors = PasswordPolicy.check(FIELD, padded("cafe\u0301"), "caf\u00E9");
        assertEquals(List.of(FieldErrors.containsPersonalInfo(FIELD)), errors);
    }

    @Test
    void personalInfoOfFourCharactersIsChecked() {
        var errors = PasswordPolicy.check(FIELD, padded("anna"), "anna");
        assertEquals(List.of(FieldErrors.containsPersonalInfo(FIELD)), errors);
    }

    @Test
    void personalInfoShorterThanFourCharactersIsIgnored() {
        assertTrue(PasswordPolicy.check(FIELD, padded("bob"), "bob").isEmpty());
    }

    @Test
    void nullAndBlankPersonalInfoAreIgnored() {
        assertTrue(PasswordPolicy.check(FIELD, validPassword(), null, "", "   ").isEmpty());
        assertTrue(PasswordPolicy.check(FIELD, validPassword(), (String[]) null).isEmpty());
    }

    // Combined

    @Test
    void reportsCommonAndPersonalInfoTogether() {
        var errors = PasswordPolicy.check(FIELD, "administrator", "administrator");
        assertEquals(List.of(
                FieldErrors.tooCommon(FIELD),
                FieldErrors.containsPersonalInfo(FIELD)), errors);
    }

    @Test
    void acceptsStrongPassword() {
        assertTrue(PasswordPolicy.check(FIELD, validPassword(), "john.smith").isEmpty());
    }

    @Test
    void doesNotRequireUppercaseDigitsOrSymbols() {
        assertTrue(PasswordPolicy.check(FIELD, "mauve otter lantern quartz").isEmpty());
    }
}