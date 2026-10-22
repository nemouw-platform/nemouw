package com.nemouw.auth.config;

import com.nemouw.auth.service.ActiveSessionValidator;
import com.nemouw.config.AppProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Access tokens are HS256 JWTs signed with JWT_SECRET. The decoder below is what Spring Security uses to
 * check every request's token: signature, expiry, then {@link ActiveSessionValidator}.
 */
@Configuration
public class JwtConfig {

    public static SecretKey hmacKey(String secret) {
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(AppProperties props) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(hmacKey(props.security().jwtSecret())));
    }

    /** HS256 only: the algorithm is pinned, so a token cannot pick a weaker one (or "none") for itself. */
    @Bean
    public JwtDecoder jwtDecoder(AppProperties props, ActiveSessionValidator activeSession) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(hmacKey(props.security().jwtSecret()))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefault(), activeSession));
        return decoder;
    }
}