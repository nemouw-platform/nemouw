package com.nemouw.auth.service;

import com.nemouw.auth.repository.VerificationTokenRepository;
import com.nemouw.enums.VerificationPurpose;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/** Issues signup OTPs and reset tokens, and enforces how often they may be issued. */
@Service
public class VerificationTokenIssuer {
    public record IssuedOtp(String code, OffsetDateTime expiresAt) {
    }

    public record IssuedResetToken(String token, OffsetDateTime expiresAt) {
    }

    private final VerificationTokenService otp;
    private final VerificationTokenRepository tokens;

    public VerificationTokenIssuer(VerificationTokenService otp, VerificationTokenRepository tokens) {
        this.otp = otp;
        this.tokens = tokens;
    }

    /** Empty means throttled (cooldown or caps). */
    public Optional<IssuedOtp> issueOtp(UUID userId, UUID identifierId, VerificationPurpose purpose) {
        String code = otp.generateCode();
        return tokens.upsertIfAllowed(userId, identifierId, purpose, otp.hash(userId, code))
                .map(expiresAt -> new IssuedOtp(code, expiresAt));
    }

    public Optional<IssuedResetToken> issueResetToken(UUID userId, UUID identifierId) {
        String token = otp.generateResetToken();
        return tokens.upsertIfAllowed(userId, identifierId, VerificationPurpose.PASSWORD_RESET, otp.hashToken(token))
                .map(expiresAt -> new IssuedResetToken(token, expiresAt));
    }

    /** 0 means allowed right now. Read-only: a hint, not a guarantee. */
    public int secondsUntilAllowed(UUID identifierId, VerificationPurpose purpose) {
        return Math.max(0, tokens.secondsUntilAllowed(identifierId, purpose));
    }
}