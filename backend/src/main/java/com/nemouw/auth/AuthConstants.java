package com.nemouw.auth;

import com.nemouw.enums.UserRole;

import java.util.EnumSet;
import java.util.Set;

public final class AuthConstants {
    private AuthConstants() {
    }

    // Roles a visitor may pick for themselves at signup.
    public static final Set<UserRole> SELF_REGISTERABLE = EnumSet.of(UserRole.SPECIALIST);

    // ---- Client info ----
    // The User-Agent header can be 8 KB: don't store attacker-sized text.
    public static final int MAX_USER_AGENT_LENGTH = 512;

    // ---- Passwords ----
    public static final int PASSWORD_MIN_LENGTH = 8;
    public static final int PASSWORD_MAX_LENGTH = 64;
    // OWASP minimum for Argon2id: 19 MiB, 2 iterations, 1 lane. Tune upward on your hardware.
    public static final int ARGON2_MEMORY_KB = 19_456;
    public static final int ARGON2_ITERATIONS = 2;
    public static final int ARGON2_PARALLELISM = 1;
    public static final int ARGON2_SALT_LENGTH = 16;
    public static final int ARGON2_HASH_LENGTH = 32;

    // ---- One-time codes ----
    public static final int OTP_CODE_DIGITS = 6;
    public static final int OTP_CODE_SPACE = (int) Math.pow(10, OTP_CODE_DIGITS);   // 1,000,000
    public static final int OTP_TTL_SECONDS = 10 * 60;
    public static final int OTP_MAX_ATTEMPTS = 5;
    public static final int OTP_SEND_COOLDOWN_SECONDS = 60;
    public static final int OTP_SEND_WINDOW_SECONDS = 15 * 60;
    public static final int OTP_SEND_CAP = 3;      // per window
    public static final int OTP_DAILY_CAP = 10;    // per day

    // ---- Password reset ----
    public static final int RESET_TOKEN_BYTES = 32;
    public static final int RESET_TTL_SECONDS = 60 * 60;
    public static final int RESET_COOLDOWN_SECONDS = 60;
    public static final int RESET_WINDOW_SECONDS = 60 * 60;
    public static final int RESET_SEND_CAP = 3;
    public static final int RESET_DAILY_CAP = 10;

    // ---- Password setup (step 3 of signup: verify first, password later) ----
    // Lifetime of the setup token issued after the OTP is verified. The user must choose a password within it.
    public static final int SETUP_TTL_SECONDS = 15 * 60;
    // Scope of that token. Spring Security turns it into the authority SCOPE_password_setup.
    public static final String PASSWORD_SETUP_SCOPE = "password_setup";

    // ---- JWT claim names (shared by the code that signs tokens and the code that checks them) ----
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_SESSION_ID = "sid";
    public static final String CLAIM_SCOPE = "scope";

    // ---- Tokens ----
    // Also what the API returns as expires_in, so changing it changes the contract.
    public static final int ACCESS_TTL_SECONDS = 900;
    public static final int REFRESH_TTL_DAYS = 90;
    // A replay of a just-rotated refresh token inside this window is treated as a tab race, not theft.
    public static final int REFRESH_REUSE_GRACE_SECONDS = 10;
}