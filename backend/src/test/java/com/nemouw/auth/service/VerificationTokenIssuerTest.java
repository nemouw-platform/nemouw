package com.nemouw.auth.service;

import com.nemouw.auth.repository.VerificationTokenRepository;
import com.nemouw.auth.service.VerificationTokenIssuer.IssuedOtp;
import com.nemouw.auth.service.VerificationTokenIssuer.IssuedResetToken;
import com.nemouw.common.enums.VerificationPurpose;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.QueryTimeoutException;

import java.time.OffsetDateTime;
import java.util.Optional;

import static com.nemouw.support.Fixtures.IDENTIFIER_ID;
import static com.nemouw.support.Fixtures.USER_ID;
import static com.nemouw.support.Fixtures.props;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VerificationTokenIssuerTest {

    private static final OffsetDateTime EXPIRES_AT = OffsetDateTime.parse("2026-10-24T09:25:30Z");

    private final VerificationTokenService tokenService = new VerificationTokenService(props());
    private final VerificationTokenRepository tokens = mock(VerificationTokenRepository.class);
    private final VerificationTokenIssuer issuer = new VerificationTokenIssuer(tokenService, tokens);

    @Nested
    class HappyPaths {

        @Test
        void issuesASixDigitOtpAndStoresOnlyItsHash() {
            when(tokens.upsertIfAllowed(eq(USER_ID), eq(IDENTIFIER_ID), eq(VerificationPurpose.SIGNUP_OTP), any()))
                    .thenReturn(Optional.of(EXPIRES_AT));
            ArgumentCaptor<String> storedHash = ArgumentCaptor.forClass(String.class);

            Optional<IssuedOtp> issued = issuer.issueOtp(USER_ID, IDENTIFIER_ID, VerificationPurpose.SIGNUP_OTP);

            verify(tokens).upsertIfAllowed(eq(USER_ID), eq(IDENTIFIER_ID), eq(VerificationPurpose.SIGNUP_OTP),
                    storedHash.capture());
            assertThat(issued).isPresent();
            assertThat(issued.get().code()).matches("\\d{6}");
            assertThat(issued.get().expiresAt()).isEqualTo(EXPIRES_AT);
            assertThat(storedHash.getValue())
                    .isEqualTo(tokenService.hash(USER_ID, issued.get().code()))
                    .isNotEqualTo(issued.get().code());
        }

        @Test
        void issuesAResetTokenAndStoresOnlyItsHashForThePasswordResetPurpose() {
            when(tokens.upsertIfAllowed(eq(USER_ID), eq(IDENTIFIER_ID), eq(VerificationPurpose.PASSWORD_RESET), any()))
                    .thenReturn(Optional.of(EXPIRES_AT));
            ArgumentCaptor<String> storedHash = ArgumentCaptor.forClass(String.class);

            Optional<IssuedResetToken> issued = issuer.issueResetToken(USER_ID, IDENTIFIER_ID);

            verify(tokens).upsertIfAllowed(eq(USER_ID), eq(IDENTIFIER_ID), eq(VerificationPurpose.PASSWORD_RESET),
                    storedHash.capture());
            assertThat(issued).isPresent();
            assertThat(issued.get().token()).hasSize(43);
            assertThat(issued.get().expiresAt()).isEqualTo(EXPIRES_AT);
            assertThat(storedHash.getValue()).isEqualTo(tokenService.hashToken(issued.get().token()));
        }

        @Test
        void reportsHowLongUntilANewSendIsAllowed() {
            when(tokens.secondsUntilAllowed(IDENTIFIER_ID, VerificationPurpose.SIGNUP_OTP)).thenReturn(42);

            assertThat(issuer.secondsUntilAllowed(IDENTIFIER_ID, VerificationPurpose.SIGNUP_OTP)).isEqualTo(42);
        }
    }

    @Nested
    class BoundaryValues {

        @ParameterizedTest
        @CsvSource({"-5, 0", "0, 0", "1, 1"})
        void neverReportsANegativeWait(int fromRepository, int expected) {
            when(tokens.secondsUntilAllowed(IDENTIFIER_ID, VerificationPurpose.PASSWORD_RESET))
                    .thenReturn(fromRepository);

            assertThat(issuer.secondsUntilAllowed(IDENTIFIER_ID, VerificationPurpose.PASSWORD_RESET))
                    .isEqualTo(expected);
        }
    }

    @Nested
    class ErrorPaths {

        @Test
        void returnsNothingWhenTheRepositoryThrottlesAnOtp() {
            when(tokens.upsertIfAllowed(any(), any(), any(), any())).thenReturn(Optional.empty());

            assertThat(issuer.issueOtp(USER_ID, IDENTIFIER_ID, VerificationPurpose.SIGNUP_OTP)).isEmpty();
        }

        @Test
        void returnsNothingWhenTheRepositoryThrottlesAResetToken() {
            when(tokens.upsertIfAllowed(any(), any(), any(), any())).thenReturn(Optional.empty());

            assertThat(issuer.issueResetToken(USER_ID, IDENTIFIER_ID)).isEmpty();
        }
    }

    @Nested
    class DependencyFailures {

        @Test
        void propagatesADatabaseTimeoutWhileIssuingSoNoCodeIsHandedOutForAnUnsavedHash() {
            when(tokens.upsertIfAllowed(any(), any(), any(), any()))
                    .thenThrow(new QueryTimeoutException("statement timeout after 5000 ms"));

            assertThatThrownBy(() -> issuer.issueOtp(USER_ID, IDENTIFIER_ID, VerificationPurpose.SIGNUP_OTP))
                    .isInstanceOf(QueryTimeoutException.class);
        }

        @Test
        void propagatesADatabaseTimeoutWhileCheckingTheWait() {
            when(tokens.secondsUntilAllowed(any(), any()))
                    .thenThrow(new QueryTimeoutException("statement timeout after 5000 ms"));

            assertThatThrownBy(() -> issuer.secondsUntilAllowed(IDENTIFIER_ID, VerificationPurpose.SIGNUP_OTP))
                    .isInstanceOf(QueryTimeoutException.class);
        }
    }
}
