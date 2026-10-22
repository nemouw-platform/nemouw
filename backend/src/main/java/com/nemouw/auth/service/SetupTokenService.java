package com.nemouw.auth.service;

import com.nemouw.auth.model.IssuedSetupToken;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

import static com.nemouw.auth.AuthConstants.*;

/**
 * Signs the short-lived token handed out after a correct signup OTP.
 * <p>
 * It carries only the scope {@code password_setup}: no role and no session id. Spring Security therefore
 * grants it exactly one authority (SCOPE_password_setup), which SecurityConfig accepts on a single endpoint.
 * Single use needs no table: setting the password flips the account from PENDING to ACTIVE under a row lock,
 * and the setup step refuses anything that is not PENDING.
 */
@Service
public class SetupTokenService {
    private static final JwsHeader JWS_HEADER = JwsHeader.with(MacAlgorithm.HS256).build();

    private final JwtEncoder jwtEncoder;

    public SetupTokenService(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }

    public IssuedSetupToken issue(UUID userId) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(userId.toString())
                .claim(CLAIM_SCOPE, PASSWORD_SETUP_SCOPE)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(SETUP_TTL_SECONDS))
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(JWS_HEADER, claims)).getTokenValue();
        return new IssuedSetupToken(token, userId);
    }
}