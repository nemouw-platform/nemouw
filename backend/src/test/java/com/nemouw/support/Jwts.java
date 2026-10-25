package com.nemouw.support;

import com.nemouw.auth.config.JwtConfig;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import static com.nemouw.support.Fixtures.JWT_SECRET;
import static com.nemouw.support.Fixtures.props;

/** Token helpers: the production encoder, and a decoder that checks the signature but not the clock. */
public final class Jwts {
    private Jwts() {
    }

    public static JwtEncoder encoder() {
        return new JwtConfig().jwtEncoder(props());
    }

    /** Verifies signature and algorithm only, so tokens minted with a fixed 2026 clock can still be read back. */
    public static Jwt decodeIgnoringExpiry(String token) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(JwtConfig.hmacKey(JWT_SECRET))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(jwt -> OAuth2TokenValidatorResult.success());
        return decoder.decode(token);
    }
}
