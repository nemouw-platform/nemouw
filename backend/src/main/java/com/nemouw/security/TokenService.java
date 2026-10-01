package com.nemouw.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.nemouw.model.AuthDtos.AuthTokens;
import com.nemouw.repository.SessionRepository;
import org.jdbi.v3.core.Handle;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

import static com.nemouw.Constants.*;

public class TokenService {
    private final SecureRandom random = new SecureRandom();
    private final SessionRepository sessions;
    private final Algorithm algorithm;

    public TokenService(SessionRepository sessions, String jwtSecret) {
        if (jwtSecret == null || jwtSecret.length() < MIN_SECRET_LENGTH)
            throw new IllegalStateException("JWT_SECRET must be at least " + MIN_SECRET_LENGTH + " characters");
        this.sessions = sessions;
        this.algorithm = Algorithm.HMAC256(jwtSecret);
    }

    public AuthTokens issue(Handle h, UUID userId, String roleCode, String userAgent, String ip) {
        byte[] raw = new byte[32];
        random.nextBytes(raw);
        String refreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);

        UUID sessionId = sessions.insert(h, userId, sha256(refreshToken), userAgent, ip, REFRESH_TTL_DAYS);

        Instant now = Instant.now();
        String accessToken = JWT.create()
                .withSubject(userId.toString())
                .withClaim("role", roleCode)
                .withClaim("sid", sessionId.toString())
                .withIssuedAt(now)
                .withExpiresAt(now.plusSeconds(ACCESS_TTL_SECONDS))
                .sign(algorithm);

        return new AuthTokens(accessToken, refreshToken, "Bearer", ACCESS_TTL_SECONDS);
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