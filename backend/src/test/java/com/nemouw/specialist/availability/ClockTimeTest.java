package com.nemouw.specialist.availability;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalTime;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class ClockTimeTest {

    @Nested
    class HappyPaths {

        @ParameterizedTest
        @CsvSource({
                "09:30, 9,  30",
                "00:00, 0,  0",
                "23:59, 23, 59",
                "12:00, 12, 0",
                "00:01, 0,  1",
                "12:59, 12, 59",
                "13:00, 13, 0"
        })
        void parsesAValidTimeOnA24HourClock(String text, int hour, int minute) {
            assertThat(ClockTime.parse(text)).isEqualTo(LocalTime.of(hour, minute));
        }

        @ParameterizedTest
        @ValueSource(strings = {" 09:30 ", "\t09:30\n", "09:30   "})
        void trimsSurroundingWhitespace(String text) {
            assertThat(ClockTime.parse(text)).isEqualTo(LocalTime.of(9, 30));
        }

        @ParameterizedTest
        @CsvSource({
                "9,  5,  09:05",
                "0,  0,  00:00",
                "23, 59, 23:59",
                "12, 0,  12:00"
        })
        void formatsWithZeroPaddedHoursAndMinutes(int hour, int minute, String expected) {
            assertThat(ClockTime.format(LocalTime.of(hour, minute))).isEqualTo(expected);
        }

        @Test
        void formatsAndParsesEveryMinuteOfTheDayBackToTheSameTime() {
            List<LocalTime> mismatches = IntStream.range(0, 24 * 60)
                    .mapToObj(minuteOfDay -> LocalTime.of(minuteOfDay / 60, minuteOfDay % 60))
                    .filter(time -> !time.equals(ClockTime.parse(ClockTime.format(time))))
                    .toList();

            assertThat(mismatches).isEmpty();
        }
    }

    @Nested
    class BoundaryValues {

        @ParameterizedTest
        @ValueSource(strings = {"24:00", "24:01", "12:60", "23:60", "99:99"})
        void rejectsTheFirstValuesPastTheHourAndMinuteLimits(String text) {
            assertThat(ClockTime.parse(text)).isNull();
        }

        @ParameterizedTest
        @ValueSource(strings = {"-1:00", "00:-1", "-01:00", "00:-01", "+9:30"})
        void rejectsSignedValues(String text) {
            assertThat(ClockTime.parse(text)).isNull();
        }

        @Test
        void returnsNullForNullInsteadOfThrowing() {
            assertThat(ClockTime.parse(null)).isNull();
        }

        @Test
        void dropsSecondsAndNanosWhenFormattingWithoutRounding() {
            assertThat(ClockTime.format(LocalTime.of(9, 30, 59, 999_999_999))).isEqualTo("09:30");
        }
    }

    @Nested
    class ErrorPaths {

        @ParameterizedTest
        @ValueSource(strings = {"9:30", "09:5", "9:5", "009:30", "09:300"})
        void rejectsHoursAndMinutesThatAreNotTwoDigits(String text) {
            assertThat(ClockTime.parse(text)).isNull();
        }

        @ParameterizedTest
        @ValueSource(strings = {"09:30:00", "9:30 PM", "09:30 AM", "0930", "T09:30", "09-30", "09.30", "09,30"})
        void rejectsOtherTimeFormats(String text) {
            assertThat(ClockTime.parse(text)).isNull();
        }

        @ParameterizedTest
        @ValueSource(strings = {"ab:cd", "nine:thirty", "09:3o", "::"})
        void rejectsText(String text) {
            assertThat(ClockTime.parse(text)).isNull();
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "\t", "\n"})
        void rejectsEmptyAndBlank(String text) {
            assertThat(ClockTime.parse(text)).isNull();
        }

        @ParameterizedTest
        @ValueSource(strings = {"09: 30", "09 :30", "0 9:30", "09:3 0"})
        void rejectsInternalWhitespace(String text) {
            assertThat(ClockTime.parse(text)).isNull();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "\u0660\u0669:\u0663\u0660",
                "\u06F0\u06F9:\u06F3\u06F0",
                "\uFF10\uFF19:\uFF13\uFF10"
        })
        void rejectsNonAsciiDigits(String text) {
            assertThat(ClockTime.parse(text)).isNull();
        }

        @ParameterizedTest
        @ValueSource(strings = {"\u00A009:30", "09:30\u00A0", "\u200309:30", "09:30\u2003"})
        void doesNotTrimNonBreakingSpacesOrUnicodeSpaces(String text) {
            assertThat(ClockTime.parse(text)).isNull();
        }
    }
}