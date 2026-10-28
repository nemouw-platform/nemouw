package com.nemouw.user.session;

import com.nemouw.support.ShippedProperties;
import com.nemouw.user.identity.TestPasswordHasher;
import com.nemouw.user.session.SessionDtos.LoginRequest;
import com.nemouw.user.identity.PasswordHasher;
import com.nemouw.user.session.SessionRepository.ReplayedSession;
import com.nemouw.user.session.SessionRepository.RotatedSession;
import com.nemouw.user.identity.UserRepository;
import com.nemouw.user.identity.UserRepository.LoginRow;
import com.nemouw.common.exception.ErrorCode;
import com.nemouw.common.exception.FieldErrors;
import com.nemouw.common.exception.ValidationException;
import com.nemouw.support.Fixtures.RecordingTransactions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;

import java.util.Optional;

import static com.nemouw.support.Fixtures.EMAIL;
import static com.nemouw.support.Fixtures.PASSWORD;
import static com.nemouw.support.Fixtures.SESSION_ID;
import static com.nemouw.support.Fixtures.USER_ID;
import static com.nemouw.support.Fixtures.assertApiError;
import static com.nemouw.support.Fixtures.clientInfo;
import static com.nemouw.support.Fixtures.fixedClock;
import static com.nemouw.support.Fixtures.props;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private static final String REFRESH_TOKEN = "Hq7-Lm2_Xv9Tn4Rb1Zp8Cw5Kd3Fs0Ja6Ye2Ui7OgBnM";

    /** One real Argon2 hasher for the whole class: it hashes a dummy on construction, which is slow by design. */
    private static final PasswordHasher REAL_HASHER = TestPasswordHasher.real();
    private static final String PASSWORD_HASH = REAL_HASHER.hash(PASSWORD);

    private final UserRepository users = mock(UserRepository.class);
    private final SessionRepository sessionRepo = mock(SessionRepository.class);
    private final PasswordHasher hasher = spy(REAL_HASHER);
    private final RecordingTransactions tx = new RecordingTransactions();
    private final SessionService sessions = new SessionService(props(),
            ShippedProperties.bind("app.user.session", SessionProperties.class),
            sessionRepo, Jwts.encoder(), fixedClock());
    private final AuthService service = new AuthService(users, hasher, sessions, tx.template());

    private static LoginRow loginRow(String status, String hash) {
        return new LoginRow(USER_ID, status, "specialist", hash);
    }

    private void accountExists(String status, String hash) {
        when(users.findLoginByIdentifier(EMAIL)).thenReturn(Optional.of(loginRow(status, hash)));
        when(sessionRepo.insert(any(), any(), any(), anyInt())).thenReturn(SESSION_ID);
    }

    @Nested
    class Login {

        @Nested
        class HappyPaths {

            @Test
            void logsInAnActiveAccountWithTheCorrectPasswordAndIssuesASession() {
                accountExists("active", PASSWORD_HASH);

                IssuedSession session = service.login(new LoginRequest(EMAIL, PASSWORD), clientInfo());

                assertThat(session.userId()).isEqualTo(USER_ID);
                assertThat(session.role()).isEqualTo("specialist");
                assertThat(session.accessToken()).isNotBlank();
                assertThat(session.refreshToken()).hasSize(43);
                verify(sessionRepo).insert(eq(USER_ID), anyString(), eq(clientInfo()), eq(props().security().refreshTtlDays()));
            }

            @Test
            void recordsTheLoginTimeAndCommitsTheTransaction() {
                accountExists("active", PASSWORD_HASH);

                service.login(new LoginRequest(EMAIL, PASSWORD), clientInfo());

                verify(users).touchLastLogin(USER_ID);
                assertThat(tx.commits()).isEqualTo(1);
                assertThat(tx.rollbacks()).isZero();
            }

            @Test
            void looksTheAccountUpByTheTrimmedLowercasedIdentifier() {
                accountExists("active", PASSWORD_HASH);

                service.login(new LoginRequest("  Amira.Haddad@ClinicMail.org ", PASSWORD), clientInfo());

                verify(users).findLoginByIdentifier("amira.haddad@clinicmail.org");
            }
        }

        @Nested
        class ErrorPaths {

            @Test
            void rejectsAWrongPasswordWithInvalidCredentialsAndIssuesNothing() {
                accountExists("active", PASSWORD_HASH);

                assertApiError(() -> service.login(new LoginRequest(EMAIL, PASSWORD + "x"), clientInfo()),
                        ErrorCode.INVALID_CREDENTIALS);

                verify(users, never()).touchLastLogin(any());
                verify(sessionRepo, never()).insert(any(), any(), any(), anyInt());
            }

            @Test
            void rejectsAnUnknownAccountWithTheSameErrorAndBurnsTheSameHashingTime() {
                assertApiError(() -> service.login(new LoginRequest(EMAIL, PASSWORD), clientInfo()),
                        ErrorCode.INVALID_CREDENTIALS);

                verify(hasher).verifyDummy(PASSWORD);
            }

            @Test
            void treatsAnAccountThatNeverChoseAPasswordLikeAnUnknownAccount() {
                accountExists("pending", null);

                assertApiError(() -> service.login(new LoginRequest(EMAIL, PASSWORD), clientInfo()),
                        ErrorCode.INVALID_CREDENTIALS);

                verify(hasher).verifyDummy(PASSWORD);
            }

            @Test
            void doesNotBurnTheDummyHashWhenARealHashWasChecked() {
                accountExists("active", PASSWORD_HASH);

                assertApiError(() -> service.login(new LoginRequest(EMAIL, PASSWORD + "x"), clientInfo()),
                        ErrorCode.INVALID_CREDENTIALS);

                verify(hasher, never()).verifyDummy(any());
            }

            @ParameterizedTest
            @CsvSource({"pending, ACCOUNT_NOT_VERIFIED", "suspended, ACCOUNT_SUSPENDED", "deleted, INVALID_CREDENTIALS"})
            void blocksANonActiveAccountOnlyAfterTheCorrectPasswordWasGiven(String status, ErrorCode expected) {
                accountExists(status, PASSWORD_HASH);

                assertApiError(() -> service.login(new LoginRequest(EMAIL, PASSWORD), clientInfo()), expected);

                verify(sessionRepo, never()).insert(any(), any(), any(), anyInt());
            }

            @ParameterizedTest
            @ValueSource(strings = {"pending", "suspended", "deleted"})
            void doesNotRevealTheAccountStatusToSomeoneWhoGotThePasswordWrong(String status) {
                accountExists(status, PASSWORD_HASH);

                assertApiError(() -> service.login(new LoginRequest(EMAIL, PASSWORD + "x"), clientInfo()),
                        ErrorCode.INVALID_CREDENTIALS);
            }

            @Test
            void reportsEveryMissingFieldAndDoesNotTouchTheDatabase() {
                assertThatThrownBy(() -> service.login(new LoginRequest("   ", null), clientInfo()))
                        .isInstanceOfSatisfying(ValidationException.class, e -> assertThat(e.fieldErrors())
                                .containsExactly(FieldErrors.required("identifier"), FieldErrors.required("password")));

                verifyNoInteractions(users);
            }
        }

        @Nested
        class DependencyFailures {

            @Test
            void propagatesADatabaseTimeoutWhileLoadingTheAccount() {
                when(users.findLoginByIdentifier(any())).thenThrow(new QueryTimeoutException("statement timeout"));

                assertThatThrownBy(() -> service.login(new LoginRequest(EMAIL, PASSWORD), clientInfo()))
                        .isInstanceOf(QueryTimeoutException.class);

                verify(sessionRepo, never()).insert(any(), any(), any(), anyInt());
            }

            @Test
            void rollsBackTheLoginTimeWhenTheSessionCannotBeStored() {
                accountExists("active", PASSWORD_HASH);
                when(sessionRepo.insert(any(), any(), any(), anyInt()))
                        .thenThrow(new DataAccessResourceFailureException("connection reset by peer"));

                assertThatThrownBy(() -> service.login(new LoginRequest(EMAIL, PASSWORD), clientInfo()))
                        .isInstanceOf(DataAccessResourceFailureException.class);

                assertThat(tx.rollbacks()).isEqualTo(1);
                assertThat(tx.commits()).isZero();
            }
        }
    }

    @Nested
    class Refresh {

        @Test
        void rotatesAValidRefreshTokenIntoANewSession() {
            when(sessionRepo.rotate(any(), any())).thenReturn(Optional.of(new RotatedSession(SESSION_ID, USER_ID, "specialist")));

            IssuedSession rotated = service.refresh(REFRESH_TOKEN);

            assertThat(rotated.userId()).isEqualTo(USER_ID);
            assertThat(rotated.refreshToken()).isNotEqualTo(REFRESH_TOKEN);
            assertThat(tx.commits()).isEqualTo(1);
        }

        @Test
        void trimsTheTokenBeforeLookingItUp() {
            when(sessionRepo.rotate(any(), any())).thenReturn(Optional.of(new RotatedSession(SESSION_ID, USER_ID, "specialist")));

            service.refresh("  " + REFRESH_TOKEN + "\n");

            verify(sessionRepo).rotate(eq(SessionService.sha256(REFRESH_TOKEN)), anyString());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        void treatsAMissingOrBlankTokenAsNotAuthenticatedWithoutTouchingTheDatabase(String token) {
            assertApiError(() -> service.refresh(token), ErrorCode.INVALID_TOKEN);

            verifyNoInteractions(sessionRepo);
        }

        @Test
        void rejectsAnUnknownTokenWithoutRevokingAnything() {
            assertApiError(() -> service.refresh(REFRESH_TOKEN), ErrorCode.INVALID_TOKEN);

            verify(sessionRepo, never()).revoke(any());
        }

        @Test
        void revokesTheSessionWhenAStaleTokenIsReplayedOutsideTheGraceWindowAndStillKeepsTheRevocation() {
            when(sessionRepo.findByPreviousHash(any(), anyInt()))
                    .thenReturn(Optional.of(new ReplayedSession(SESSION_ID, USER_ID, false)));

            assertApiError(() -> service.refresh(REFRESH_TOKEN), ErrorCode.INVALID_TOKEN);

            verify(sessionRepo).revoke(SESSION_ID);
            assertThat(tx.commits()).as("the revoke must commit even though the request fails").isEqualTo(1);
            assertThat(tx.rollbacks()).isZero();
        }

        @Test
        void rejectsAReplayInsideTheGraceWindowWithoutRevokingTheSession() {
            when(sessionRepo.findByPreviousHash(any(), anyInt()))
                    .thenReturn(Optional.of(new ReplayedSession(SESSION_ID, USER_ID, true)));

            assertApiError(() -> service.refresh(REFRESH_TOKEN), ErrorCode.INVALID_TOKEN);

            verify(sessionRepo, never()).revoke(any());
        }

        @Test
        void propagatesADatabaseTimeoutAndRollsBack() {
            when(sessionRepo.rotate(any(), any())).thenThrow(new QueryTimeoutException("statement timeout"));

            assertThatThrownBy(() -> service.refresh(REFRESH_TOKEN)).isInstanceOf(QueryTimeoutException.class);

            assertThat(tx.rollbacks()).isEqualTo(1);
        }
    }

    @Nested
    class Logout {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void doesNothingWhenThereIsNoRefreshCookie(String token) {
            service.logout(token);

            verifyNoInteractions(sessionRepo);
        }

        @Test
        void revokesTheSessionOfTheTrimmedRefreshToken() {
            service.logout(" " + REFRESH_TOKEN + " ");

            verify(sessionRepo).revokeByHash(SessionService.sha256(REFRESH_TOKEN));
        }

        @Test
        void propagatesADatabaseTimeoutSoTheClientKnowsTheLogoutDidNotHappen() {
            doThrow(new QueryTimeoutException("statement timeout"))
                    .when(sessionRepo).revokeByHash(any());

            assertThatThrownBy(() -> service.logout(REFRESH_TOKEN)).isInstanceOf(QueryTimeoutException.class);
        }
    }
}