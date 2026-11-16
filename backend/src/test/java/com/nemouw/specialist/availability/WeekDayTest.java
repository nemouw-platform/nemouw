package com.nemouw.specialist.availability;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WeekDayTest {

    @Nested
    class HappyPaths {

        @Test
        void spellsEveryDayAsItsLowercaseEnglishNameStartingOnMonday() {
            assertThat(Arrays.stream(WeekDay.values()).map(WeekDay::code)).containsExactly(
                    "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday");
        }

        @ParameterizedTest
        @EnumSource(WeekDay.class)
        void findsEveryDayByItsCode(WeekDay day) {
            assertThat(WeekDay.tryFrom(day.code())).contains(day);
        }

        @ParameterizedTest
        @EnumSource(WeekDay.class)
        void returnsEveryDayFromFromCode(WeekDay day) {
            assertThat(WeekDay.fromCode(day.code())).isEqualTo(day);
        }

        @ParameterizedTest
        @ValueSource(strings = {" monday ", "\tmonday\n", "monday   "})
        void trimsSurroundingWhitespace(String code) {
            assertThat(WeekDay.tryFrom(code)).contains(WeekDay.MONDAY);
        }

        @Test
        void listsAllCodesInOrderForValidationMessages() {
            assertThat(WeekDay.allCodes())
                    .isEqualTo("monday, tuesday, wednesday, thursday, friday, saturday, sunday");
        }
    }

    @Nested
    class BoundaryValues {

        @Test
        void putsMondayFirstAndSundayLastBecauseListsAreSortedByEnumOrder() {
            assertThat(WeekDay.values()).hasSize(7);
            assertThat(WeekDay.values()[0]).isEqualTo(WeekDay.MONDAY);
            assertThat(WeekDay.values()[6]).isEqualTo(WeekDay.SUNDAY);
            assertThat(WeekDay.MONDAY).isLessThan(WeekDay.SUNDAY);
        }
    }

    @Nested
    class ErrorPaths {

        @ParameterizedTest
        @ValueSource(strings = {"Monday", "MONDAY", "mOnDay", "Sunday", "SUNDAY"})
        void isCaseSensitiveSoOnlyTheLowercaseCodeMatches(String code) {
            assertThat(WeekDay.tryFrom(code)).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {"mon", "funday", "mon day", "mondays", "mon-day", "1", "0"})
        void returnsEmptyForUnknownDays(String code) {
            assertThat(WeekDay.tryFrom(code)).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "\t"})
        void returnsEmptyForEmptyAndBlank(String code) {
            assertThat(WeekDay.tryFrom(code)).isEmpty();
        }

        @ParameterizedTest
        @NullSource
        void returnsEmptyForNull(String code) {
            assertThat(WeekDay.tryFrom(code)).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {"\u00A0monday", "monday\u00A0", "\u2003monday", "monday\u2003"})
        void doesNotTrimNonBreakingSpacesOrUnicodeSpaces(String code) {
            assertThat(WeekDay.tryFrom(code)).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {"funday", "Monday", ""})
        void throwsIllegalArgumentExceptionFromFromCodeForAnInvalidCode(String code) {
            assertThatThrownBy(() -> WeekDay.fromCode(code))
                    .isExactlyInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Unknown WeekDay code: " + code);
        }

        @Test
        void throwsIllegalArgumentExceptionNotNullPointerExceptionFromFromCodeForNull() {
            assertThatThrownBy(() -> WeekDay.fromCode(null))
                    .isExactlyInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Unknown WeekDay code: null");
        }
    }
}