package com.nemouw.user.session;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import static com.nemouw.support.Fixtures.NOW;
import static com.nemouw.support.Fixtures.OTHER_USER_ID;
import static com.nemouw.support.Fixtures.SESSION_ID;
import static com.nemouw.support.Fixtures.USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ActiveSessionValidatorTest {

    private final SessionRepository sessions = mock(SessionRepository.class);
    private final ActiveSessionValidator validator = new ActiveSessionValidator(sessions);

    private static Jwt.Builder token() {
        return Jwt.withTokenValue("eyJhbGciOiJIUzI1NiJ9.payload.signature")
                .header("alg", "HS256")
                .issuedAt(NOW)
                .expiresAt(NOW.plusSeconds(900));
    }

    private static Jwt sessionToken() {
        return token()
                .subject(USER_ID.toString())
                .claim("sid", SESSION_ID.toString())
                .claim("role", "specialist")
                .build();
    }

    private static Jwt setupToken() {
        return token()
                .subject(USER_ID.toString())
                .claim("scope", "password_setup")
                .build();
    }

    private void sessionIsLive() {
        when(sessions.isActive(SESSION_ID, USER_ID)).thenReturn(true);
    }

    @Nested
    class HappyPaths {

        @Test
        void acceptsASessionTokenWhoseSessionIsLiveAndUserIsActive() {
            sessionIsLive();

            OAuth2TokenValidatorResult result = validator.validate(sessionToken());

            assertThat(result.hasErrors()).isFalse();
        }

        @Test
        void acceptsAPasswordSetupTokenWithoutTouchingTheDatabase() {
            OAuth2TokenValidatorResult result = validator.validate(setupToken());

            assertThat(result.hasErrors()).isFalse();
            verifyNoInteractions(sessions);
        }
    }

    @Nested
    class ErrorPaths {

        @Test
        void rejectsASessionTokenWhoseSessionWasRevokedOrExpiredOrWhoseUserIsNotActive() {
            when(sessions.isActive(SESSION_ID, USER_ID)).thenReturn(false);

            OAuth2TokenValidatorResult result = validator.validate(sessionToken());

            assertThat(result.hasErrors()).isTrue();
            assertThat(result.getErrors())
                    .extracting(OAuth2Error::getErrorCode)
                    .containsExactly("invalid_token");
        }

        @Test
        void rejectsASessionIdPairedWithAnotherUsersId() {
            when(sessions.isActive(SESSION_ID, OTHER_USER_ID)).thenReturn(false);

            Jwt forged = token()
                    .subject(OTHER_USER_ID.toString())
                    .claim("sid", SESSION_ID.toString())
                    .build();

            assertThat(validator.validate(forged).hasErrors()).isTrue();
        }

        @Test
        void rejectsATokenWithNeitherASessionIdNorTheSetupScope() {
            Jwt bare = token()
                    .subject(USER_ID.toString())
                    .claim("role", "admin")
                    .build();

            assertThat(validator.validate(bare).hasErrors()).isTrue();
            verifyNoInteractions(sessions);
        }

        @Test
        void rejectsATokenWithoutASessionIdWhoseScopeIsSomethingOtherThanPasswordSetup() {
            Jwt other = token()
                    .subject(USER_ID.toString())
                    .claim("scope", "admin")
                    .build();

            assertThat(validator.validate(other).hasErrors()).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "not-a-uuid",
                "",
                "7a1d9e02-4c55-4b8a-9f3e",
                "7a1d9e02-4c55-4b8a-9f3e-60b2c8d41e75-extra"
        })
        void rejectsAMalformedSessionIdWithoutQueryingTheDatabase(String sessionId) {
            Jwt malformed = token()
                    .subject(USER_ID.toString())
                    .claim("sid", sessionId)
                    .build();

            assertThat(validator.validate(malformed).hasErrors()).isTrue();
            verifyNoInteractions(sessions);
        }

        @ParameterizedTest
        @ValueSource(strings = {"anonymous", "42", ""})
        void rejectsAMalformedSubjectWithoutQueryingTheDatabase(String subject) {
            Jwt malformed = token()
                    .subject(subject)
                    .claim("sid", SESSION_ID.toString())
                    .build();

            assertThat(validator.validate(malformed).hasErrors()).isTrue();
            verifyNoInteractions(sessions);
        }

        @Test
        void rejectsASessionTokenWithNoSubject() {
            Jwt anonymous = token()
                    .claim("sid", SESSION_ID.toString())
                    .build();

            assertThat(validator.validate(anonymous).hasErrors()).isTrue();
            verifyNoInteractions(sessions);
        }
    }

    @Nested
    class DependencyFailures {

        @Test
        void neverGrantsAccessWhenTheSessionLookupTimesOut() {
            when(sessions.isActive(any(), any()))
                    .thenThrow(new QueryTimeoutException("statement timeout after 5000 ms"));

            assertThatThrownBy(() -> validator.validate(sessionToken()))
                    .isInstanceOf(QueryTimeoutException.class);
        }
    }
}
