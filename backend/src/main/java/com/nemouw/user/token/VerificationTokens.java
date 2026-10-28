package com.nemouw.user.token;

import com.nemouw.common.config.AppProperties;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import java.time.Duration;

import static com.nemouw.user.token.VerificationPurpose.PASSWORD_RESET;
import static com.nemouw.user.token.VerificationPurpose.SIGNUP_OTP;

/**
 * Signup OTPs and password-link tokens: issuing with throttling, HMAC hashing and single-use consumption.
 */
@Service
public class VerificationTokens {
    public record IssuedOtp(String code, OffsetDateTime expiresAt) {
    }

    public record IssuedResetToken(String token, OffsetDateTime expiresAt) {
    }

    public static final int OTP_CODE_DIGITS = 6;

    private static final int OTP_CODE_SPACE = (int) Math.pow(10, OTP_CODE_DIGITS);   // 1,000,000
    private static final String OTP_FORMAT = "%0" + OTP_CODE_DIGITS + "d";
    private static final int LINK_TOKEN_BYTES = 32;

    private final VerificationTokenRepository repo;
    private final TokenProperties props;
    private final SecureRandom random = new SecureRandom();
    private final byte[] key;

    VerificationTokens(VerificationTokenRepository repo, TokenProperties props, AppProperties appProps) {
        this.repo = repo;
        this.props = props;
        this.key = appProps.security().otpHashSecret().getBytes(StandardCharsets.UTF_8);
    }

    public Duration otpTtl() {
        return props.otp().ttl();
    }

    public Duration resetTtl() {
        return props.reset().ttl();
    }

    public Duration inviteTtl() {
        return props.inviteTtl();
    }

    /**
     * Empty means throttled (cooldown or caps).
     */
    public Optional<IssuedOtp> issueSignupOtp(UUID userId, UUID identifierId) {
        String code = String.format(OTP_FORMAT, random.nextInt(OTP_CODE_SPACE));
        return repo.upsertIfAllowed(userId, identifierId, SIGNUP_OTP, hashOtp(userId, code)).map(expiresAt -> new IssuedOtp(code, expiresAt));
    }

    public Optional<IssuedResetToken> issueResetToken(UUID userId, UUID identifierId) {
        String token = newLinkToken();
        return repo.upsertIfAllowed(userId, identifierId, PASSWORD_RESET, hashLinkToken(token)).map(expiresAt -> new IssuedResetToken(token, expiresAt));
    }

    /**
     * A reset token with a 24 h lifetime, so the normal reset endpoint accepts it.
     */
    public Optional<IssuedResetToken> issueInviteToken(UUID userId, UUID identifierId) {
        String token = newLinkToken();
        return repo.upsertIfAllowed(userId, identifierId, PASSWORD_RESET, hashLinkToken(token), props.inviteTtlSeconds()).map(expiresAt -> new IssuedResetToken(token, expiresAt));
    }

    /**
     * 0 means allowed right now. Read-only: a hint, not a guarantee.
     */
    public int secondsUntilAllowed(UUID identifierId, VerificationPurpose purpose) {
        return repo.secondsUntilAllowed(identifierId, purpose);
    }

    /**
     * Spends one attempt. Never throws on a wrong code, so the spent attempt is not rolled back.
     */
    public boolean consumeSignupOtp(UUID userId, String code) {
        var claimed = repo.claimAttempt(userId, SIGNUP_OTP, props.maxOtpAttempts());
        if (claimed.isEmpty() || !sameHash(hashOtp(userId, code), claimed.get().tokenHash())) {
            return false;
        }
        repo.delete(claimed.get().id());
        return true;
    }

    /**
     * Cheap read before Argon2: the email a live reset token belongs to.
     */
    public Optional<String> findLiveResetEmail(String token) {
        return repo.findLiveIdentifier(PASSWORD_RESET, hashLinkToken(token));
    }

    /**
     * Atomic single use: only one concurrent caller gets the row.
     */
    public Optional<ConsumedToken> consumeResetToken(String token) {
        return repo.consumeByHash(PASSWORD_RESET, hashLinkToken(token));
    }

    public void revoke(UUID userId, VerificationPurpose purpose) {
        repo.deleteByUserAndPurpose(userId, purpose);
    }

    private String newLinkToken() {
        byte[] bytes = new byte[LINK_TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashOtp(UUID userId, String code) {
        return hmac(userId + ":" + code);
    }

    private String hashLinkToken(String token) {
        return hmac("reset-token:" + token);
    }

    private static boolean sameHash(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    private String hmac(String input) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(input.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}