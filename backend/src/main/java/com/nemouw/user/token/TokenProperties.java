package com.nemouw.user.token;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.user.token")
record TokenProperties(
        @Min(1) int maxOtpAttempts,
        @NotNull Duration inviteTtl,
        @Valid @NotNull Throttle otp,
        @Valid @NotNull Throttle reset) {

    record Throttle(
            @NotNull Duration ttl,
            @NotNull Duration cooldown,
            @NotNull Duration window,
            @Min(1) int sendCap,
            @Min(1) int dailyCap) {

        int ttlSeconds() { return (int) ttl.toSeconds(); }
        int cooldownSeconds() { return (int) cooldown.toSeconds(); }
        int windowSeconds() { return (int) window.toSeconds(); }
    }

    Throttle of(VerificationPurpose purpose) {
        return switch (purpose) {
            case SIGNUP_OTP -> otp;
            case PASSWORD_RESET -> reset;
        };
    }

    int inviteTtlSeconds() { return (int) inviteTtl.toSeconds(); }
}