package com.nemouw.common.time;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * The one place that turns a wall-clock date and time into an instant. Lebanon changes its clocks at midnight, so
 * one local hour is skipped in spring (00:00 to 00:59) and one repeats in autumn (23:00 to 23:59).
 */
public final class ZoneTimes {
    private ZoneTimes() {
    }

    /**
     * A time inside the spring gap moves forward by the gap length (00:30 becomes 01:30). A time that repeats in
     * autumn means its first occurrence (the earlier offset).
     */
    public static Instant toInstant(LocalDate date, LocalTime time, ZoneId zone) {
        return ZonedDateTime.of(date, time, zone).toInstant();
    }

    /** False when the time falls in a gap, so the wall clock never shows it. */
    public static boolean exists(LocalDate date, LocalTime time, ZoneId zone) {
        return !zone.getRules().getValidOffsets(LocalDateTime.of(date, time)).isEmpty();
    }

    /** True when the time happens twice (autumn overlap). */
    public static boolean repeats(LocalDate date, LocalTime time, ZoneId zone) {
        return zone.getRules().getValidOffsets(LocalDateTime.of(date, time)).size() == 2;
    }
}
