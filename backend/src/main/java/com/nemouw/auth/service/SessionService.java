package com.nemouw.auth.service;

import com.nemouw.auth.model.ClientInfo;
import com.nemouw.auth.model.IssuedSession;
import com.nemouw.auth.repository.SessionRepository;
import com.nemouw.auth.repository.SessionRepository.ReplayedSession;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static com.nemouw.auth.AuthConstants.*;

/**
 * Creates, rotates and revokes sessions. Methods do not open transactions themselves:
 * they join the one the calling service (AuthService) opens.
 * Verifying access tokens is no longer done here; Spring Security does it per request.
 */
@Service
public class SessionService {
    private static final int REFRESH_TOKEN_BYTES = 32;
    private static final JwsHeader JWS_HEADER = JwsHeader.with(MacAlgorithm.HS256).build();

    private final SecureRandom random = new SecureRandom();
    private final SessionRepository sessions;
    private final JwtEncoder jwtEncoder;

    public SessionService(SessionRepository sessions, JwtEncoder jwtEncoder) {
        this.sessions = sessions;
        this.jwtEncoder = jwtEncoder;
    }

    public IssuedSession issue(UUID userId, String roleCode, ClientInfo client) {
        String refreshToken = newRefreshToken();
        UUID sessionId = sessions.insert(userId, sha256(refreshToken), client, REFRESH_TTL_DAYS);
        return new IssuedSession(signAccessToken(userId, roleCode, sessionId), refreshToken, userId, roleCode);
    }

    public Optional<IssuedSession> rotate(String presentedRefreshToken) {
        String next = newRefreshToken();
        return sessions.rotate(sha256(presentedRefreshToken), sha256(next))
                .map(s -> new IssuedSession(
                        signAccessToken(s.userId(), s.roleCode(), s.sessionId()), next, s.userId(), s.roleCode()));
    }

    /**
     * Call after a failed rotate. If the token was a stale one replayed OUTSIDE the grace window,
     * revokes that session and returns it. Otherwise, does nothing.
     */
    public Optional<ReplayedSession> revokeIfReplayed(String presentedRefreshToken) {
        var replay = sessions.findByPreviousHash(sha256(presentedRefreshToken), REFRESH_REUSE_GRACE_SECONDS);
        if (replay.isEmpty() || replay.get().withinGrace()) return Optional.empty();
        sessions.revoke(replay.get().sessionId());
        return replay;
    }

    public void revokeByRefreshToken(String presentedRefreshToken) {
        sessions.revokeByHash(sha256(presentedRefreshToken));
    }

    private String newRefreshToken() {
        byte[] raw = new byte[REFRESH_TOKEN_BYTES];
        random.nextBytes(raw);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    }

    private String signAccessToken(UUID userId, String roleCode, UUID sessionId) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(userId.toString())
                .claim(CLAIM_ROLE, roleCode)
                .claim(CLAIM_SESSION_ID, sessionId.toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(ACCESS_TTL_SECONDS))
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(JWS_HEADER, claims)).getTokenValue();
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