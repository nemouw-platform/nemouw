package com.nemouw.user.identity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.user.password")
record PasswordProperties(@Valid @NotNull Argon2 argon2) {

    record Argon2(
            @Min(1) int memoryKb,
            @Min(1) int iterations,
            @Min(1) int parallelism,
            @Min(8) int saltLength,
            @Min(16) int hashLength) {
    }
}