package com.nemouw.common.time;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;

class PlatformTimeTest {
    private static final ZoneId BEIRUT = ZoneId.of("Asia/Beirut");

    @Test
    void nowIsBeirutWallClockInWinterAndSummer() {
        assertThat(at("2026-01-15T10:00:00Z").now()).isEqualTo(LocalDateTime.of(2026, 1, 15, 12, 0));
        assertThat(at("2026-07-15T09:00:00Z").now()).isEqualTo(LocalDateTime.of(2026, 7, 15, 12, 0));
    }

    @Test
    void springForwardSkipsTheMidnightHour() {
        assertThat(at("2026-03-28T21:59:59Z").now()).isEqualTo(LocalDateTime.of(2026, 3, 28, 23, 59, 59));
        assertThat(at("2026-03-28T22:00:00Z").now()).isEqualTo(LocalDateTime.of(2026, 3, 29, 1, 0));
    }

    @Test
    void autumnFallRepeatsTheLastHourOfTheDayWithoutChangingTheDate() {
        assertThat(at("2026-10-24T20:30:00Z").now()).isEqualTo(LocalDateTime.of(2026, 10, 24, 23, 30));
        assertThat(at("2026-10-24T21:00:00Z").now()).isEqualTo(LocalDateTime.of(2026, 10, 24, 23, 0));
        assertThat(at("2026-10-24T21:30:00Z").now()).isEqualTo(LocalDateTime.of(2026, 10, 24, 23, 30));
        assertThat(at("2026-10-24T22:00:00Z").now()).isEqualTo(LocalDateTime.of(2026, 10, 25, 0, 0));
    }

    @Test
    void machineDefaultZoneIsIgnored() {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Kiritimati"));

            assertThat(at("2026-01-15T10:00:00Z").now()).isEqualTo(LocalDateTime.of(2026, 1, 15, 12, 0));
        } finally {
            TimeZone.setDefault(original);
        }
    }

    @Test
    void windowTimesResolveWithTheSamePolicyAsZoneTimes() {
        assertThat(at("2026-03-01T00:00:00Z").toInstant(LocalDate.of(2026, 3, 29), LocalTime.of(0, 30)))
                .isEqualTo(Instant.parse("2026-03-28T22:30:00Z"));
    }

    private static PlatformTime at(String instant) {
        return new PlatformTime(Clock.fixed(Instant.parse(instant), ZoneId.of("UTC")), BEIRUT);
    }
}
