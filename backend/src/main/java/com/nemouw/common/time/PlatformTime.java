package com.nemouw.common.time;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

/** "Now" and wall-clock conversions in the platform zone. Never reads the machine's default zone. */
@Component
public class PlatformTime {
    private final Clock clock;
    private final ZoneId zone;

    public PlatformTime(Clock clock, ZoneId zone) {
        this.clock = clock;
        this.zone = zone;
    }

    public ZoneId zone() {
        return zone;
    }

    public LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), zone);
    }

    public Instant toInstant(LocalDate date, LocalTime time) {
        return ZoneTimes.toInstant(date, time, zone);
    }
}
