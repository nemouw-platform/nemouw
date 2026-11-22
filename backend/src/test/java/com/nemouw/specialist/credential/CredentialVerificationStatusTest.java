package com.nemouw.specialist.credential;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CredentialVerificationStatusTest {

    @Nested
    class HappyPaths {

        @Test
        void spellsEveryStatusAsItsLowercaseCodeInDeclarationOrder() {
            assertThat(Arrays.stream(CredentialVerificationStatus.values()).map(CredentialVerificationStatus::code))
                    .containsExactly("pending", "verified", "rejected");
        }

        @ParameterizedTest
        @EnumSource(CredentialVerificationStatus.class)
        void findsEveryStatusByItsCode(CredentialVerificationStatus status) {
            assertThat(CredentialVerificationStatus.tryFrom(status.code())).contains(status);
        }

        @ParameterizedTest
        @EnumSource(CredentialVerificationStatus.class)
        void returnsEveryStatusFromFromCode(CredentialVerificationStatus status) {
            assertThat(CredentialVerificationStatus.fromCode(status.code())).isEqualTo(status);
        }

        @ParameterizedTest
        @ValueSource(strings = {" verified ", "\tverified\n", "verified   "})
        void trimsSurroundingWhitespaceInTryFromAndFromCode(String code) {
            assertThat(CredentialVerificationStatus.tryFrom(code)).contains(CredentialVerificationStatus.VERIFIED);
            assertThat(CredentialVerificationStatus.fromCode(code)).isEqualTo(CredentialVerificationStatus.VERIFIED);
        }

        @Test
        void listsAllCodesInDeclarationOrderForValidationMessages() {
            assertThat(CredentialVerificationStatus.allCodes()).isEqualTo("pending, verified, rejected");
        }
    }

    @Nested
    class BoundaryValues {

        @Test
        void hasExactlyThreeStatuses() {
            assertThat(CredentialVerificationStatus.values()).hasSize(3);
        }

        @Test
        void usesVerifiedWhereTheSpecialistStatusUsesActive() {
            assertThat(CredentialVerificationStatus.tryFrom("active")).isEmpty();
        }
    }

    @Nested
    class ErrorPaths {

        @ParameterizedTest
        @ValueSource(strings = {"Pending", "VERIFIED", "rEjEcTeD"})
        void isCaseSensitive(String code) {
            assertThat(CredentialVerificationStatus.tryFrom(code)).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {"approved", "verify", "pend ing", "1"})
        void returnsEmptyForUnknownStatuses(String code) {
            assertThat(CredentialVerificationStatus.tryFrom(code)).isEmpty();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        void returnsEmptyForNullEmptyAndBlank(String code) {
            assertThat(CredentialVerificationStatus.tryFrom(code)).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {"\u00A0verified", "verified\u00A0", "\u2003verified", "verified\u2003"})
        void doesNotTrimNonBreakingSpacesOrUnicodeSpaces(String code) {
            assertThat(CredentialVerificationStatus.tryFrom(code)).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {"approved", "Verified", "", "\u00A0verified"})
        void throwsIllegalArgumentExceptionFromFromCodeForAnInvalidCode(String code) {
            assertThatThrownBy(() -> CredentialVerificationStatus.fromCode(code))
                    .isExactlyInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Unknown CredentialVerificationStatus code: " + code);
        }

        @Test
        void throwsIllegalArgumentExceptionNotNullPointerExceptionFromFromCodeForNull() {
            assertThatThrownBy(() -> CredentialVerificationStatus.fromCode(null))
                    .isExactlyInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Unknown CredentialVerificationStatus code: null");
        }
    }
}