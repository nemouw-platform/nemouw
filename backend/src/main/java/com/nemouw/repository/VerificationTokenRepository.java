package com.nemouw.repository;

import com.nemouw.enums.VerificationPurpose;
import org.jdbi.v3.core.Handle;

import java.time.OffsetDateTime;
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
}