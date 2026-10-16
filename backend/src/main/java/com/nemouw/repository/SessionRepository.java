package com.nemouw.repository;

import com.nemouw.enums.UserStatus;
import com.nemouw.model.ClientInfo;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;

import java.util.Optional;
import java.util.UUID;

public class SessionRepository {
    public record RotatedSession(UUID sessionId, UUID userId, String roleCode) {}

    public record ReplayedSession(UUID sessionId, UUID userId, boolean withinGrace) {}

    public record ActiveSession(UUID userId, String roleCode) {}

    public UUID insert(Handle h, UUID userId, String refreshTokenHash, ClientInfo c, int ttlDays) {
        return h.createQuery("""
                        INSERT INTO user_sessions
                            (user_id, refresh_token_hash, user_agent, device_type, os_name, browser, ip_address, expires_at)
                        VALUES (:u, :h, :ua, :device, :os, :browser, CAST(:ip AS inet),
                                now() + make_interval(days => :days))
                        RETURNING id""")
                .bind("u", userId).bind("h", refreshTokenHash)
                .bind("ua", c.userAgent()).bind("device", c.deviceType())
                .bind("os", c.osName()).bind("browser", c.browser())
                .bind("ip", c.ip()).bind("days", ttlDays)
                .mapTo(UUID.class).one();
    }

    /**
     * Atomic swap. Two requests with the same token cannot both succeed: Postgres locks the row, and the
     * second request re-checks the WHERE clause after the first commits, finds the hash changed, and gets nothing.
     * Also refuses revoked or expired sessions and users who are no longer active.
     */
    public Optional<RotatedSession> rotate(Handle h, String oldHash, String newHash) {
        return h.createQuery("""
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
                .bind("oldHash", oldHash).bind("newHash", newHash)
                .bind("active", UserStatus.ACTIVE.code())
                .map(ConstructorMapper.of(RotatedSession.class)).findFirst();
    }

    /**
     * Finds a live session whose PREVIOUS token this is, i.e. a stale token being replayed.
     */
    public Optional<ReplayedSession> findByPreviousHash(Handle h, String hash, int graceSeconds) {
        return h.createQuery("""
                        SELECT id AS session_id, user_id,
                               coalesce(rotated_at > now() - make_interval(secs => :grace), false) AS within_grace
                        FROM user_sessions
                        WHERE previous_refresh_token_hash = :hash AND revoked_at IS NULL""")
                .bind("hash", hash).bind("grace", graceSeconds)
                .map(ConstructorMapper.of(ReplayedSession.class)).findFirst();
    }

    public void revoke(Handle h, UUID sessionId) {
        h.createUpdate("UPDATE user_sessions SET revoked_at = now() WHERE id = :id AND revoked_at IS NULL")
                .bind("id", sessionId).execute();
    }

    /**
     * Returns a row only if the session is live (not revoked, not expired) AND the user is active.
     */
    public Optional<ActiveSession> findActive(Handle h, UUID sessionId, UUID userId) {
        return h.createQuery("""
                        SELECT u.id AS user_id, r.code AS role_code
                        FROM user_sessions s
                        JOIN users u ON u.id = s.user_id
                        JOIN user_roles r ON r.id = u.role_id
                        WHERE s.id = :sid AND s.user_id = :uid
                          AND s.revoked_at IS NULL AND s.expires_at > now()
                          AND u.status = :active""")
                .bind("sid", sessionId).bind("uid", userId)
                .bind("active", UserStatus.ACTIVE.code())
                .map(ConstructorMapper.of(ActiveSession.class)).findFirst();
    }

    /**
     * Logout: matches the current OR previous token, since a client may be holding either after a tab race.
     */
    public void revokeByHash(Handle h, String hash) {
        h.createUpdate("""
                        UPDATE user_sessions SET revoked_at = now()
                        WHERE (refresh_token_hash = :h OR previous_refresh_token_hash = :h)
                          AND revoked_at IS NULL""")
                .bind("h", hash).execute();
    }

    public void revokeAllForUser(Handle h, UUID userId) {
        h.createUpdate("UPDATE user_sessions SET revoked_at = now() WHERE user_id = :userId AND revoked_at IS NULL")
                .bind("userId", userId).execute();
    }
}