package com.nemouw.user.session;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import static com.nemouw.support.Fixtures.JWT_SECRET;
import static com.nemouw.support.Fixtures.OTHER_SECRET;
import static com.nemouw.support.Fixtures.SESSION_ID;
import static com.nemouw.support.Fixtures.USER_ID;
import static com.nemouw.support.Fixtures.props;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtConfigTest {

    private final SessionRepository sessions = mock(SessionRepository.class);
    private final JwtConfig config = new JwtConfig();
    private final JwtEncoder encoder = config.jwtEncoder(props());
    private final JwtDecoder decoder = config.jwtDecoder(props(), new ActiveSessionValidator(sessions));

    private static String sign(JwtEncoder with, JwtClaimsSet claims) {
        return with.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private static JwtClaimsSet sessionClaims(Instant issuedAt, Instant expiresAt) {
        return JwtClaimsSet.builder()
                .subject(USER_ID.toString())
                .claim("role", "specialist")
                .claim("sid", SESSION_ID.toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();
    }

    private static String reencodePayload(String token, String from, String to) {
        String[] parts = token.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        String changed = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.replace(from, to).getBytes(StandardCharsets.UTF_8));
        return parts[0] + "." + changed + "." + parts[2];
    }

    private void sessionIsLive() {
        when(sessions.isActive(SESSION_ID, USER_ID))
                .thenReturn(true);
    }

    @Nested
    class HappyPaths {

        @Test
        void acceptsAFreshTokenSignedWithTheServerSecretForALiveSession() {
            sessionIsLive();
            Instant now = Instant.now();
            String token = sign(encoder, sessionClaims(now, now.plusSeconds(300)));

            Jwt jwt = decoder.decode(token);

            assertThat(jwt.getSubject()).isEqualTo(USER_ID.toString());
            assertThat(jwt.getClaimAsString("role")).isEqualTo("specialist");
        }

        @Test
        void buildsAnHmacKeyForTheHmacSha256Algorithm() {
            assertThat(JwtConfig.hmacKey(JWT_SECRET).getAlgorithm()).isEqualTo("HmacSHA256");
            assertThat(JwtConfig.hmacKey(JWT_SECRET).getEncoded()).isEqualTo(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        }
    }

    @Nested
    class ErrorPaths {

        @Test
        void rejectsATokenThatExpiredFiveSecondsAgoBecauseThereIsNoClockSkew() {
            sessionIsLive();
            Instant now = Instant.now();
            String token = sign(encoder, sessionClaims(now.minusSeconds(905), now.minusSeconds(5)));

            assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtValidationException.class);
        }

        @Test
        void rejectsATokenForARevokedSession() {
            when(sessions.isActive(any(), any()))
                    .thenReturn(false);

            Instant now = Instant.now();
            String token = sign(
                    encoder,
                    sessionClaims(now, now.plusSeconds(300))
            );

            assertThatThrownBy(() -> decoder.decode(token))
                    .isInstanceOf(JwtValidationException.class);
        }

        @Test
        void rejectsATokenSignedWithAnotherSecret() {
            sessionIsLive();
            JwtEncoder stranger = new NimbusJwtEncoder(new ImmutableSecret<>(JwtConfig.hmacKey(OTHER_SECRET)));
            Instant now = Instant.now();
            String token = sign(stranger, sessionClaims(now, now.plusSeconds(300)));

            assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(BadJwtException.class);
        }

        @Test
        void rejectsATokenWhosePayloadWasEditedToClaimAnotherRole() {
            sessionIsLive();
            Instant now = Instant.now();
            String genuine = sign(encoder, sessionClaims(now, now.plusSeconds(300)));
            String tampered = reencodePayload(genuine, "\"specialist\"", "\"admin\"");

            assertThatThrownBy(() -> decoder.decode(tampered)).isInstanceOf(BadJwtException.class);
        }

        @Test
        void rejectsATokenSignedWithHs512EvenWhenItUsesTheRightSecret() throws Exception {
            sessionIsLive();
            SignedJWT forged = new SignedJWT(new JWSHeader(JWSAlgorithm.HS512), new JWTClaimsSet.Builder()
                    .subject(USER_ID.toString())
                    .claim("sid", SESSION_ID.toString())
                    .expirationTime(Date.from(Instant.now().plusSeconds(300)))
                    .build());
            forged.sign(new MACSigner(JWT_SECRET.getBytes(StandardCharsets.UTF_8)));
            String token = forged.serialize();

            assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(BadJwtException.class);
        }

        @Test
        void rejectsAnUnsignedTokenWithAlgNone() {
            sessionIsLive();
            String token = new PlainJWT(new JWTClaimsSet.Builder()
                    .subject(USER_ID.toString())
                    .claim("sid", SESSION_ID.toString())
                    .expirationTime(Date.from(Instant.now().plusSeconds(300)))
                    .build()).serialize();

            assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(BadJwtException.class);
        }

        @Test
        void rejectsATokenThatIsNotAJwtAtAll() {
            assertThatThrownBy(() -> decoder.decode("not.a.jwt")).isInstanceOf(BadJwtException.class);
        }
    }

    @Nested
    class DependencyFailures {

        @Test
        void doesNotAcceptATokenWhenTheSessionLookupFails() {
            when(sessions.isActive(any(), any()))
                    .thenThrow(new QueryTimeoutException("statement timeout after 5000 ms"));
            Instant now = Instant.now();
            String token = sign(encoder, sessionClaims(now, now.plusSeconds(300)));

            assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(RuntimeException.class);
        }
    }
}
