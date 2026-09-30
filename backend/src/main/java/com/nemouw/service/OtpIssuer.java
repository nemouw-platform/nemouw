package com.nemouw.service;

import com.nemouw.enums.VerificationPurpose;
import com.nemouw.repository.VerificationTokenRepository;
import org.jdbi.v3.core.Handle;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Issues signup OTPs and enforces how often they may be issued.
 */
public class OtpIssuer {
    private static final int TTL_SECONDS = 600;       // 10 minutes
    private static final int COOLDOWN_SECONDS = 60;
    private static final int HOURLY_CAP = 5;          // counts the initial signup code too
    private static final int HOUR_SECONDS = 3600;

    public record IssuedOtp(String code, OffsetDateTime expiresAt) {
    }

    private final OtpService otp;
    private final VerificationTokenRepository tokens;

    public OtpIssuer(OtpService otp, VerificationTokenRepository tokens) {
        this.otp = otp;
        this.tokens = tokens;
    }

    /**
     * Cooldown plus hourly cap. Call it while holding the user row lock, otherwise
     * two concurrent requests can both pass the check.
     */
    public boolean isThrottled(Handle h, UUID userId) {
        return tokens.issuedWithin(h, userId, VerificationPurpose.SIGNUP_OTP, COOLDOWN_SECONDS)
                || tokens.countIssuedWithin(h, userId, VerificationPurpose.SIGNUP_OTP, HOUR_SECONDS) >= HOURLY_CAP;
    }

    /**
     * Invalidates any live code and stores a fresh one. Email delivery is the caller's job, after commit.
     */
    public IssuedOtp issue(Handle h, UUID userId, UUID identifierId) {
        String code = otp.generateCode();
        tokens.invalidateActive(h, userId, VerificationPurpose.SIGNUP_OTP);
        OffsetDateTime expiresAt = tokens.insert(h, userId, identifierId, VerificationPurpose.SIGNUP_OTP,
                otp.hash(userId, code), TTL_SECONDS);
        return new IssuedOtp(code, expiresAt);
    }

    public int retryAfterSeconds() {
        return COOLDOWN_SECONDS;
    }
}