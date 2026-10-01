package com.nemouw;

/**
 * Fixed shared values, not for per-environment settings (those belong in .env)
 */
public final class Constants {
    private Constants() {
    }

    // ---- General ----
    public static final int HOUR_SECONDS = 3600;

    // ---- Passwords ----
    public static final int PASSWORD_MIN_LENGTH = 8;
    public static final int PASSWORD_MAX_BYTES = 72;
    public static final int BCRYPT_COST = 12;

    // ---- One-time codes ----
    public static final int OTP_CODE_DIGITS = 6;
    public static final int OTP_CODE_SPACE = (int) Math.pow(10, OTP_CODE_DIGITS);   // 1,000,000
    public static final int OTP_TTL_SECONDS = 600;
    public static final int OTP_MAX_ATTEMPTS = 5;
    public static final int OTP_HOURLY_CAP = 5;
    public static final int OTP_RESEND_COOLDOWN_SECONDS = 60;

    // ---- Tokens ----
    // Also what the API returns as expires_in, so changing it changes the contract.
    public static final int ACCESS_TTL_SECONDS = 900;
    public static final int REFRESH_TTL_DAYS = 90;
    // A replay of a just-rotated refresh token inside this window is treated as a tab race, not theft.
    public static final int REFRESH_REUSE_GRACE_SECONDS = 10;

    // ---- Secrets ----
    public static final int MIN_SECRET_LENGTH = 32;
}