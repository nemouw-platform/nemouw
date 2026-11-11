package com.nemouw.common.util;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class TextNormalizerStripTest {

    @Nested
    class HappyPaths {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t\n", "\u00A0", "\u2003", "\u3000", "\u0001", "\u0085", "\u00A0\u2003 \t\u0001"})
        void treatsNullEmptyAndInvisibleOnlyTextAsBlank(String value) {
            assertThat(TextNormalizer.isBlank(value)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"a", " a ", "\u00A0a", "a\u2003", "0"})
        void treatsTextWithAVisibleCharacterAsNotBlank(String value) {
            assertThat(TextNormalizer.isBlank(value)).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {"  Dr  ", "\tDr\n", "\u00A0Dr\u00A0", "\u2003Dr\u2003", "\u0001Dr\u0001", " \u00A0Dr \u2003"})
        void stripsInvisibleCharactersFromBothEnds(String value) {
            assertThat(TextNormalizer.strip(value)).isEqualTo("Dr");
        }

        @Test
        void keepsInvisibleCharactersInsideTheText() {
            assertThat(TextNormalizer.strip(" Dr\u00A0Smith ")).isEqualTo("Dr\u00A0Smith");
        }

        @Test
        void returnsAlreadyCleanTextUnchanged() {
            assertThat(TextNormalizer.strip("Dr Smith")).isEqualTo("Dr Smith");
        }

        @Test
        void turnsBlankIntoNullAndStripsOtherValues() {
            assertThat(TextNormalizer.blankToNull("\u00A0Dr\u00A0")).isEqualTo("Dr");
            assertThat(TextNormalizer.blankToNull("  Dr ")).isEqualTo("Dr");
        }

        @Test
        void normalizesAnEmailByStrippingAndLowercasing() {
            assertThat(TextNormalizer.normalizeEmail("\u00A0Amira.Haddad@ClinicMail.org\u2003"))
                    .isEqualTo("amira.haddad@clinicmail.org");
        }
    }

    @Nested
    class BoundaryValues {

        @Test
        void returnsAnEmptyStringWhenEverythingIsStripped() {
            assertThat(TextNormalizer.strip("\u00A0\u2003\u0001")).isEmpty();
        }

        @Test
        void keepsNullAsNull() {
            assertThat(TextNormalizer.strip(null)).isNull();
            assertThat(TextNormalizer.blankToNull(null)).isNull();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\u00A0", "\u2003"})
        void turnsBlankIntoNullInBlankToNull(String value) {
            assertThat(TextNormalizer.blankToNull(value)).isNull();
        }

        @Test
        void doesNotTreatZeroWidthCharactersAsBlankBecauseArabicTextNeedsThem() {
            assertThat(TextNormalizer.isBlank("\u200B")).isFalse();
            assertThat(TextNormalizer.strip("\u200Bx\u200F")).isEqualTo("\u200Bx\u200F");
        }
    }
}