package com.nemouw.user.session;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.user.session")
record SessionProperties(
        @NotNull Duration refreshReuseGrace) {
}