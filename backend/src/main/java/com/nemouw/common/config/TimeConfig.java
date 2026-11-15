package com.nemouw.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
class TimeConfig {
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    /** The zone weekly availability is written in. An unknown id stops the application at startup. */
    @Bean
    ZoneId platformZone(@Value("${app.time-zone:Asia/Beirut}") String id) {
        return ZoneId.of(id);
    }
}
