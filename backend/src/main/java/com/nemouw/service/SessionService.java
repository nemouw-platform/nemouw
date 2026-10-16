package com.nemouw.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.nemouw.error.Errors;
import com.nemouw.model.AuthDtos.IssuedSession;
import com.nemouw.model.ClientInfo;
import com.nemouw.repository.SessionRepository;
import com.nemouw.repository.SessionRepository.ReplayedSession;
import org.jdbi.v3.core.Handle;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static com.nemouw.Constants.*;

public class SessionService {
    private final SecureRandom random = new SecureRandom();
    private final SessionRepository sessions;
    private final Algorithm algorithm;
    private final JWTVerifier verifier;

    public record AccessClaims(UUID userId, UUID sessionId) {}

    public SessionService(SessionRepository sessions, String jwtSecret) {
        if (jwtSecret == null || jwtSecret.length() < MIN_SECRET_LENGTH)
            throw new IllegalStateException("JWT_SECRET must be at least " + MIN_SECRET_LENGTH + " characters");
        this.sessions = sessions;
        this.algorithm = Algorithm.HMAC256(jwtSecret);
        this.verifier = JWT.require(algorithm).build();
    }

    public IssuedSession issue(Handle h, UUID userId, String roleCode, ClientInfo client) {
        String refreshToken = newRefreshToken();
        UUID sessionId = sessions.insert(h, userId, sha256(refreshToken), client, REFRESH_TTL_DAYS);
        return new IssuedSession(signAccessToken(userId, roleCode, sessionId), refreshToken, userId, roleCode);
    }

    public Optional<IssuedSession> rotate(Handle h, String presentedRefreshToken) {
        String next = newRefreshToken();
        return sessions.rotate(h, sha256(presentedRefreshToken), sha256(next))
                .map(s -> new IssuedSession(
                        signAccessToken(s.userId(), s.roleCode(), s.sessionId()), next, s.userId(), s.roleCode()));
    }

    /** Signature and expiry only. Whether the session and user are still valid is the middleware's DB check. */
    public AccessClaims verifyAccessToken(String token) {
        try {
            DecodedJWT jwt = verifier.verify(token);
            String sid = jwt.getClaim("sid").asString();
            if (jwt.getSubject() == null || sid == null) throw Errors.invalidAccessToken();
            return new AccessClaims(UUID.fromString(jwt.getSubject()), UUID.fromString(sid));
        } catch (JWTVerificationException | IllegalArgumentException e) {
            throw Errors.invalidAccessToken();
        }
    }

    /**
     * Call after a failed rotate. If the token was a stale one replayed OUTSIDE the grace window,
     * revokes that session and returns it. Otherwise, does nothing.
     */
    public Optional<ReplayedSession> revokeIfReplayed(Handle h, String presentedRefreshToken) {
        var replay = sessions.findByPreviousHash(h, sha256(presentedRefreshToken), REFRESH_REUSE_GRACE_SECONDS);
        if (replay.isEmpty() || replay.get().withinGrace()) return Optional.empty();
        sessions.revoke(h, replay.get().sessionId());
        return replay;
    }

    public void revokeByRefreshToken(Handle h, String presentedRefreshToken) {
        sessions.revokeByHash(h, sha256(presentedRefreshToken));
    }

    private String newRefreshToken() {
        byte[] raw = new byte[32];
        random.nextBytes(raw);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    }

    private String signAccessToken(UUID userId, String roleCode, UUID sessionId) {
        Instant now = Instant.now();
        return JWT.create()
                .withSubject(userId.toString())
                .withClaim("role", roleCode)
                .withClaim("sid", sessionId.toString())
                .withIssuedAt(now)
                .withExpiresAt(now.plusSeconds(ACCESS_TTL_SECONDS))
                .sign(algorithm);
    }

    public static String sha256(String s) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}