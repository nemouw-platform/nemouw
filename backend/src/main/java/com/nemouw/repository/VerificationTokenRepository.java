package com.nemouw.repository;

import com.nemouw.enums.VerificationPurpose;
import org.jdbi.v3.core.Handle;

import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public class VerificationTokenRepository {
    /** Returns expires_at, computed by the DB clock. */
    public OffsetDateTime insert(Handle h, UUID userId, UUID identifierId, VerificationPurpose purpose,
                                 String tokenHash, int ttlSeconds) {
        return h.createQuery("""
                        INSERT INTO user_verification_tokens (user_id, identifier_id, purpose, token_hash, expires_at)
                        VALUES (:u, :i, :p, :h, now() + make_interval(secs => :ttl))
                        RETURNING expires_at""")
                .bind("u", userId).bind("i", identifierId).bind("p", purpose.code())
                .bind("h", tokenHash).bind("ttl", ttlSeconds)
                .mapTo(OffsetDateTime.class).one();
    }

    public record ClaimedToken(UUID id, String tokenHash) {}

    /** Spends one attempt on the live token and returns it. Empty if none, expired, consumed, or locked out. */
    public Optional<ClaimedToken> claimAttempt(Handle h, UUID userId, VerificationPurpose purpose, int maxAttempts) {
        return h.createQuery("""
                    UPDATE user_verification_tokens SET attempts = attempts + 1
                    WHERE id = (
                        SELECT id FROM user_verification_tokens
                        WHERE user_id = :u AND purpose = :p AND consumed_at IS NULL
                          AND expires_at > now() AND attempts < :max
                        ORDER BY created_at DESC LIMIT 1 FOR UPDATE)
                    RETURNING id, token_hash""")
                .bind("u", userId).bind("p", purpose.code()).bind("max", maxAttempts)
                .map(ConstructorMapper.of(ClaimedToken.class)).findFirst();
    }

    public void consume(Handle h, UUID tokenId) {
        h.createUpdate("UPDATE user_verification_tokens SET consumed_at = now() WHERE id = :id")
                .bind("id", tokenId).execute();
    }
}