package com.nemouw.user.session;

import com.nemouw.common.enums.UserStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
class SessionRepository {
    private final JdbcClient jdbc;

    SessionRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public record RotatedSession(UUID sessionId, UUID userId, String roleCode) {
    }

    public record ReplayedSession(UUID sessionId, UUID userId, boolean withinGrace) {
    }

    UUID insert(UUID userId, String refreshTokenHash, ClientInfo c, int ttlDays) {
        return jdbc.sql("""
                        INSERT INTO user_sessions
                            (user_id, refresh_token_hash, user_agent, device_type, os_name, browser, ip_address, expires_at)
                        VALUES (:u, :h, :ua, :device, :os, :browser, CAST(:ip AS inet),
                                now() + make_interval(days => :days))
                        RETURNING id""")
                .param("u", userId).param("h", refreshTokenHash)
                .param("ua", c.userAgent()).param("device", c.deviceType())
                .param("os", c.osName()).param("browser", c.browser())
                .param("ip", c.ip()).param("days", ttlDays)
                .query(UUID.class).single();
    }

    /**
     * Atomic swap. Two requests with the same token cannot both succeed: Postgres locks the row, and the
     * second request re-checks the WHERE clause after the first commits, finds the hash changed, and gets nothing.
     * Also refuses revoked or expired sessions and users who are no longer active.
     */
    Optional<RotatedSession> rotate(String oldHash, String newHash) {
        return jdbc.sql("""
                        UPDATE user_sessions s
                        SET previous_refresh_token_hash = s.refresh_token_hash,
                            refresh_token_hash = :newHash,
                            rotated_at = now()
                        FROM users u
                        JOIN user_roles r ON r.id = u.role_id
                        WHERE s.refresh_token_hash = :oldHash
                          AND u.id = s.user_id
                          AND s.revoked_at IS NULL AND s.expires_at > now()
                          AND u.status = :active
                        RETURNING s.id AS session_id, s.user_id, r.code AS role_code""")
                .param("oldHash", oldHash).param("newHash", newHash)
                .param("active", UserStatus.ACTIVE.code())
                .query(RotatedSession.class).list().stream().findFirst();
    }

    /**
     * Finds a live session whose PREVIOUS token this is, i.e. a stale token being replayed.
     */
    Optional<ReplayedSession> findByPreviousHash(String hash, int graceSeconds) {
        return jdbc.sql("""
                        SELECT id AS session_id, user_id,
                               coalesce(rotated_at > now() - make_interval(secs => :grace), false) AS within_grace
                        FROM user_sessions
                        WHERE previous_refresh_token_hash = :hash AND revoked_at IS NULL""")
                .param("hash", hash).param("grace", graceSeconds)
                .query(ReplayedSession.class).list().stream().findFirst();
    }

    void revoke(UUID sessionId) {
        jdbc.sql("UPDATE user_sessions SET revoked_at = now() WHERE id = :id AND revoked_at IS NULL")
                .param("id", sessionId)
                .update();
    }

    /**
     * True only if the session is live (not revoked, not expired) and the user is active.
     */
    boolean isActive(UUID sessionId, UUID userId) {
        return jdbc.sql("""
                        SELECT EXISTS (
                            SELECT 1
                            FROM user_sessions s
                            JOIN users u ON u.id = s.user_id
                            WHERE s.id = :sid AND s.user_id = :uid
                              AND s.revoked_at IS NULL AND s.expires_at > now()
                              AND u.status = :active)""")
                .param("sid", sessionId)
                .param("uid", userId)
                .param("active", UserStatus.ACTIVE.code())
                .query(Boolean.class)
                .single();
    }

    /**
     * Logout: matches the current OR previous token, since a client may be holding either after a tab race.
     */
    void revokeByHash(String hash) {
        jdbc.sql("""
                        UPDATE user_sessions SET revoked_at = now()
                        WHERE (refresh_token_hash = :h OR previous_refresh_token_hash = :h)
                          AND revoked_at IS NULL""")
                .param("h", hash)
                .update();
    }

    void revokeAllForUser(UUID userId) {
        jdbc.sql("UPDATE user_sessions SET revoked_at = now() WHERE user_id = :userId AND revoked_at IS NULL")
                .param("userId", userId)
                .update();
    }
}