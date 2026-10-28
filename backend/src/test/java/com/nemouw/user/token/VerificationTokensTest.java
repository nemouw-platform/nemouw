package com.nemouw.user.token;

import com.nemouw.user.token.VerificationTokenRepository.ClaimedToken;
import com.nemouw.common.config.AppProperties;
import com.nemouw.support.ShippedProperties;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.QueryTimeoutException;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static com.nemouw.support.Fixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class VerificationTokensTest {

    private static final TokenProperties TOKEN_PROPS =
            ShippedProperties.bind("app.user.token", TokenProperties.class);
    private static final int OTP_MAX_ATTEMPTS = TOKEN_PROPS.maxOtpAttempts();
    private static final int OTP_CODE_DIGITS = VerificationTokens.OTP_CODE_DIGITS;
    private static final int RESET_TOKEN_BYTES = 32;

    private static final String CODE = "482913";
    private static final String RESET_TOKEN =
            "Zx9-Kq2_mT7vLb4NwR1pYc8HdJ5sUe0AfG3iOa6BkQ";

    // Independently calculated HMAC-SHA256 test vectors.
    private static final String EXPECTED_CODE_HASH =
            "c079310d86c90c156986f8397779eba2f1f896ceee527266a710fa5970fba054";

    private static final String EXPECTED_NEXT_CODE_HASH =
            "f0cda239a3c849c9ff22c276ca16262c9a79ecd616e0527c0daba11ef5d7bf6e";

    private static final String EXPECTED_TOKEN_HASH =
            "d13c7402d2fef0ca4d3667020a3509ee662d63f2b0a84027d607986ece9715d0";

    private static final String EXPECTED_CODE_HASH_WITH_OTHER_SECRET =
            "66d06b196a525e2a8830a4b211313172c40b2ebe20373cf38592d15d591eb663";

    private static final OffsetDateTime EXPIRES =
            OffsetDateTime.parse("2026-10-24T09:25:30Z");

    private final VerificationTokenRepository repo =
            mock(VerificationTokenRepository.class);

    private final VerificationTokens service =
            new VerificationTokens(repo, TOKEN_PROPS, props());

    private static VerificationTokens withSecret(
            VerificationTokenRepository repository,
            String secret
    ) {
        AppProperties base = props();

        AppProperties.Security security = new AppProperties.Security(
                base.security().jwtSecret(),
                secret,
                base.security().cookieSecure(),
                base.security().trustProxy(),
                base.security().accessTtlSeconds(),
                base.security().refreshTtlDays()
        );

        return new VerificationTokens(
                repository,
                TOKEN_PROPS,
                new AppProperties(
                        security,
                        base.cors(),
                        base.rateLimit(),
                        base.mail()
                )
        );
    }

    private void allowIssuance(VerificationPurpose purpose) {
        when(repo.upsertIfAllowed(
                any(),
                any(),
                eq(purpose),
                anyString()
        )).thenReturn(Optional.of(EXPIRES));
    }

    private String captureStoredHash(VerificationPurpose purpose) {
        ArgumentCaptor<String> captor =
                ArgumentCaptor.forClass(String.class);

        verify(repo).upsertIfAllowed(
                eq(USER_ID),
                eq(IDENTIFIER_ID),
                eq(purpose),
                captor.capture()
        );

        return captor.getValue();
    }

    @Nested
    class HappyPaths {

        @Test
        void issuesInviteTokensWithTheConfiguredInviteLifetime() {
            when(repo.upsertIfAllowed(any(), any(), eq(VerificationPurpose.PASSWORD_RESET), anyString(), anyInt()))
                    .thenReturn(Optional.of(EXPIRES));

            service.issueInviteToken(USER_ID, IDENTIFIER_ID).orElseThrow();

            verify(repo).upsertIfAllowed(eq(USER_ID), eq(IDENTIFIER_ID),
                    eq(VerificationPurpose.PASSWORD_RESET), anyString(), eq(86_400));
        }

        @Test
        void exposesTheConfiguredLifetimesForEmails() {
            assertThat(service.otpTtl()).isEqualTo(Duration.ofMinutes(10));
            assertThat(service.resetTtl()).isEqualTo(Duration.ofHours(1));
            assertThat(service.inviteTtl()).isEqualTo(Duration.ofHours(24));
        }

        @Test
        void generatesSixDigitCodesAndStoresTheirHashes() {
            allowIssuance(VerificationPurpose.SIGNUP_OTP);

            List<VerificationTokens.IssuedOtp> issued =
                    IntStream.range(0, 2_000)
                            .mapToObj(i -> service.issueSignupOtp(
                                    USER_ID,
                                    IDENTIFIER_ID
                            ).orElseThrow())
                            .toList();

            assertThat(issued).allSatisfy(otp -> {
                assertThat(otp.code())
                        .matches("[0-9]{" + OTP_CODE_DIGITS + "}");

                assertThat(otp.expiresAt())
                        .isEqualTo(EXPIRES);
            });

            assertThat(
                    issued.stream()
                            .map(VerificationTokens.IssuedOtp::code)
                            .distinct()
                            .count()
            ).isGreaterThan(1_900);

            verify(repo, times(2_000)).upsertIfAllowed(
                    eq(USER_ID),
                    eq(IDENTIFIER_ID),
                    eq(VerificationPurpose.SIGNUP_OTP),
                    matches("[0-9a-f]{64}")
            );
        }

        @Test
        void storesTheHashOfTheActualIssuedOtp() {
            allowIssuance(VerificationPurpose.SIGNUP_OTP);

            VerificationTokens.IssuedOtp issued =
                    service.issueSignupOtp(
                            USER_ID,
                            IDENTIFIER_ID
                    ).orElseThrow();

            String storedHash =
                    captureStoredHash(VerificationPurpose.SIGNUP_OTP);

            assertThat(storedHash)
                    .matches("[0-9a-f]{64}")
                    .isNotEqualTo(issued.code());

            // The stored hash must authenticate the issued code.
            when(repo.claimAttempt(
                    USER_ID,
                    VerificationPurpose.SIGNUP_OTP,
                    OTP_MAX_ATTEMPTS
            )).thenReturn(Optional.of(
                    new ClaimedToken(TOKEN_ID, storedHash)
            ));

            assertThat(service.consumeSignupOtp(
                    USER_ID,
                    issued.code()
            )).isTrue();

            verify(repo).delete(TOKEN_ID);
        }

        @Test
        void generatesUrlSafeResetTokensOf32RandomBytes() {
            allowIssuance(VerificationPurpose.PASSWORD_RESET);

            List<String> tokens =
                    IntStream.range(0, 1_000)
                            .mapToObj(i -> service.issueResetToken(
                                    USER_ID,
                                    IDENTIFIER_ID
                            ).orElseThrow().token())
                            .toList();

            assertThat(tokens).allSatisfy(token -> {
                assertThat(token)
                        .hasSize(43)
                        .matches("[A-Za-z0-9_-]+");

                assertThat(Base64.getUrlDecoder().decode(token))
                        .hasSize(RESET_TOKEN_BYTES);
            });

            assertThat(tokens.stream().distinct().count())
                    .isEqualTo(1_000);
        }

        @Test
        void storesTheHashOfTheActualIssuedResetToken() {
            allowIssuance(VerificationPurpose.PASSWORD_RESET);

            VerificationTokens.IssuedResetToken issued =
                    service.issueResetToken(
                            USER_ID,
                            IDENTIFIER_ID
                    ).orElseThrow();

            String storedHash =
                    captureStoredHash(VerificationPurpose.PASSWORD_RESET);

            assertThat(storedHash)
                    .matches("[0-9a-f]{64}")
                    .isNotEqualTo(issued.token());

            when(repo.findLiveIdentifier(
                    VerificationPurpose.PASSWORD_RESET,
                    storedHash
            )).thenReturn(Optional.of(EMAIL));

            assertThat(service.findLiveResetEmail(issued.token()))
                    .contains(EMAIL);
        }

        @Test
        void returnsExpirationForIssuedTokens() {
            allowIssuance(VerificationPurpose.SIGNUP_OTP);
            allowIssuance(VerificationPurpose.PASSWORD_RESET);

            assertThat(service.issueSignupOtp(
                    USER_ID,
                    IDENTIFIER_ID
            ).orElseThrow().expiresAt()).isEqualTo(EXPIRES);

            assertThat(service.issueResetToken(
                    USER_ID,
                    IDENTIFIER_ID
            ).orElseThrow().expiresAt()).isEqualTo(EXPIRES);
        }

        @Test
        void consumesMatchingOtpAndDeletesIt() {
            when(repo.claimAttempt(
                    USER_ID,
                    VerificationPurpose.SIGNUP_OTP,
                    OTP_MAX_ATTEMPTS
            )).thenReturn(Optional.of(
                    new ClaimedToken(TOKEN_ID, EXPECTED_CODE_HASH)
            ));

            assertThat(service.consumeSignupOtp(USER_ID, CODE))
                    .isTrue();

            verify(repo).delete(TOKEN_ID);
        }

        @Test
        void hashesResetTokensWithTheirOwnDomainPrefix() {
            when(repo.findLiveIdentifier(
                    VerificationPurpose.PASSWORD_RESET,
                    EXPECTED_TOKEN_HASH
            )).thenReturn(Optional.of(EMAIL));

            assertThat(service.findLiveResetEmail(RESET_TOKEN))
                    .contains(EMAIL);

            verify(repo).findLiveIdentifier(
                    VerificationPurpose.PASSWORD_RESET,
                    EXPECTED_TOKEN_HASH
            );
        }

        @Test
        void reportsRemainingCooldown() {
            when(repo.secondsUntilAllowed(
                    IDENTIFIER_ID,
                    VerificationPurpose.SIGNUP_OTP
            )).thenReturn(42);

            assertThat(service.secondsUntilAllowed(
                    IDENTIFIER_ID,
                    VerificationPurpose.SIGNUP_OTP
            )).isEqualTo(42);
        }
    }

    @Nested
    class BoundaryValues {

        @Test
        void preservesRepositoryCooldownValues() {
            when(repo.secondsUntilAllowed(
                    IDENTIFIER_ID,
                    VerificationPurpose.PASSWORD_RESET
            )).thenReturn(-5)
                    .thenReturn(0)
                    .thenReturn(1);

            assertThat(service.secondsUntilAllowed(
                    IDENTIFIER_ID,
                    VerificationPurpose.PASSWORD_RESET
            )).isEqualTo(-5);

            assertThat(service.secondsUntilAllowed(
                    IDENTIFIER_ID,
                    VerificationPurpose.PASSWORD_RESET
            )).isZero();

            assertThat(service.secondsUntilAllowed(
                    IDENTIFIER_ID,
                    VerificationPurpose.PASSWORD_RESET
            )).isEqualTo(1);
        }

        @Test
        void changingOneOtpDigitChangesTheStoredHash() {
            when(repo.claimAttempt(
                    USER_ID,
                    VerificationPurpose.SIGNUP_OTP,
                    OTP_MAX_ATTEMPTS
            )).thenReturn(Optional.of(
                    new ClaimedToken(TOKEN_ID, EXPECTED_NEXT_CODE_HASH)
            ));

            assertThat(service.consumeSignupOtp(
                    USER_ID,
                    "482914"
            )).isTrue();

            verify(repo).delete(TOKEN_ID);

            assertThat(EXPECTED_NEXT_CODE_HASH)
                    .isNotEqualTo(EXPECTED_CODE_HASH);
        }

        @Test
        void leadingZerosAreSignificant() {
            when(repo.claimAttempt(
                    USER_ID,
                    VerificationPurpose.SIGNUP_OTP,
                    OTP_MAX_ATTEMPTS
            )).thenReturn(Optional.of(
                    new ClaimedToken(TOKEN_ID, EXPECTED_CODE_HASH)
            ));

            assertThat(service.consumeSignupOtp(
                    USER_ID,
                    "0482913"
            )).isFalse();

            verify(repo, never()).delete(any());
        }
    }

    @Nested
    class ErrorPaths {

        @Test
        void rejectsWrongCodeAndWrongUserWithoutDeletingToken() {
            when(repo.claimAttempt(
                    any(),
                    eq(VerificationPurpose.SIGNUP_OTP),
                    eq(OTP_MAX_ATTEMPTS)
            )).thenReturn(Optional.of(
                    new ClaimedToken(TOKEN_ID, EXPECTED_CODE_HASH)
            ));

            assertThat(service.consumeSignupOtp(
                    USER_ID,
                    "482914"
            )).isFalse();

            assertThat(service.consumeSignupOtp(
                    OTHER_USER_ID,
                    CODE
            )).isFalse();

            verify(repo, never()).delete(any());
        }

        @Test
        void rejectsMissingOrMalformedStoredHashes() {
            // Chained stubbing avoids unchecked generic varargs warnings.
            when(repo.claimAttempt(
                    USER_ID,
                    VerificationPurpose.SIGNUP_OTP,
                    OTP_MAX_ATTEMPTS
            ))
                    .thenReturn(Optional.empty())
                    .thenReturn(Optional.of(
                            new ClaimedToken(TOKEN_ID, "deadbeef")
                    ))
                    .thenReturn(Optional.of(
                            new ClaimedToken(TOKEN_ID, "")
                    ));

            assertThat(service.consumeSignupOtp(USER_ID, CODE))
                    .isFalse();

            assertThat(service.consumeSignupOtp(USER_ID, CODE))
                    .isFalse();

            assertThat(service.consumeSignupOtp(USER_ID, CODE))
                    .isFalse();

            verify(repo, never()).delete(any());
        }

        @Test
        void aDifferentSecretChangesOtpHashAndRejectsOriginalHash() {
            VerificationTokens rotated =
                    withSecret(repo, OTHER_SECRET);

            when(repo.claimAttempt(
                    USER_ID,
                    VerificationPurpose.SIGNUP_OTP,
                    OTP_MAX_ATTEMPTS
            )).thenReturn(Optional.of(
                    new ClaimedToken(TOKEN_ID, EXPECTED_CODE_HASH)
            ));

            assertThat(rotated.consumeSignupOtp(
                    USER_ID,
                    CODE
            )).isFalse();

            verify(repo, never()).delete(any());
        }

        @Test
        void rotatedSecretProducesExpectedIndependentHmac() {
            VerificationTokens rotated =
                    withSecret(repo, OTHER_SECRET);

            when(repo.claimAttempt(
                    USER_ID,
                    VerificationPurpose.SIGNUP_OTP,
                    OTP_MAX_ATTEMPTS
            )).thenReturn(Optional.of(
                    new ClaimedToken(
                            TOKEN_ID,
                            EXPECTED_CODE_HASH_WITH_OTHER_SECRET
                    )
            ));

            assertThat(rotated.consumeSignupOtp(
                    USER_ID,
                    CODE
            )).isTrue();

            verify(repo).delete(TOKEN_ID);
        }

        @Test
        void throttlingReturnsEmptyWithoutExposingAnOtp() {
            when(repo.upsertIfAllowed(
                    any(),
                    any(),
                    eq(VerificationPurpose.SIGNUP_OTP),
                    anyString()
            )).thenReturn(Optional.empty());

            assertThat(service.issueSignupOtp(
                    USER_ID,
                    IDENTIFIER_ID
            )).isEmpty();
        }

        @Test
        void throttlingReturnsEmptyWithoutExposingAResetToken() {
            when(repo.upsertIfAllowed(
                    any(),
                    any(),
                    eq(VerificationPurpose.PASSWORD_RESET),
                    anyString()
            )).thenReturn(Optional.empty());

            assertThat(service.issueResetToken(
                    USER_ID,
                    IDENTIFIER_ID
            )).isEmpty();
        }

        @Test
        void resetTokenAndOtpUseSeparateHashDomains() {
            when(repo.findLiveIdentifier(
                    eq(VerificationPurpose.PASSWORD_RESET),
                    anyString()
            )).thenReturn(Optional.empty());

            service.findLiveResetEmail(CODE);

            ArgumentCaptor<String> resetHash =
                    ArgumentCaptor.forClass(String.class);

            verify(repo).findLiveIdentifier(
                    eq(VerificationPurpose.PASSWORD_RESET),
                    resetHash.capture()
            );

            assertThat(resetHash.getValue())
                    .matches("[0-9a-f]{64}")
                    .isNotEqualTo(EXPECTED_CODE_HASH);
        }

        @Test
        void unknownResetTokenReturnsEmpty() {
            assertThat(service.findLiveResetEmail(RESET_TOKEN))
                    .isEmpty();
        }
    }

    @Nested
    class DependencyFailures {

        @Test
        void propagatesDatabaseTimeoutDuringOtpIssuance() {
            when(repo.upsertIfAllowed(
                    any(),
                    any(),
                    eq(VerificationPurpose.SIGNUP_OTP),
                    anyString()
            )).thenThrow(
                    new QueryTimeoutException("statement timeout")
            );

            assertThatThrownBy(() -> service.issueSignupOtp(
                    USER_ID,
                    IDENTIFIER_ID
            )).isInstanceOf(QueryTimeoutException.class);
        }

        @Test
        void propagatesDatabaseTimeoutDuringResetTokenIssuance() {
            when(repo.upsertIfAllowed(
                    any(),
                    any(),
                    eq(VerificationPurpose.PASSWORD_RESET),
                    anyString()
            )).thenThrow(
                    new QueryTimeoutException("statement timeout")
            );

            assertThatThrownBy(() -> service.issueResetToken(
                    USER_ID,
                    IDENTIFIER_ID
            )).isInstanceOf(QueryTimeoutException.class);
        }

        @Test
        void propagatesDatabaseTimeoutDuringCooldownLookup() {
            when(repo.secondsUntilAllowed(
                    IDENTIFIER_ID,
                    VerificationPurpose.SIGNUP_OTP
            )).thenThrow(
                    new QueryTimeoutException("statement timeout")
            );

            assertThatThrownBy(() -> service.secondsUntilAllowed(
                    IDENTIFIER_ID,
                    VerificationPurpose.SIGNUP_OTP
            )).isInstanceOf(QueryTimeoutException.class);
        }

        @Test
        void propagatesDatabaseTimeoutWhileClaimingOtp() {
            when(repo.claimAttempt(
                    USER_ID,
                    VerificationPurpose.SIGNUP_OTP,
                    OTP_MAX_ATTEMPTS
            )).thenThrow(
                    new QueryTimeoutException("statement timeout")
            );

            assertThatThrownBy(() -> service.consumeSignupOtp(
                    USER_ID,
                    CODE
            )).isInstanceOf(QueryTimeoutException.class);
        }

        @Test
        void propagatesDatabaseTimeoutWhileLookingUpResetToken() {
            when(repo.findLiveIdentifier(
                    eq(VerificationPurpose.PASSWORD_RESET),
                    anyString()
            )).thenThrow(
                    new QueryTimeoutException("statement timeout")
            );

            assertThatThrownBy(() ->
                    service.findLiveResetEmail(RESET_TOKEN)
            ).isInstanceOf(QueryTimeoutException.class);
        }
    }
}