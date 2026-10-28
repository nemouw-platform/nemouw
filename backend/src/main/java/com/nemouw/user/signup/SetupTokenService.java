package com.nemouw.user.signup;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import static com.nemouw.user.AuthConstants.*;

@Service
class SetupTokenService {
    private static final JwsHeader JWS_HEADER = JwsHeader.with(MacAlgorithm.HS256).build();

    private final JwtEncoder jwtEncoder;
    private final Clock clock;

    @Autowired
    SetupTokenService(JwtEncoder jwtEncoder) {
        this(jwtEncoder, Clock.systemUTC());
    }

    SetupTokenService(JwtEncoder jwtEncoder, Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.clock = clock;
    }

    IssuedSetupToken issue(UUID userId) {
        Instant now = clock.instant();
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