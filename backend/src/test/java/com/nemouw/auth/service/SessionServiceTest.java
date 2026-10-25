package com.nemouw.auth.service;

import com.nemouw.auth.model.IssuedSession;
import com.nemouw.auth.repository.SessionRepository;
import com.nemouw.auth.repository.SessionRepository.ReplayedSession;
import com.nemouw.auth.repository.SessionRepository.RotatedSession;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncodingException;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.Optional;
import java.util.stream.Stream;

import static com.nemouw.auth.AuthConstants.ACCESS_TTL_SECONDS;
import static com.nemouw.auth.AuthConstants.REFRESH_REUSE_GRACE_SECONDS;
import static com.nemouw.auth.AuthConstants.REFRESH_TTL_DAYS;
import static com.nemouw.support.Fixtures.NOW;
import static com.nemouw.support.Fixtures.SESSION_ID;
import static com.nemouw.support.Fixtures.USER_ID;
import static com.nemouw.support.Fixtures.clientInfo;
import static com.nemouw.support.Fixtures.fixedClock;
import com.nemouw.support.Jwts;
import static com.nemouw.support.Jwts.decodeIgnoringExpiry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SessionServiceTest {

    private static final String ROLE = "specialist";
    private static final String PRESENTED_REFRESH_TOKEN = "Hq7-Lm2_Xv9Tn4Rb1Zp8Cw5Kd3Fs0Ja6Ye2Ui7OgBnM";

    private final SessionRepository sessions = mock(SessionRepository.class);
    private final SessionService service = new SessionService(sessions, Jwts.encoder(), fixedClock());

    @Nested
    class HappyPaths {

        @Test
        void issuesAnAccessTokenCarryingSubjectRoleAndSessionId() {
            when(sessions.insert(any(), any(), any(), anyInt())).thenReturn(SESSION_ID);

            IssuedSession issued = service.issue(USER_ID, ROLE, clientInfo());

            Jwt jwt = decodeIgnoringExpiry(issued.accessToken());
            assertThat(jwt.getSubject()).isEqualTo(USER_ID.toString());
            assertThat(jwt.getClaimAsString("role")).isEqualTo(ROLE);
            assertThat(jwt.getClaimAsString("sid")).isEqualTo(SESSION_ID.toString());
            assertThat(jwt.getHeaders().get("alg")).isEqualTo("HS256");
        }

        @Test
        void stampsTheAccessTokenWithTheClockTimeAndAFifteenMinuteLifetime() {
            when(sessions.insert(any(), any(), any(), anyInt())).thenReturn(SESSION_ID);

            IssuedSession issued = service.issue(USER_ID, ROLE, clientInfo());

            Jwt jwt = decodeIgnoringExpiry(issued.accessToken());
            assertThat(jwt.getIssuedAt()).isEqualTo(NOW);
            assertThat(jwt.getExpiresAt()).isEqualTo(NOW.plusSeconds(ACCESS_TTL_SECONDS));
        }

        @Test
        void givesTheAccessTokenNoScopeSoItGrantsNoSetupAuthority() {
            when(sessions.insert(any(), any(), any(), anyInt())).thenReturn(SESSION_ID);

            Jwt jwt = decodeIgnoringExpiry(service.issue(USER_ID, ROLE, clientInfo()).accessToken());

            assertThat(new JwtGrantedAuthoritiesConverter().convert(jwt)).isEmpty();
        }

        @Test
        void storesOnlyTheSha256OfTheRefreshTokenItHandsToTheClient() {
            when(sessions.insert(any(), any(), any(), anyInt())).thenReturn(SESSION_ID);
            ArgumentCaptor<String> storedHash = ArgumentCaptor.forClass(String.class);

            IssuedSession issued = service.issue(USER_ID, ROLE, clientInfo());

            verify(sessions).insert(eq(USER_ID), storedHash.capture(), eq(clientInfo()), eq(REFRESH_TTL_DAYS));
            assertThat(storedHash.getValue())
                    .isEqualTo(SessionService.sha256(issued.refreshToken()))
                    .isNotEqualTo(issued.refreshToken());
            assertThat(issued.userId()).isEqualTo(USER_ID);
            assertThat(issued.role()).isEqualTo(ROLE);
        }

        @Test
        void rotatesToANewRefreshTokenAndStoresBothHashes() {
            when(sessions.rotate(any(), any())).thenReturn(Optional.of(new RotatedSession(SESSION_ID, USER_ID, ROLE)));
            ArgumentCaptor<String> newHash = ArgumentCaptor.forClass(String.class);

            Optional<IssuedSession> rotated = service.rotate(PRESENTED_REFRESH_TOKEN);

            verify(sessions).rotate(eq(SessionService.sha256(PRESENTED_REFRESH_TOKEN)), newHash.capture());
            assertThat(rotated).isPresent();
            assertThat(rotated.get().refreshToken()).isNotEqualTo(PRESENTED_REFRESH_TOKEN);
            assertThat(newHash.getValue()).isEqualTo(SessionService.sha256(rotated.get().refreshToken()));
        }

        @Test
        void rotatesIntoAnAccessTokenForTheSameSessionAndRoleFromTheDatabaseRow() {
            when(sessions.rotate(any(), any())).thenReturn(Optional.of(new RotatedSession(SESSION_ID, USER_ID, ROLE)));

            IssuedSession rotated = service.rotate(PRESENTED_REFRESH_TOKEN).orElseThrow();

            Jwt jwt = decodeIgnoringExpiry(rotated.accessToken());
            assertThat(jwt.getClaimAsString("sid")).isEqualTo(SESSION_ID.toString());
            assertThat(jwt.getClaimAsString("role")).isEqualTo(ROLE);
            assertThat(jwt.getExpiresAt()).isEqualTo(NOW.plusSeconds(ACCESS_TTL_SECONDS));
        }

        @Test
        void revokesTheSessionWhenAStaleTokenIsReplayedAfterTheGraceWindow() {
            when(sessions.findByPreviousHash(SessionService.sha256(PRESENTED_REFRESH_TOKEN), REFRESH_REUSE_GRACE_SECONDS))
                    .thenReturn(Optional.of(new ReplayedSession(SESSION_ID, USER_ID, false)));

            Optional<ReplayedSession> replay = service.revokeIfReplayed(PRESENTED_REFRESH_TOKEN);

            assertThat(replay).contains(new ReplayedSession(SESSION_ID, USER_ID, false));
            verify(sessions).revoke(SESSION_ID);
        }

        @Test
        void revokesBySha256OfThePresentedTokenOnLogout() {
            service.revokeByRefreshToken(PRESENTED_REFRESH_TOKEN);

            verify(sessions).revokeByHash(SessionService.sha256(PRESENTED_REFRESH_TOKEN));
        }
    }

    @Nested
    class BoundaryValues {

        @Test
        void generatesRefreshTokensOf43UrlSafeCharactersThatNeverRepeat() {
            when(sessions.insert(any(), any(), any(), anyInt())).thenReturn(SESSION_ID);

            long distinct = Stream.generate(() -> service.issue(USER_ID, ROLE, clientInfo()).refreshToken())
                    .limit(200)
                    .peek(token -> assertThat(token).hasSize(43).matches("[A-Za-z0-9_-]+"))
                    .distinct()
                    .count();

            assertThat(distinct).isEqualTo(200);
        }

        @Test
        void hashesTheKnownSha256TestVectors() {
            assertThat(SessionService.sha256("abc"))
                    .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
            assertThat(SessionService.sha256(""))
                    .isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
        }

        @Test
        void usesTheTenSecondGraceWindowWhenLookingForAReplay() {
            service.revokeIfReplayed(PRESENTED_REFRESH_TOKEN);

            verify(sessions).findByPreviousHash(SessionService.sha256(PRESENTED_REFRESH_TOKEN), 10);
        }
    }

    @Nested
    class ErrorPaths {

        @Test
        void returnsNothingAndMintsNoTokenWhenTheRefreshTokenIsUnknownRevokedOrExpired() {
            when(sessions.rotate(any(), any())).thenReturn(Optional.empty());

            assertThat(service.rotate(PRESENTED_REFRESH_TOKEN)).isEmpty();
        }

        @Test
        void doesNotRevokeAReplayInsideTheGraceWindowBecauseItIsATabRace() {
            when(sessions.findByPreviousHash(any(), anyInt()))
                    .thenReturn(Optional.of(new ReplayedSession(SESSION_ID, USER_ID, true)));

            Optional<ReplayedSession> replay = service.revokeIfReplayed(PRESENTED_REFRESH_TOKEN);

            assertThat(replay).isEmpty();
            verify(sessions, never()).revoke(any());
        }

        @Test
        void doesNotRevokeAnythingWhenTheTokenIsNotAStaleOne() {
            when(sessions.findByPreviousHash(any(), anyInt())).thenReturn(Optional.empty());

            Optional<ReplayedSession> replay = service.revokeIfReplayed(PRESENTED_REFRESH_TOKEN);

            assertThat(replay).isEmpty();
            verify(sessions, never()).revoke(any());
        }
    }

    @Nested
    class DependencyFailures {

        @Test
        void propagatesADatabaseTimeoutWhileCreatingTheSession() {
            when(sessions.insert(any(), any(), any(), anyInt()))
                    .thenThrow(new QueryTimeoutException("statement timeout after 5000 ms"));

            assertThatThrownBy(() -> service.issue(USER_ID, ROLE, clientInfo()))
                    .isInstanceOf(QueryTimeoutException.class);
        }

        @Test
        void propagatesADatabaseTimeoutWhileRotating() {
            when(sessions.rotate(any(), any())).thenThrow(new QueryTimeoutException("statement timeout after 5000 ms"));

            assertThatThrownBy(() -> service.rotate(PRESENTED_REFRESH_TOKEN))
                    .isInstanceOf(QueryTimeoutException.class);
        }

        @Test
        void propagatesASigningFailureSoNoHalfIssuedSessionIsReturned() {
            JwtEncoder brokenEncoder = mock(JwtEncoder.class);
            when(brokenEncoder.encode(any())).thenThrow(new JwtEncodingException("Failed to sign the JWT"));
            when(sessions.insert(any(), any(), any(), anyInt())).thenReturn(SESSION_ID);
            SessionService failing = new SessionService(sessions, brokenEncoder, fixedClock());

            assertThatThrownBy(() -> failing.issue(USER_ID, ROLE, clientInfo()))
                    .isInstanceOf(JwtEncodingException.class);
        }
    }
}
