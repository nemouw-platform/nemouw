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

class CredentialTypeTest {

    @Nested
    class HappyPaths {

        @Test
        void spellsEveryTypeAsItsLowercaseCodeInDeclarationOrder() {
            assertThat(Arrays.stream(CredentialType.values()).map(CredentialType::code))
                    .containsExactly("degree", "license", "certification", "membership");
        }

        @ParameterizedTest
        @EnumSource(CredentialType.class)
        void findsEveryTypeByItsCode(CredentialType type) {
            assertThat(CredentialType.tryFrom(type.code())).contains(type);
        }

        @ParameterizedTest
        @EnumSource(CredentialType.class)
        void returnsEveryTypeFromFromCode(CredentialType type) {
            assertThat(CredentialType.fromCode(type.code())).isEqualTo(type);
        }

        @ParameterizedTest
        @ValueSource(strings = {" degree ", "\tdegree\n", "degree   "})
        void trimsSurroundingWhitespaceInTryFromAndFromCode(String code) {
            assertThat(CredentialType.tryFrom(code)).contains(CredentialType.DEGREE);
            assertThat(CredentialType.fromCode(code)).isEqualTo(CredentialType.DEGREE);
        }

        @Test
        void listsAllCodesInDeclarationOrderForValidationMessages() {
            assertThat(CredentialType.allCodes()).isEqualTo("degree, license, certification, membership");
        }
    }

    @Nested
    class BoundaryValues {

        @Test
        void hasExactlyFourTypes() {
            assertThat(CredentialType.values()).hasSize(4);
        }
    }

    @Nested
    class ErrorPaths {

        @ParameterizedTest
        @ValueSource(strings = {"Degree", "DEGREE", "dEgReE"})
        void isCaseSensitive(String code) {
            assertThat(CredentialType.tryFrom(code)).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {"diploma", "degrees", "deg ree", "1"})
        void returnsEmptyForUnknownTypes(String code) {
            assertThat(CredentialType.tryFrom(code)).isEmpty();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        void returnsEmptyForNullEmptyAndBlank(String code) {
            assertThat(CredentialType.tryFrom(code)).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {"\u00A0degree", "degree\u00A0", "\u2003degree", "degree\u2003"})
        void doesNotTrimNonBreakingSpacesOrUnicodeSpaces(String code) {
            assertThat(CredentialType.tryFrom(code)).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {"diploma", "Degree", "", "\u00A0degree"})
        void throwsIllegalArgumentExceptionFromFromCodeForAnInvalidCode(String code) {
            assertThatThrownBy(() -> CredentialType.fromCode(code))
                    .isExactlyInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Unknown CredentialType code: " + code);
        }

        @Test
        void throwsIllegalArgumentExceptionNotNullPointerExceptionFromFromCodeForNull() {
            assertThatThrownBy(() -> CredentialType.fromCode(null))
                    .isExactlyInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Unknown CredentialType code: null");
        }
    }
}