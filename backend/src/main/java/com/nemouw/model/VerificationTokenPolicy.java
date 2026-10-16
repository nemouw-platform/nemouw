package com.nemouw.model;

import com.nemouw.enums.VerificationPurpose;

import static com.nemouw.Constants.*;

public record VerificationTokenPolicy(int ttlSeconds, int cooldownSeconds, int windowSeconds, int windowCap, int dayCap) {
    private static final VerificationTokenPolicy SIGNUP = new VerificationTokenPolicy(
            OTP_TTL_SECONDS, OTP_SEND_COOLDOWN_SECONDS, OTP_SEND_WINDOW_SECONDS, OTP_SEND_CAP, OTP_DAILY_CAP);
    private static final VerificationTokenPolicy RESET = new VerificationTokenPolicy(
            RESET_TTL_SECONDS, RESET_COOLDOWN_SECONDS, RESET_WINDOW_SECONDS, RESET_SEND_CAP, RESET_DAILY_CAP);

    public static VerificationTokenPolicy of(VerificationPurpose purpose) {
        return switch (purpose) {
            case SIGNUP_OTP -> SIGNUP;
            case PASSWORD_RESET -> RESET;
        };
    }
}