package com.nemouw.auth.service;

import com.nemouw.auth.model.IssuedSetupToken;
import com.nemouw.support.Jwts;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncodingException;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import static com.nemouw.auth.AuthConstants.SETUP_TTL_SECONDS;
import static com.nemouw.support.Fixtures.NOW;
import static com.nemouw.support.Fixtures.USER_ID;
import static com.nemouw.support.Fixtures.fixedClock;
import static com.nemouw.support.Jwts.decodeIgnoringExpiry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SetupTokenServiceTest {

    private final SetupTokenService service = new SetupTokenService(Jwts.encoder(), fixedClock());

    @Nested
    class HappyPaths {

        @Test
        void issuesATokenForTheUserWithOnlyThePasswordSetupScope() {
            IssuedSetupToken issued = service.issue(USER_ID);

            Jwt jwt = decodeIgnoringExpiry(issued.token());
            assertThat(issued.userId()).isEqualTo(USER_ID);
            assertThat(jwt.getSubject()).isEqualTo(USER_ID.toString());
            assertThat(jwt.getClaimAsString("scope")).isEqualTo("password_setup");
        }

        @Test
        void carriesNoRoleAndNoSessionIdSoItCanNeverActAsASession() {
            Jwt jwt = decodeIgnoringExpiry(service.issue(USER_ID).token());

            assertThat(jwt.getClaims()).doesNotContainKeys("role", "sid");
        }

        @Test
        void expiresExactlyFifteenMinutesAfterTheClockTime() {
            Jwt jwt = decodeIgnoringExpiry(service.issue(USER_ID).token());

            assertThat(jwt.getIssuedAt()).isEqualTo(NOW);
            assertThat(jwt.getExpiresAt()).isEqualTo(NOW.plusSeconds(SETUP_TTL_SECONDS));
        }

        @Test
        void grantsExactlyOneAuthorityWhichIsScopePasswordSetup() {
            Jwt jwt = decodeIgnoringExpiry(service.issue(USER_ID).token());

            assertThat(new JwtGrantedAuthoritiesConverter().convert(jwt))
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactly("SCOPE_password_setup");
        }

        @Test
        void signsWithHs256() {
            Jwt jwt = decodeIgnoringExpiry(service.issue(USER_ID).token());

            assertThat(jwt.getHeaders().get("alg")).isEqualTo("HS256");
        }
    }

    @Nested
    class ErrorPaths {

        @Test
        void keepsTheTokenOutOfItsToString() {
            IssuedSetupToken issued = service.issue(USER_ID);

            assertThat(issued.toString()).doesNotContain(issued.token()).contains(USER_ID.toString());
        }
    }

    @Nested
    class DependencyFailures {

        @Test
        void propagatesASigningFailureInsteadOfReturningAnUnsignedToken() {
            JwtEncoder brokenEncoder = mock(JwtEncoder.class);
            when(brokenEncoder.encode(any())).thenThrow(new JwtEncodingException("Failed to sign the JWT"));
            SetupTokenService failing = new SetupTokenService(brokenEncoder, fixedClock());

            assertThatThrownBy(() -> failing.issue(USER_ID)).isInstanceOf(JwtEncodingException.class);
        }
    }
}
