package com.nemouw.user.token;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
class VerificationTokenRepository {
    private final JdbcClient jdbc;

    VerificationTokenRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public record ClaimedToken(UUID id, String tokenHash) {
    }

    /**
     * Creates or replaces the single token row for (identifier, purpose), only if cooldown,
     * window cap and daily cap allow it. Empty result means throttled.
     * Uses the purpose's default lifetime.
     */
    Optional<OffsetDateTime> upsertIfAllowed(UUID userId, UUID identifierId,
                                             VerificationPurpose purpose, String tokenHash) {
        return upsertIfAllowed(userId, identifierId, purpose, tokenHash,
                VerificationTokenPolicy.of(purpose).ttlSeconds());
    }

    /** Same as above, with an explicit token lifetime (invites live longer than self-service resets). */
    Optional<OffsetDateTime> upsertIfAllowed(UUID userId, UUID identifierId,
                                             VerificationPurpose purpose, String tokenHash,
                                             int ttlSeconds) {
        VerificationTokenPolicy p = VerificationTokenPolicy.of(purpose);
        return jdbc.sql("""
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
                .param("userId", userId)
                .param("identifierId", identifierId)
                .param("purpose", purpose.code())
                .param("hash", tokenHash)
                .param("ttl", ttlSeconds)
                .param("windowSecs", p.windowSeconds())
                .param("cooldown", p.cooldownSeconds())
                .param("windowCap", p.windowCap())
                .param("dayCap", p.dayCap())
                .query(OffsetDateTime.class)
                .optional();
    }

    /**
     * Seconds until a new send would be allowed. 0 means allowed now, or no row exists yet.
     */
    int secondsUntilAllowed(UUID identifierId, VerificationPurpose purpose) {
        VerificationTokenPolicy p = VerificationTokenPolicy.of(purpose);
        int seconds = jdbc.sql("""
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
                .param("identifierId", identifierId)
                .param("purpose", purpose.code())
                .param("cooldown", p.cooldownSeconds())
                .param("windowSecs", p.windowSeconds())
                .param("windowCap", p.windowCap())
                .param("dayCap", p.dayCap())
                .query(Integer.class)
                .optional()
                .orElse(0);
        return Math.max(0, seconds);
    }

    /**
     * Spends one verification attempt. Increment and limit check are one statement.
     */
    Optional<ClaimedToken> claimAttempt(UUID userId, VerificationPurpose purpose, int maxAttempts) {
        return jdbc.sql("""
                        UPDATE user_verification_tokens
                        SET failed_attempts = failed_attempts + 1
                        WHERE user_id = :userId
                          AND purpose = :purpose
                          AND expires_at > now()
                          AND failed_attempts < :max
                        RETURNING id, token_hash
                        """)
                .param("userId", userId)
                .param("purpose", purpose.code())
                .param("max", maxAttempts)
                .query(ClaimedToken.class)
                .optional();
    }

    void delete(UUID tokenId) {
        jdbc.sql("DELETE FROM user_verification_tokens WHERE id = :id")
                .param("id", tokenId)
                .update();
    }

    /**
     * Cheap read used before Argon2. Returns the identifier (email) the live token belongs to.
     */
    Optional<String> findLiveIdentifier(VerificationPurpose purpose, String tokenHash) {
        return jdbc.sql("""
                        SELECT ui.identifier
                        FROM user_verification_tokens t
                        JOIN user_identifiers ui ON ui.id = t.identifier_id
                        WHERE t.purpose = :purpose AND t.token_hash = :hash AND t.expires_at > now()
                        """)
                .param("purpose", purpose.code())
                .param("hash", tokenHash)
                .query(String.class)
                .optional();
    }

    void deleteByUserAndPurpose(UUID userId, VerificationPurpose purpose) {
        jdbc.sql("DELETE FROM user_verification_tokens WHERE user_id = :userId AND purpose = :purpose")
                .param("userId", userId)
                .param("purpose", purpose.code())
                .update();
    }

    /**
     * Atomic single use for link tokens: only one concurrent caller gets the row back.
     */
    Optional<ConsumedToken> consumeByHash(VerificationPurpose purpose, String tokenHash) {
        return jdbc.sql("""
                        DELETE FROM user_verification_tokens
                        WHERE purpose = :purpose AND token_hash = :hash AND expires_at > now()
                        RETURNING id, user_id, identifier_id
                        """)
                .param("purpose", purpose.code())
                .param("hash", tokenHash)
                .query(ConsumedToken.class)
                .optional();
    }
}