package com.nemouw.user.password;

import com.nemouw.user.identity.TestPasswordHasher;
import com.nemouw.user.session.SessionService;
import com.nemouw.user.token.ConsumedToken;
import com.nemouw.user.password.PasswordDtos.ForgotPasswordRequest;
import com.nemouw.user.password.PasswordDtos.ResetPasswordRequest;
import com.nemouw.user.token.VerificationPurpose;
import com.nemouw.user.identity.PasswordHasher;
import com.nemouw.user.identity.UserRepository;
import com.nemouw.user.identity.UserRepository.EmailIdentity;
import com.nemouw.common.enums.LocaleCode;
import com.nemouw.common.enums.UserStatus;
import com.nemouw.common.exception.ErrorCode;
import com.nemouw.common.exception.FieldError;
import com.nemouw.common.exception.FieldErrors;
import com.nemouw.common.exception.TooManyRequestsException;
import com.nemouw.common.exception.ValidationException;
import com.nemouw.common.mail.EmailSender;
import com.nemouw.support.Fixtures.RecordingTransactions;
import com.nemouw.user.token.VerificationTokens;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.QueryTimeoutException;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static com.nemouw.user.AuthConstants.RESET_TTL_SECONDS;
import static com.nemouw.support.Fixtures.EMAIL;
import static com.nemouw.support.Fixtures.IDENTIFIER_ID;
import static com.nemouw.support.Fixtures.TOKEN_ID;
import static com.nemouw.support.Fixtures.USER_ID;
import static com.nemouw.support.Fixtures.assertApiError;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PasswordResetServiceTest {

    private static final PasswordHasher REAL_HASHER = TestPasswordHasher.real();
    private static final OffsetDateTime TOKEN_EXPIRES_AT = OffsetDateTime.parse("2026-10-24T10:15:30Z");
    private static final String RESET_TOKEN = "Zx9-Kq2_mT7vLb4NwR1pYc8HdJ5sUe0AfG3iOa6BkQ";
    private static final String NEW_PASSWORD = "mauve otter lantern quartz";

    private final UserRepository users = mock(UserRepository.class);
    private final SessionService sessions = mock(SessionService.class);
    private final EmailSender emailSender = mock(EmailSender.class);
    private final VerificationTokens tokens = mock(VerificationTokens.class);
    private final PasswordHasher hasher = spy(REAL_HASHER);
    private final RecordingTransactions tx = new RecordingTransactions();
    private final PasswordResetService service = new PasswordResetService(
            users, hasher, tokens, sessions, emailSender, tx.template());

    private void accountExists(String status) {
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(
                new EmailIdentity(IDENTIFIER_ID, USER_ID, EMAIL, status, "specialist")));
    }

    private void resetLinkIsLive() {
        when(tokens.findLiveResetEmail(RESET_TOKEN)).thenReturn(Optional.of(EMAIL));
    }

    private void tokenCanBeConsumed(UserStatus status) {
        when(tokens.consumeResetToken(RESET_TOKEN))
                .thenReturn(Optional.of(new ConsumedToken(TOKEN_ID, USER_ID, IDENTIFIER_ID)));
        when(users.lockStatus(USER_ID)).thenReturn(Optional.of(status));
    }

    private static List<FieldError> fieldErrorsOf(Throwable t) {
        return ((ValidationException) t).fieldErrors();
    }

    @Nested
    class ForgotPassword {

        @ParameterizedTest
        @ValueSource(strings = {"active", "pending"})
        void emailsAResetLinkForActiveAndPendingAccounts(String status) {
            accountExists(status);
            when(tokens.issueResetToken(USER_ID, IDENTIFIER_ID))
                    .thenReturn(Optional.of(new VerificationTokens.IssuedResetToken(RESET_TOKEN, TOKEN_EXPIRES_AT)));

            service.forgotPassword(new ForgotPasswordRequest(EMAIL, "en"));

            verify(tokens).issueResetToken(USER_ID, IDENTIFIER_ID);
            verify(emailSender).sendPasswordReset(EMAIL, RESET_TOKEN,
                    Duration.ofSeconds(RESET_TTL_SECONDS), LocaleCode.EN);
            assertThat(tx.commits()).isEqualTo(1);
        }

        @Test
        void normalizesTheEmailAndUsesTheRequestedLocale() {
            accountExists("active");
            when(tokens.issueResetToken(USER_ID, IDENTIFIER_ID))
                    .thenReturn(Optional.of(new VerificationTokens.IssuedResetToken(RESET_TOKEN, TOKEN_EXPIRES_AT)));

            service.forgotPassword(new ForgotPasswordRequest("  Amira.Haddad@ClinicMail.org ", "ar"));

            verify(emailSender).sendPasswordReset(EMAIL, RESET_TOKEN,
                    Duration.ofSeconds(RESET_TTL_SECONDS), LocaleCode.AR);
        }

        @Test
        void fallsBackToEnglishWhenNoLocaleIsGiven() {
            accountExists("active");
            when(tokens.issueResetToken(USER_ID, IDENTIFIER_ID))
                    .thenReturn(Optional.of(new VerificationTokens.IssuedResetToken(RESET_TOKEN, TOKEN_EXPIRES_AT)));

            service.forgotPassword(new ForgotPasswordRequest(EMAIL, null));

            verify(emailSender).sendPasswordReset(EMAIL, RESET_TOKEN,
                    Duration.ofSeconds(RESET_TTL_SECONDS), LocaleCode.EN);
        }

        @Test
        void staysSilentForAnUnknownEmailSoRegistrationsCannotBeProbed() {
            service.forgotPassword(new ForgotPasswordRequest(EMAIL, "en"));
            verifyNoInteractions(tokens, emailSender);
        }

        @ParameterizedTest
        @ValueSource(strings = {"suspended", "deleted"})
        void staysSilentForSuspendedAndDeletedAccounts(String status) {
            accountExists(status);
            service.forgotPassword(new ForgotPasswordRequest(EMAIL, "en"));
            verifyNoInteractions(tokens, emailSender);
        }

        @Test
        void answers429WithTheRemainingWaitAndSendsNothingWhileInCooldown() {
            accountExists("active");
            when(tokens.issueResetToken(USER_ID, IDENTIFIER_ID)).thenReturn(Optional.empty());
            when(tokens.secondsUntilAllowed(IDENTIFIER_ID, VerificationPurpose.PASSWORD_RESET)).thenReturn(41);

            assertThatThrownBy(() -> service.forgotPassword(new ForgotPasswordRequest(EMAIL, "en")))
                    .isInstanceOfSatisfying(TooManyRequestsException.class,
                            e -> assertThat(e.retryAfterSeconds()).isEqualTo(41));

            verifyNoInteractions(emailSender);
        }

        @Test
        void tellsTheClientToWaitAtLeastOneSecondEvenWhenTheCooldownJustEnded() {
            accountExists("active");
            when(tokens.issueResetToken(USER_ID, IDENTIFIER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.forgotPassword(new ForgotPasswordRequest(EMAIL, "en")))
                    .isInstanceOfSatisfying(TooManyRequestsException.class,
                            e -> assertThat(e.retryAfterSeconds()).isEqualTo(1));
        }

        @Test
        void rejectsAMalformedOrMissingEmailBeforeTouchingTheDatabase() {
            assertThatThrownBy(() -> service.forgotPassword(new ForgotPasswordRequest("amira.haddad", "en")))
                    .satisfies(e -> assertThat(fieldErrorsOf(e)).containsExactly(FieldErrors.mustBeEmail("email")));
            assertThatThrownBy(() -> service.forgotPassword(new ForgotPasswordRequest(null, "en")))
                    .satisfies(e -> assertThat(fieldErrorsOf(e)).containsExactly(FieldErrors.required("email")));

            verifyNoInteractions(users, tokens, emailSender);
        }

        @Test
        void sendsNoEmailWhenTheDatabaseTimesOutStoringTheToken() {
            accountExists("active");
            when(tokens.issueResetToken(USER_ID, IDENTIFIER_ID))
                    .thenThrow(new QueryTimeoutException("statement timeout"));

            assertThatThrownBy(() -> service.forgotPassword(new ForgotPasswordRequest(EMAIL, "en")))
                    .isInstanceOf(QueryTimeoutException.class);

            verifyNoInteractions(emailSender);
            assertThat(tx.rollbacks()).isEqualTo(1);
        }
    }

    @Nested
    class ResetPassword {
        @Nested
        class HappyPaths {

            @Test
            void replacesThePasswordWithAnArgon2HashOfTheNewOne() {
                resetLinkIsLive();
                tokenCanBeConsumed(UserStatus.ACTIVE);
                ArgumentCaptor<String> storedHash = ArgumentCaptor.forClass(String.class);

                service.resetPassword(new ResetPasswordRequest(RESET_TOKEN, NEW_PASSWORD));

                verify(users).replacePassword(eq(USER_ID), storedHash.capture());
                assertThat(hasher.verify(NEW_PASSWORD, storedHash.getValue())).isTrue();
                assertThat(tx.commits()).isEqualTo(1);
            }

            @Test
            void signsEverySessionOutAndDropsStaleSignupCodes() {
                resetLinkIsLive();
                tokenCanBeConsumed(UserStatus.ACTIVE);

                service.resetPassword(new ResetPasswordRequest(RESET_TOKEN, NEW_PASSWORD));

                verify(sessions).revokeAllForUser(USER_ID);
                verify(tokens).revoke(USER_ID, VerificationPurpose.SIGNUP_OTP);
            }

            @Test
            void doesNotReactivateAnAccountThatIsAlreadyActive() {
                resetLinkIsLive();
                tokenCanBeConsumed(UserStatus.ACTIVE);

                service.resetPassword(new ResetPasswordRequest(RESET_TOKEN, NEW_PASSWORD));

                verify(users, never()).activate(any());
                verify(users, never()).activate(any(), any());
            }

            @Test
            void activatesAPendingAccountBecauseTheResetLinkProvedMailboxOwnership() {
                resetLinkIsLive();
                tokenCanBeConsumed(UserStatus.PENDING);

                service.resetPassword(new ResetPasswordRequest(RESET_TOKEN, NEW_PASSWORD));

                verify(users).activate(USER_ID, IDENTIFIER_ID);
            }

            @Test
            void trimsTheTokenBeforeLookingItUpAndConsumingIt() {
                String padded = "  " + RESET_TOKEN + "\n";
                when(tokens.findLiveResetEmail(RESET_TOKEN)).thenReturn(Optional.of(EMAIL));
                tokenCanBeConsumed(UserStatus.ACTIVE);

                service.resetPassword(new ResetPasswordRequest(padded, NEW_PASSWORD));

                verify(tokens).findLiveResetEmail(RESET_TOKEN);
                verify(tokens).consumeResetToken(RESET_TOKEN);
                verify(users).replacePassword(eq(USER_ID), anyString());
            }
        }

        @Nested
        class ErrorPaths {

            @Test
            void rejectsAnUnknownOrExpiredTokenBeforeAnyPasswordHashingOrWrites() {
                assertApiError(() -> service.resetPassword(new ResetPasswordRequest(RESET_TOKEN, NEW_PASSWORD)),
                        ErrorCode.INVALID_OR_EXPIRED_TOKEN);

                verify(hasher, never()).hash(any());
                verifyNoInteractions(users, sessions);
                verify(tokens, never()).consumeResetToken(anyString());
            }

            @Test
            void reportsBothMissingFields() {
                assertThatThrownBy(() -> service.resetPassword(new ResetPasswordRequest(" ", null)))
                        .satisfies(e -> assertThat(fieldErrorsOf(e))
                                .containsExactly(FieldErrors.required("token"), FieldErrors.required("new_password")));
            }

            @Test
            void rejectsAWeakNewPasswordWithoutConsumingTheTokenSoTheLinkStaysUsable() {
                resetLinkIsLive();

                assertThatThrownBy(() -> service.resetPassword(new ResetPasswordRequest(RESET_TOKEN, "short")))
                        .satisfies(e -> assertThat(fieldErrorsOf(e))
                                .containsExactly(FieldErrors.tooShort("new_password", 8)));

                verify(tokens, never()).consumeResetToken(anyString());
            }

            @Test
            void rejectsANewPasswordBuiltFromTheAccountsEmailLocalPart() {
                resetLinkIsLive();

                assertThatThrownBy(() -> service.resetPassword(new ResetPasswordRequest(RESET_TOKEN, "AmiraHaddad-2026")))
                        .satisfies(e -> assertThat(fieldErrorsOf(e))
                                .containsExactly(FieldErrors.containsPersonalInfo("new_password")));

                verify(tokens, never()).consumeResetToken(anyString());
            }

            @Test
            void rejectsATokenThatWasConsumedByAParallelRequestAndRollsBack() {
                resetLinkIsLive();

                assertApiError(() -> service.resetPassword(new ResetPasswordRequest(RESET_TOKEN, NEW_PASSWORD)),
                        ErrorCode.INVALID_OR_EXPIRED_TOKEN);

                verify(users, never()).replacePassword(any(), any());
                assertThat(tx.rollbacks()).isEqualTo(1);
            }

            @ParameterizedTest
            @ValueSource(strings = {"SUSPENDED", "DELETED"})
            void refusesToResetASuspendedOrDeletedAccountAndRollsTheTokenDeletionBack(String status) {
                resetLinkIsLive();
                tokenCanBeConsumed(UserStatus.valueOf(status));

                assertApiError(() -> service.resetPassword(new ResetPasswordRequest(RESET_TOKEN, NEW_PASSWORD)),
                        ErrorCode.INVALID_OR_EXPIRED_TOKEN);

                verify(users, never()).replacePassword(any(), any());
                verify(sessions, never()).revokeAllForUser(any());
                assertThat(tx.rollbacks()).isEqualTo(1);
            }

            @Test
            void refusesWhenTheUserRowNoLongerExists() {
                resetLinkIsLive();
                when(tokens.consumeResetToken(RESET_TOKEN))
                        .thenReturn(Optional.of(new ConsumedToken(TOKEN_ID, USER_ID, IDENTIFIER_ID)));

                assertApiError(() -> service.resetPassword(new ResetPasswordRequest(RESET_TOKEN, NEW_PASSWORD)),
                        ErrorCode.INVALID_OR_EXPIRED_TOKEN);

                verify(users, never()).replacePassword(any(), any());
            }
        }

        @Nested
        class DependencyFailures {

            @Test
            void rollsEverythingBackWhenSessionsCannotBeRevoked() {
                resetLinkIsLive();
                tokenCanBeConsumed(UserStatus.ACTIVE);
                doThrow(new QueryTimeoutException("statement timeout"))
                        .when(sessions).revokeAllForUser(any());

                assertThatThrownBy(() -> service.resetPassword(new ResetPasswordRequest(RESET_TOKEN, NEW_PASSWORD)))
                        .isInstanceOf(QueryTimeoutException.class);

                assertThat(tx.rollbacks()).isEqualTo(1);
                assertThat(tx.commits()).isZero();
            }

            @Test
            void propagatesATimeoutWhileLookingUpTheToken() {
                when(tokens.findLiveResetEmail(RESET_TOKEN))
                        .thenThrow(new QueryTimeoutException("statement timeout"));

                assertThatThrownBy(() -> service.resetPassword(new ResetPasswordRequest(RESET_TOKEN, NEW_PASSWORD)))
                        .isInstanceOf(QueryTimeoutException.class);

                verify(hasher, never()).hash(any());
            }
        }
    }
}
