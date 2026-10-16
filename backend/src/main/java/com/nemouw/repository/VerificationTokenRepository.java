package com.nemouw.repository;

import com.nemouw.enums.VerificationPurpose;
import com.nemouw.model.VerificationTokenPolicy;
import org.jdbi.v3.core.Handle;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public class VerificationTokenRepository {

    public record ClaimedToken(UUID id, String tokenHash) {
    }

    public record ConsumedToken(UUID id, UUID userId, UUID identifierId) {
    }

    /**
     * Creates or replaces the single token row for (identifier, purpose), only if cooldown,
     * window cap and daily cap allow it. Empty result means throttled.
     */
    public Optional<OffsetDateTime> upsertIfAllowed(Handle h, UUID userId, UUID identifierId,
                                                    VerificationPurpose purpose, String tokenHash) {
        VerificationTokenPolicy p = VerificationTokenPolicy.of(purpose);
        return h.createQuery("""
                        INSERT INTO user_verification_tokens AS t
                            (user_id, identifier_id, purpose, token_hash, expires_at)
                        VALUES (:userId, :identifierId, :purpose, :hash, now() + make_interval(secs => :ttl))
                        ON CONFLICT (identifier_id, purpose) DO UPDATE SET
                            token_hash        = EXCLUDED.token_hash,
                            expires_at        = EXCLUDED.expires_at,
                            failed_attempts   = 0,
                            last_sent_at      = now(),
                            window_started_at = CASE WHEN t.window_started_at <= now() - make_interval(secs => :windowSecs)
                                                     THEN now() ELSE t.window_started_at END,
                            send_count        = CASE WHEN t.window_started_at <= now() - make_interval(secs => :windowSecs)
                                                     THEN 1 ELSE t.send_count + 1 END,
                            day_started_at    = CASE WHEN t.day_started_at <= now() - interval '1 day'
                                                     THEN now() ELSE t.day_started_at END,
                            day_count         = CASE WHEN t.day_started_at <= now() - interval '1 day'
                                                     THEN 1 ELSE t.day_count + 1 END
                        WHERE t.last_sent_at <= now() - make_interval(secs => :cooldown)
                          AND (t.window_started_at <= now() - make_interval(secs => :windowSecs)
                               OR t.send_count < :windowCap)
                          AND (t.day_started_at <= now() - interval '1 day'
                               OR t.day_count < :dayCap)
                        RETURNING expires_at
                        """)
                .bind("userId", userId)
                .bind("identifierId", identifierId)
                .bind("purpose", purpose.code())
                .bind("hash", tokenHash)
                .bind("ttl", p.ttlSeconds())
                .bind("windowSecs", p.windowSeconds())
                .bind("cooldown", p.cooldownSeconds())
                .bind("windowCap", p.windowCap())
                .bind("dayCap", p.dayCap())
                .mapTo(OffsetDateTime.class)
                .findOne();
    }

    /**
     * Seconds until a new send would be allowed. 0 means allowed now, or no row exists yet.
     */
    public int secondsUntilAllowed(Handle h, UUID identifierId, VerificationPurpose purpose) {
        VerificationTokenPolicy p = VerificationTokenPolicy.of(purpose);
        int seconds = h.createQuery("""
                        SELECT CAST(CEIL(EXTRACT(EPOCH FROM (
                                 GREATEST(
                                   last_sent_at + make_interval(secs => :cooldown),
                                   CASE WHEN send_count >= :windowCap
                                        THEN window_started_at + make_interval(secs => :windowSecs) END,
                                   CASE WHEN day_count >= :dayCap
                                        THEN day_started_at + interval '1 day' END
                                 ) - now()))) AS integer) AS wait
                        FROM user_verification_tokens
                        WHERE identifier_id = :identifierId AND purpose = :purpose
                        """)
                .bind("identifierId", identifierId)
                .bind("purpose", purpose.code())
                .bind("cooldown", p.cooldownSeconds())
                .bind("windowSecs", p.windowSeconds())
                .bind("windowCap", p.windowCap())
                .bind("dayCap", p.dayCap())
                .mapTo(Integer.class)
                .findOne()
                .orElse(0);
        return Math.max(0, seconds);
    }

    /**
     * Spends one verification attempt. Increment and limit check are one statement.
     */
    public Optional<ClaimedToken> claimAttempt(Handle h, UUID userId, VerificationPurpose purpose, int maxAttempts) {
        return h.createQuery("""
                        UPDATE user_verification_tokens
                        SET failed_attempts = failed_attempts + 1
                        WHERE user_id = :userId
                          AND purpose = :purpose
                          AND expires_at > now()
                          AND failed_attempts < :max
                        RETURNING id, token_hash
                        """)
                .bind("userId", userId)
                .bind("purpose", purpose.code())
                .bind("max", maxAttempts)
                .map((rs, ctx) -> new ClaimedToken(rs.getObject("id", UUID.class), rs.getString("token_hash")))
                .findOne();
    }

    public void delete(Handle h, UUID tokenId) {
        h.createUpdate("DELETE FROM user_verification_tokens WHERE id = :id")
                .bind("id", tokenId)
                .execute();
    }

    /**
     * Cheap read used before Argon2. Returns the identifier (email) the live token belongs to.
     */
    public Optional<String> findLiveIdentifier(Handle h, VerificationPurpose purpose, String tokenHash) {
        return h.createQuery("""
                        SELECT ui.identifier
                        FROM user_verification_tokens t
                        JOIN user_identifiers ui ON ui.id = t.identifier_id
                        WHERE t.purpose = :purpose AND t.token_hash = :hash AND t.expires_at > now()
                        """)
                .bind("purpose", purpose.code())
                .bind("hash", tokenHash)
                .mapTo(String.class)
                .findOne();
    }

    public void deleteByUserAndPurpose(Handle h, UUID userId, VerificationPurpose purpose) {
        h.createUpdate("DELETE FROM user_verification_tokens WHERE user_id = :userId AND purpose = :purpose")
                .bind("userId", userId)
                .bind("purpose", purpose.code())
                .execute();
    }

    /**
     * Atomic single use for link tokens: only one concurrent caller gets the row back.
     */
    public Optional<ConsumedToken> consumeByHash(Handle h, VerificationPurpose purpose, String tokenHash) {
        return h.createQuery("""
                        DELETE FROM user_verification_tokens
                        WHERE purpose = :purpose AND token_hash = :hash AND expires_at > now()
                        RETURNING id, user_id, identifier_id
                        """)
                .bind("purpose", purpose.code())
                .bind("hash", tokenHash)
                .map((rs, ctx) -> new ConsumedToken(
                        rs.getObject("id", UUID.class),
                        rs.getObject("user_id", UUID.class),
                        rs.getObject("identifier_id", UUID.class)))
                .findOne();
    }
}