package com.nemouw.repository;

import org.jdbi.v3.core.Handle;

import java.util.UUID;

public class SessionRepository {
    public UUID insert(Handle h, UUID userId, String refreshTokenHash, String userAgent, String ip, int ttlDays) {
        return h.createQuery("""
                        INSERT INTO user_sessions (user_id, refresh_token_hash, user_agent, ip_address, expires_at)
                        VALUES (:u, :h, :ua, CAST(:ip AS inet), now() + make_interval(days => :days))
                        RETURNING id""")
                .bind("u", userId).bind("h", refreshTokenHash)
                .bind("ua", userAgent).bind("ip", ip).bind("days", ttlDays)
                .mapTo(UUID.class).one();
    }
}