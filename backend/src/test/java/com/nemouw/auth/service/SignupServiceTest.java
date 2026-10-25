package com.nemouw.auth.service;

import com.nemouw.auth.config.PasswordConfig;
import com.nemouw.auth.dto.AuthDtos.ResendOtpRequest;
import com.nemouw.auth.dto.AuthDtos.SetPasswordRequest;
import com.nemouw.auth.dto.AuthDtos.SignupRequest;
import com.nemouw.auth.dto.AuthDtos.SignupResponse;
import com.nemouw.auth.dto.AuthDtos.VerifyOtpRequest;
import com.nemouw.auth.model.IssuedSession;
import com.nemouw.auth.model.IssuedSetupToken;
import com.nemouw.auth.repository.SessionRepository;
import com.nemouw.auth.repository.UserRepository;
import com.nemouw.auth.repository.UserRepository.EmailIdentity;
import com.nemouw.auth.repository.VerificationTokenRepository;
import com.nemouw.auth.repository.VerificationTokenRepository.ClaimedToken;
import com.nemouw.common.enums.LocaleCode;
import com.nemouw.common.enums.UserRole;
import com.nemouw.common.enums.UserStatus;
import com.nemouw.common.enums.VerificationPurpose;
import com.nemouw.common.exception.ErrorCode;
import com.nemouw.common.exception.FieldError;
import com.nemouw.common.exception.FieldErrors;
import com.nemouw.common.exception.TooManyRequestsException;
import com.nemouw.common.exception.ValidationException;
import com.nemouw.common.mail.EmailSender;
import com.nemouw.support.Fixtures.RecordingTransactions;
import com.nemouw.support.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.QueryTimeoutException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static com.nemouw.auth.AuthConstants.OTP_MAX_ATTEMPTS;
import static com.nemouw.common.util.ValidationLimits.NAME_MAX_LENGTH;
import static com.nemouw.support.Fixtures.EMAIL;
import static com.nemouw.support.Fixtures.IDENTIFIER_ID;
import static com.nemouw.support.Fixtures.PASSWORD;
import static com.nemouw.support.Fixtures.SESSION_ID;
import static com.nemouw.support.Fixtures.TOKEN_ID;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SignupServiceTest {

    private static final PasswordHasher REAL_HASHER = new PasswordHasher(new PasswordConfig().passwordEncoder());
    private static final OffsetDateTime OTP_EXPIRES_AT = OffsetDateTime.parse("2026-10-24T09:25:30Z");
    private static final String OTP_CODE = "482913";

    private final UserRepository users = mock(UserRepository.class);
    private final VerificationTokenRepository tokenRepo = mock(VerificationTokenRepository.class);
    private final SessionRepository sessionRepo = mock(SessionRepository.class);
    private final EmailSender emailSender = mock(EmailSender.class);
    private final PasswordHasher hasher = spy(REAL_HASHER);
    private final VerificationTokenService tokenService = new VerificationTokenService(props());
    private final VerificationTokenIssuer issuer = new VerificationTokenIssuer(tokenService, tokenRepo);
    private final SetupTokenService setupTokens = new SetupTokenService(Jwts.encoder(), fixedClock());
    private final SessionService sessions = new SessionService(sessionRepo, Jwts.encoder(), fixedClock());
    private final RecordingTransactions tx = new RecordingTransactions();
    private final SignupService service = new SignupService(users, tokenRepo, hasher, tokenService, issuer,
            setupTokens, sessions, emailSender, tx.template());

    @BeforeEach
    void stubTheWritesASuccessfulSignupMakes() {
        when(users.insertUser(any(), any(), any())).thenReturn(USER_ID);
        when(users.insertEmailIdentifier(any(), any())).thenReturn(IDENTIFIER_ID);
        when(tokenRepo.upsertIfAllowed(any(), any(), any(), any())).thenReturn(Optional.of(OTP_EXPIRES_AT));
        when(sessionRepo.insert(any(), any(), any(), anyInt())).thenReturn(SESSION_ID);
    }

    private static SignupRequest signupRequest() {
        return new SignupRequest("specialist", "Amira", "Haddad", EMAIL, "en");
    }

    private static EmailIdentity identity(String status) {
        return new EmailIdentity(IDENTIFIER_ID, USER_ID, EMAIL, status, "specialist");
    }

    private void accountExists(String status) {
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(identity(status)));
        when(users.findPrimaryEmail(USER_ID)).thenReturn(Optional.of(identity(status)));
    }

    private void lockedStatusIs(UserStatus status) {
        when(users.lockStatus(USER_ID)).thenReturn(Optional.of(status));
    }

    private static List<FieldError> fieldErrorsOf(Throwable t) {
        return ((ValidationException) t).fieldErrors();
    }

    @Nested
    class Signup {

        @Nested
        class HappyPaths {

            @Test
            void registersANewSpecialistAndSendsTheOtpByEmail() {
                SignupResponse response = service.signup(signupRequest());

                assertThat(response).isEqualTo(new SignupResponse(EMAIL, OTP_EXPIRES_AT));
                verify(users).insertUser(UserRole.SPECIALIST, "Amira", "Haddad");
                verify(users).insertEmailIdentifier(USER_ID, EMAIL);
                assertThat(tx.commits()).isEqualTo(1);
            }

            @Test
            void emailsTheSameCodeWhoseHashItStored() {
                ArgumentCaptor<String> emailedCode = ArgumentCaptor.forClass(String.class);
                ArgumentCaptor<String> storedHash = ArgumentCaptor.forClass(String.class);

                service.signup(signupRequest());

                verify(emailSender).sendSignupOtp(eq(EMAIL), emailedCode.capture(), eq(LocaleCode.EN));
                verify(tokenRepo).upsertIfAllowed(eq(USER_ID), eq(IDENTIFIER_ID), eq(VerificationPurpose.SIGNUP_OTP),
                        storedHash.capture());
                assertThat(emailedCode.getValue()).matches("\\d{6}");
                assertThat(storedHash.getValue()).isEqualTo(tokenService.hash(USER_ID, emailedCode.getValue()));
            }

            @Test
            void normalizesTheEmailAndTrimsTheNames() {
                SignupResponse response = service.signup(
                        new SignupRequest("specialist", "  Amira ", " Haddad  ", "  Amira.Haddad@ClinicMail.org ", "en"));

                assertThat(response.email()).isEqualTo(EMAIL);
                verify(users).insertUser(UserRole.SPECIALIST, "Amira", "Haddad");
            }

            @Test
            void emailsInTheRequestedLocaleAndFallsBackToEnglishForAnUnknownOne() {
                service.signup(new SignupRequest("specialist", "Amira", "Haddad", EMAIL, "fr"));
                service.signup(new SignupRequest("specialist", "Amira", "Haddad", EMAIL, "de"));

                verify(emailSender).sendSignupOtp(eq(EMAIL), anyString(), eq(LocaleCode.FR));
                verify(emailSender).sendSignupOtp(eq(EMAIL), anyString(), eq(LocaleCode.EN));
            }

            @Test
            void restartsAPendingSignupWithANewOtpAndTheNameJustTyped() {
                accountExists("pending");
                lockedStatusIs(UserStatus.PENDING);
                when(tokenRepo.secondsUntilAllowed(IDENTIFIER_ID, VerificationPurpose.SIGNUP_OTP)).thenReturn(0);

                SignupResponse response = service.signup(
                        new SignupRequest("specialist", "Layla", "Khoury", EMAIL, "ar"));

                assertThat(response.otpExpiresAt()).isEqualTo(OTP_EXPIRES_AT);
                verify(users).updateName(USER_ID, "Layla", "Khoury");
                verify(users, never()).insertUser(any(), any(), any());
                verify(emailSender).sendSignupOtp(eq(EMAIL), anyString(), eq(LocaleCode.AR));
            }
        }

        @Nested
        class BoundaryValues {

            @Test
            void acceptsNamesOfExactlyTheMaximumLengthInCodePoints() {
                String longName = "\uD83D\uDE00".repeat(NAME_MAX_LENGTH);

                SignupResponse response = service.signup(new SignupRequest("specialist", longName, longName, EMAIL, "en"));

                assertThat(response.email()).isEqualTo(EMAIL);
            }

            @Test
            void rejectsANameOneCharacterOverTheMaximum() {
                String tooLong = "a".repeat(NAME_MAX_LENGTH + 1);

                assertThatThrownBy(() -> service.signup(new SignupRequest("specialist", tooLong, "Haddad", EMAIL, "en")))
                        .isInstanceOf(ValidationException.class)
                        .satisfies(e -> assertThat(fieldErrorsOf(e))
                                .containsExactly(FieldErrors.tooLong("first_name", NAME_MAX_LENGTH)));
            }

            @Test
            void reportsAThrottledRestartWithAtLeastOneSecondToWait() {
                accountExists("pending");
                lockedStatusIs(UserStatus.PENDING);
                when(tokenRepo.secondsUntilAllowed(IDENTIFIER_ID, VerificationPurpose.SIGNUP_OTP)).thenReturn(0, 0);
                when(tokenRepo.upsertIfAllowed(any(), any(), any(), any())).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.signup(signupRequest()))
                        .isInstanceOfSatisfying(TooManyRequestsException.class,
                                e -> assertThat(e.retryAfterSeconds()).isEqualTo(1));
            }
        }

        @Nested
        class ErrorPaths {

            @ParameterizedTest
            @NullSource
            @ValueSource(strings = {"admin", "patient", "syndicate_staff", "Specialist", ""})
            void allowsOnlySpecialistsToSelfRegister(String role) {
                assertThatThrownBy(() -> service.signup(new SignupRequest(role, "Amira", "Haddad", EMAIL, "en")))
                        .satisfies(e -> assertThat(fieldErrorsOf(e)).containsExactly(FieldErrors.invalidRole("role")));

                verifyNoInteractions(users, emailSender);
            }

            @Test
            void reportsEveryInvalidFieldAtOnceInFormOrder() {
                SignupRequest everythingWrong = new SignupRequest("admin", " ", "", "amira.haddad", null);

                assertThatThrownBy(() -> service.signup(everythingWrong))
                        .satisfies(e -> assertThat(fieldErrorsOf(e)).containsExactly(
                                FieldErrors.invalidRole("role"),
                                FieldErrors.required("first_name"),
                                FieldErrors.required("last_name"),
                                FieldErrors.mustBeEmail("email")));
            }

            @ParameterizedTest
            @ValueSource(strings = {"active", "suspended", "deleted"})
            void refusesAnEmailThatBelongsToANonPendingAccount(String status) {
                accountExists(status);

                assertApiError(() -> service.signup(signupRequest()), ErrorCode.EMAIL_TAKEN);

                verify(emailSender, never()).sendSignupOtp(any(), any(), any());
                verify(tokenRepo, never()).upsertIfAllowed(any(), any(), any(), any());
            }

            @Test
            void answers429WithRetryAfterWhileThePendingAccountIsInCooldownAndSendsNothing() {
                accountExists("pending");
                when(tokenRepo.secondsUntilAllowed(IDENTIFIER_ID, VerificationPurpose.SIGNUP_OTP)).thenReturn(37);

                assertThatThrownBy(() -> service.signup(signupRequest()))
                        .isInstanceOfSatisfying(TooManyRequestsException.class,
                                e -> assertThat(e.retryAfterSeconds()).isEqualTo(37));

                verifyNoInteractions(emailSender);
                assertThat(tx.commits()).isZero();
            }

            @Test
            void leavesTheNameUntouchedWhenTheRestartIsThrottledInsideTheTransaction() {
                accountExists("pending");
                lockedStatusIs(UserStatus.PENDING);
                when(tokenRepo.secondsUntilAllowed(IDENTIFIER_ID, VerificationPurpose.SIGNUP_OTP)).thenReturn(0, 12);
                when(tokenRepo.upsertIfAllowed(any(), any(), any(), any())).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.signup(signupRequest()))
                        .isInstanceOfSatisfying(TooManyRequestsException.class,
                                e -> assertThat(e.retryAfterSeconds()).isEqualTo(12));

                verify(users, never()).updateName(any(), any(), any());
                assertThat(tx.rollbacks()).isEqualTo(1);
            }

            @Test
            void refusesWhenThePendingAccountBecameActiveBetweenTheCheckAndTheLock() {
                accountExists("pending");
                lockedStatusIs(UserStatus.ACTIVE);
                when(tokenRepo.secondsUntilAllowed(IDENTIFIER_ID, VerificationPurpose.SIGNUP_OTP)).thenReturn(0);

                assertApiError(() -> service.signup(signupRequest()), ErrorCode.EMAIL_TAKEN);

                verifyNoInteractions(emailSender);
                verify(users, never()).updateName(any(), any(), any());
            }

            @Test
            void reportsEmailTakenWhenTwoSignupsRaceForTheSameEmail() {
                when(users.insertEmailIdentifier(any(), any()))
                        .thenThrow(new DuplicateKeyException("duplicate key value violates uq_user_identifiers"));

                assertApiError(() -> service.signup(signupRequest()), ErrorCode.EMAIL_TAKEN);

                verifyNoInteractions(emailSender);
            }
        }

        @Nested
        class DependencyFailures {

            @Test
            void sendsNoEmailAndRollsBackWhenTheDatabaseTimesOutCreatingTheUser() {
                when(users.insertUser(any(), any(), any())).thenThrow(new QueryTimeoutException("statement timeout"));

                assertThatThrownBy(() -> service.signup(signupRequest())).isInstanceOf(QueryTimeoutException.class);

                verifyNoInteractions(emailSender);
                assertThat(tx.rollbacks()).isEqualTo(1);
            }
        }
    }

    @Nested
    class VerifyOtp {

        private void otpIsStored(String code) {
            when(tokenRepo.claimAttempt(USER_ID, VerificationPurpose.SIGNUP_OTP, OTP_MAX_ATTEMPTS))
                    .thenReturn(Optional.of(new ClaimedToken(TOKEN_ID, tokenService.hash(USER_ID, code))));
        }

        @Test
        void acceptsTheCorrectCodeOnceAndIssuesAPasswordSetupToken() {
            accountExists("pending");
            lockedStatusIs(UserStatus.PENDING);
            otpIsStored(OTP_CODE);

            IssuedSetupToken setup = service.verifyOtp(new VerifyOtpRequest(EMAIL, OTP_CODE));

            assertThat(setup.userId()).isEqualTo(USER_ID);
            assertThat(setup.token()).isNotBlank();
            verify(tokenRepo).delete(TOKEN_ID);
            verify(users).markVerified(IDENTIFIER_ID);
            assertThat(tx.commits()).isEqualTo(1);
        }

        @Test
        void trimsSpacesAroundTheCodeAndNormalizesTheEmail() {
            accountExists("pending");
            lockedStatusIs(UserStatus.PENDING);
            otpIsStored(OTP_CODE);

            IssuedSetupToken setup = service.verifyOtp(new VerifyOtpRequest(" Amira.Haddad@ClinicMail.org ", " 482913 "));

            assertThat(setup.userId()).isEqualTo(USER_ID);
        }

        @ParameterizedTest
        @ValueSource(strings = {"48291", "4829134", "48a913", "\uFF14\uFF18\uFF12\uFF19\uFF11\uFF13"})
        void rejectsACodeThatIsNotExactlySixAsciiDigitsWithoutTouchingTheDatabase(String code) {
            assertThatThrownBy(() -> service.verifyOtp(new VerifyOtpRequest(EMAIL, code)))
                    .satisfies(e -> assertThat(fieldErrorsOf(e))
                            .containsExactly(FieldErrors.invalidFormat("code", "must be 6 digits")));

            verifyNoInteractions(users, tokenRepo);
        }

        @Test
        void reportsMissingEmailAndCodeTogether() {
            assertThatThrownBy(() -> service.verifyOtp(new VerifyOtpRequest(null, null)))
                    .satisfies(e -> assertThat(fieldErrorsOf(e))
                            .containsExactly(FieldErrors.required("email"), FieldErrors.required("code")));
        }

        @Test
        void keepsTheSpentAttemptWhenTheCodeIsWrongBecauseItCommitsInsteadOfRollingBack() {
            accountExists("pending");
            lockedStatusIs(UserStatus.PENDING);
            otpIsStored("482914");

            assertApiError(() -> service.verifyOtp(new VerifyOtpRequest(EMAIL, OTP_CODE)),
                    ErrorCode.INVALID_OR_EXPIRED_TOKEN);

            assertThat(tx.commits()).isEqualTo(1);
            assertThat(tx.rollbacks()).isZero();
            verify(tokenRepo, never()).delete(any());
            verify(users, never()).markVerified(any());
        }

        @Test
        void givesTheSameAnswerWhenNoLiveCodeIsLeftBecauseItExpiredOrAttemptsRanOut() {
            accountExists("pending");
            lockedStatusIs(UserStatus.PENDING);

            assertApiError(() -> service.verifyOtp(new VerifyOtpRequest(EMAIL, OTP_CODE)),
                    ErrorCode.INVALID_OR_EXPIRED_TOKEN);
        }

        @Test
        void givesTheSameAnswerForAnUnknownEmailSoRegistrationsCannotBeProbed() {
            assertApiError(() -> service.verifyOtp(new VerifyOtpRequest(EMAIL, OTP_CODE)),
                    ErrorCode.INVALID_OR_EXPIRED_TOKEN);

            verifyNoInteractions(tokenRepo);
        }

        @ParameterizedTest
        @ValueSource(strings = {"active", "suspended", "deleted"})
        void givesTheSameAnswerForAnAccountThatIsNoLongerPending(String status) {
            accountExists(status);
            lockedStatusIs(UserStatus.valueOf(status.toUpperCase()));

            assertApiError(() -> service.verifyOtp(new VerifyOtpRequest(EMAIL, OTP_CODE)),
                    ErrorCode.INVALID_OR_EXPIRED_TOKEN);

            verify(tokenRepo, never()).claimAttempt(any(), any(), anyInt());
        }

        @Test
        void propagatesADatabaseTimeoutWhileClaimingAnAttemptAndRollsBack() {
            accountExists("pending");
            lockedStatusIs(UserStatus.PENDING);
            when(tokenRepo.claimAttempt(any(), any(), anyInt())).thenThrow(new QueryTimeoutException("statement timeout"));

            assertThatThrownBy(() -> service.verifyOtp(new VerifyOtpRequest(EMAIL, OTP_CODE)))
                    .isInstanceOf(QueryTimeoutException.class);

            assertThat(tx.rollbacks()).isEqualTo(1);
        }
    }

    @Nested
    class ResendOtp {

        @Test
        void sendsAFreshCodeToAPendingAccountInItsLocale() {
            accountExists("pending");
            lockedStatusIs(UserStatus.PENDING);
            ArgumentCaptor<String> emailedCode = ArgumentCaptor.forClass(String.class);

            service.resendOtp(new ResendOtpRequest(EMAIL, "fr"));

            verify(emailSender).sendSignupOtp(eq(EMAIL), emailedCode.capture(), eq(LocaleCode.FR));
            assertThat(emailedCode.getValue()).matches("\\d{6}");
        }

        @Test
        void staysSilentForAnUnknownEmailSoRegistrationsCannotBeProbed() {
            service.resendOtp(new ResendOtpRequest(EMAIL, "en"));

            verifyNoInteractions(emailSender, tokenRepo);
        }

        @Test
        void staysSilentForAnAccountThatIsAlreadyActive() {
            accountExists("active");

            service.resendOtp(new ResendOtpRequest(EMAIL, "en"));

            verifyNoInteractions(emailSender, tokenRepo);
        }

        @Test
        void staysSilentWhenTheAccountStoppedBeingPendingBeforeTheLockWasTaken() {
            accountExists("pending");
            lockedStatusIs(UserStatus.ACTIVE);

            service.resendOtp(new ResendOtpRequest(EMAIL, "en"));

            verifyNoInteractions(emailSender);
        }

        @Test
        void answers429AndSendsNothingWhileThrottled() {
            accountExists("pending");
            lockedStatusIs(UserStatus.PENDING);
            when(tokenRepo.upsertIfAllowed(any(), any(), any(), any())).thenReturn(Optional.empty());
            when(tokenRepo.secondsUntilAllowed(IDENTIFIER_ID, VerificationPurpose.SIGNUP_OTP)).thenReturn(44);

            assertThatThrownBy(() -> service.resendOtp(new ResendOtpRequest(EMAIL, "en")))
                    .isInstanceOfSatisfying(TooManyRequestsException.class,
                            e -> assertThat(e.retryAfterSeconds()).isEqualTo(44));

            verifyNoInteractions(emailSender);
        }

        @Test
        void requiresAValidEmail() {
            assertThatThrownBy(() -> service.resendOtp(new ResendOtpRequest("amira.haddad", "en")))
                    .satisfies(e -> assertThat(fieldErrorsOf(e)).containsExactly(FieldErrors.mustBeEmail("email")));
        }
    }

    @Nested
    class SetPassword {

        @Test
        void activatesThePendingAccountStoresAnArgon2HashAndIssuesASession() {
            accountExists("pending");
            lockedStatusIs(UserStatus.PENDING);
            ArgumentCaptor<String> storedHash = ArgumentCaptor.forClass(String.class);

            IssuedSession session = service.setPassword(USER_ID, new SetPasswordRequest(PASSWORD), clientInfo());

            verify(users).replacePassword(eq(USER_ID), storedHash.capture());
            verify(users).activate(USER_ID);
            verify(users).touchLastLogin(USER_ID);
            assertThat(hasher.verify(PASSWORD, storedHash.getValue())).isTrue();
            assertThat(session.userId()).isEqualTo(USER_ID);
            assertThat(session.role()).isEqualTo("specialist");
            assertThat(tx.commits()).isEqualTo(1);
        }

        @Test
        void rejectsAWeakPasswordBeforeSpendingAnyHashingWork() {
            accountExists("pending");

            assertThatThrownBy(() -> service.setPassword(USER_ID, new SetPasswordRequest("short"), clientInfo()))
                    .satisfies(e -> assertThat(fieldErrorsOf(e)).containsExactly(FieldErrors.tooShort("password", 8)));

            verify(hasher, never()).hash(any());
        }

        @Test
        void rejectsACommonPassword() {
            accountExists("pending");

            assertThatThrownBy(() -> service.setPassword(USER_ID, new SetPasswordRequest("password1234"), clientInfo()))
                    .satisfies(e -> assertThat(fieldErrorsOf(e)).containsExactly(FieldErrors.tooCommon("password")));
        }

        @Test
        void rejectsAPasswordBuiltFromTheEmailLocalPartEvenWithoutItsDot() {
            accountExists("pending");

            assertThatThrownBy(() -> service.setPassword(USER_ID, new SetPasswordRequest("amirahaddad2026!!"), clientInfo()))
                    .satisfies(e -> assertThat(fieldErrorsOf(e))
                            .containsExactly(FieldErrors.containsPersonalInfo("password")));
        }

        @Test
        void requiresAPassword() {
            accountExists("pending");

            assertThatThrownBy(() -> service.setPassword(USER_ID, new SetPasswordRequest(null), clientInfo()))
                    .satisfies(e -> assertThat(fieldErrorsOf(e)).containsExactly(FieldErrors.required("password")));
        }

        @ParameterizedTest
        @ValueSource(strings = {"active", "suspended", "deleted"})
        void refusesASetupTokenForAnAccountThatIsNotPendingAndNeverHashes(String status) {
            accountExists(status);

            assertApiError(() -> service.setPassword(USER_ID, new SetPasswordRequest(PASSWORD), clientInfo()),
                    ErrorCode.INVALID_TOKEN);

            verify(hasher, never()).hash(any());
            verify(users, never()).replacePassword(any(), any());
        }

        @Test
        void refusesASetupTokenForAUserThatNoLongerExists() {
            assertApiError(() -> service.setPassword(USER_ID, new SetPasswordRequest(PASSWORD), clientInfo()),
                    ErrorCode.INVALID_TOKEN);
        }

        @Test
        void makesTheSetupTokenSingleUseBecauseTheSecondOfTwoRacingRequestsSeesAnActiveAccount() {
            accountExists("pending");
            lockedStatusIs(UserStatus.ACTIVE);

            assertApiError(() -> service.setPassword(USER_ID, new SetPasswordRequest(PASSWORD), clientInfo()),
                    ErrorCode.INVALID_TOKEN);

            verify(users, never()).replacePassword(any(), any());
            verify(sessionRepo, never()).insert(any(), any(), any(), anyInt());
            assertThat(tx.rollbacks()).isEqualTo(1);
        }

        @Test
        void issuesNoSessionAndRollsBackWhenTheDatabaseTimesOutStoringThePassword() {
            accountExists("pending");
            lockedStatusIs(UserStatus.PENDING);
            org.mockito.Mockito.doThrow(new QueryTimeoutException("statement timeout"))
                    .when(users).replacePassword(any(), any());

            assertThatThrownBy(() -> service.setPassword(USER_ID, new SetPasswordRequest(PASSWORD), clientInfo()))
                    .isInstanceOf(QueryTimeoutException.class);

            verify(sessionRepo, never()).insert(any(), any(), any(), anyInt());
            assertThat(tx.rollbacks()).isEqualTo(1);
        }
    }
}
