package com.nemouw.specialist.availability;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

/** The API's time-of-day format: strict 24h HH:mm. */
final class ClockTime {
    private static final DateTimeFormatter HH_MM =
            DateTimeFormatter.ofPattern("HH:mm").withResolverStyle(ResolverStyle.STRICT);

    private ClockTime() {
    }

    /** Null when the text is not a valid HH:mm. */
    static LocalTime parse(String text) {
        try {
            return LocalTime.parse(text.trim(), HH_MM);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    static String format(LocalTime time) {
        return HH_MM.format(time);
    }
}