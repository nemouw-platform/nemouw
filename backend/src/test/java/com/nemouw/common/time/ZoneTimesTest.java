package com.nemouw.common.time;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.zone.ZoneOffsetTransition;

import static org.assertj.core.api.Assertions.assertThat;

class ZoneTimesTest {
    private static final ZoneId BEIRUT = ZoneId.of("Asia/Beirut");
    private static final LocalDate SPRING_DAY = LocalDate.of(2026, 3, 29);   // 00:00 becomes 01:00
    private static final LocalDate AUTUMN_EVE = LocalDate.of(2026, 10, 24);  // 23:00 to 23:59 happens twice

    @Test
    void winterIsUtcPlus2AndSummerIsUtcPlus3() {
        assertThat(ZoneTimes.toInstant(LocalDate.of(2026, 1, 15), LocalTime.NOON, BEIRUT))
                .isEqualTo(Instant.parse("2026-01-15T10:00:00Z"));
        assertThat(ZoneTimes.toInstant(LocalDate.of(2026, 7, 15), LocalTime.NOON, BEIRUT))
                .isEqualTo(Instant.parse("2026-07-15T09:00:00Z"));
    }

    @Test
    void springGapHourDoesNotExist() {
        assertThat(ZoneTimes.exists(SPRING_DAY.minusDays(1), LocalTime.of(23, 59), BEIRUT)).isTrue();
        assertThat(ZoneTimes.exists(SPRING_DAY, LocalTime.of(0, 0), BEIRUT)).isFalse();
        assertThat(ZoneTimes.exists(SPRING_DAY, LocalTime.of(0, 59), BEIRUT)).isFalse();
        assertThat(ZoneTimes.exists(SPRING_DAY, LocalTime.of(1, 0), BEIRUT)).isTrue();
    }

    @Test
    void aTimeInTheGapMovesForwardByTheGapLength() {
        assertThat(ZoneTimes.toInstant(SPRING_DAY, LocalTime.of(0, 30), BEIRUT))
                .isEqualTo(Instant.parse("2026-03-28T22:30:00Z"));   // 01:30 +03:00
    }

    @Test
    void autumnOverlapHourRepeatsAndMeansTheFirstOccurrence() {
        assertThat(ZoneTimes.repeats(AUTUMN_EVE, LocalTime.of(22, 59), BEIRUT)).isFalse();
        assertThat(ZoneTimes.repeats(AUTUMN_EVE, LocalTime.of(23, 0), BEIRUT)).isTrue();
        assertThat(ZoneTimes.repeats(AUTUMN_EVE, LocalTime.of(23, 59), BEIRUT)).isTrue();
        assertThat(ZoneTimes.repeats(AUTUMN_EVE.plusDays(1), LocalTime.of(0, 0), BEIRUT)).isFalse();

        assertThat(ZoneTimes.toInstant(AUTUMN_EVE, LocalTime.of(23, 30), BEIRUT))
                .isEqualTo(Instant.parse("2026-10-24T20:30:00Z"));   // +03:00, the earlier one
        assertThat(ZoneTimes.toInstant(AUTUMN_EVE.plusDays(1), LocalTime.MIDNIGHT, BEIRUT))
                .isEqualTo(Instant.parse("2026-10-24T22:00:00Z"));   // +02:00, after the repeat
    }

    @Test
    void localDaysAreNot24HoursOnChangeDays() {
        assertThat(dayLength(LocalDate.of(2026, 3, 28))).isEqualTo(Duration.ofHours(24));
        assertThat(dayLength(SPRING_DAY)).isEqualTo(Duration.ofHours(23));
        assertThat(dayLength(AUTUMN_EVE)).isEqualTo(Duration.ofHours(25));
        assertThat(dayLength(AUTUMN_EVE.plusDays(1))).isEqualTo(Duration.ofHours(24));
    }

    /** Fails when the JDK's tzdata is stale or Lebanon changed its schedule: update the JDK, then this test. */
    @Test
    void lebanonScheduleMatchesTzdata() {
        assertTransition("2026-01-01T00:00:00Z", "2026-03-28T22:00:00Z", true);
        assertTransition("2026-04-01T00:00:00Z", "2026-10-24T21:00:00Z", false);
        assertTransition("2027-01-01T00:00:00Z", "2027-03-27T22:00:00Z", true);
        assertTransition("2027-04-01T00:00:00Z", "2027-10-30T21:00:00Z", false);
    }

    private static void assertTransition(String after, String at, boolean gap) {
        ZoneOffsetTransition t = BEIRUT.getRules().nextTransition(Instant.parse(after));

        assertThat(t.getInstant()).isEqualTo(Instant.parse(at));
        assertThat(t.isGap()).isEqualTo(gap);
        assertThat(t.isOverlap()).isEqualTo(!gap);
    }

    private static Duration dayLength(LocalDate day) {
        return Duration.between(day.atStartOfDay(BEIRUT), day.plusDays(1).atStartOfDay(BEIRUT));
    }
}
