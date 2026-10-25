package com.nemouw.auth.service;

import com.nemouw.common.config.AppProperties;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.List;
import java.util.stream.Stream;

import static com.nemouw.auth.AuthConstants.OTP_CODE_DIGITS;
import static com.nemouw.auth.AuthConstants.RESET_TOKEN_BYTES;
import static com.nemouw.support.Fixtures.OTHER_SECRET;
import static com.nemouw.support.Fixtures.OTHER_USER_ID;
import static com.nemouw.support.Fixtures.USER_ID;
import static com.nemouw.support.Fixtures.props;
import static org.assertj.core.api.Assertions.assertThat;

class VerificationTokenServiceTest {

    private static final String CODE = "482913";
    private static final String RESET_TOKEN = "Zx9-Kq2_mT7vLb4NwR1pYc8HdJ5sUe0AfG3iOa6BkQ";

    // HMAC-SHA256 values computed independently (Python hmac) with the secret in Fixtures.OTP_SECRET.
    private static final String EXPECTED_CODE_HASH = "c079310d86c90c156986f8397779eba2f1f896ceee527266a710fa5970fba054";
    private static final String EXPECTED_NEXT_CODE_HASH = "f0cda239a3c849c9ff22c276ca16262c9a79ecd616e0527c0daba11ef5d7bf6e";
    private static final String EXPECTED_TOKEN_HASH = "d13c7402d2fef0ca4d3667020a3509ee662d63f2b0a84027d607986ece9715d0";
    private static final String EXPECTED_CODE_HASH_WITH_OTHER_SECRET = "66d06b196a525e2a8830a4b211313172c40b2ebe20373cf38592d15d591eb663";

    private final VerificationTokenService service = new VerificationTokenService(props());

    private static VerificationTokenService serviceWithOtpSecret(String secret) {
        AppProperties base = props();
        AppProperties.Security security = new AppProperties.Security(
                base.security().jwtSecret(), secret, base.security().cookieSecure(), base.security().trustProxy());
        return new VerificationTokenService(
                new AppProperties(security, base.cors(), base.rateLimit(), base.mail()));
    }

    @Nested
    class HappyPaths {

        @Test
        void generatesCodesOfExactlySixDigits() {
            List<String> codes = Stream.generate(service::generateCode).limit(2_000).toList();

            assertThat(codes).allMatch(code -> code.matches("\\d{" + OTP_CODE_DIGITS + "}"));
        }

        @Test
        void generatesCodesThatAreSpreadAcrossTheSpaceInsteadOfRepeating() {
            long distinct = Stream.generate(service::generateCode).limit(2_000).distinct().count();

            assertThat(distinct).isGreaterThan(1_900);
        }

        @Test
        void generatesUrlSafeResetTokensCarrying32RandomBytes() {
            String token = service.generateResetToken();

            assertThat(token).hasSize(43).matches("[A-Za-z0-9_-]+");
            assertThat(Base64.getUrlDecoder().decode(token)).hasSize(RESET_TOKEN_BYTES);
        }

        @Test
        void neverRepeatsAResetTokenAcrossAThousandDraws() {
            long distinct = Stream.generate(service::generateResetToken).limit(1_000).distinct().count();

            assertThat(distinct).isEqualTo(1_000);
        }

        @Test
        void hashesACodeAsHmacSha256OfUserIdAndCode() {
            assertThat(service.hash(USER_ID, CODE)).isEqualTo(EXPECTED_CODE_HASH);
        }

        @Test
        void hashesAResetTokenWithADomainPrefix() {
            assertThat(service.hashToken(RESET_TOKEN)).isEqualTo(EXPECTED_TOKEN_HASH);
        }

        @Test
        void hashesTheSameInputToTheSameValueEveryTime() {
            assertThat(service.hash(USER_ID, CODE)).isEqualTo(service.hash(USER_ID, CODE));
        }

        @Test
        void matchesTheCodeThatWasHashedForThatUser() {
            assertThat(service.matches(USER_ID, CODE, EXPECTED_CODE_HASH)).isTrue();
        }
    }

    @Nested
    class BoundaryValues {

        @Test
        void changingASingleDigitOfTheCodeChangesTheHash() {
            assertThat(service.hash(USER_ID, "482914")).isEqualTo(EXPECTED_NEXT_CODE_HASH)
                    .isNotEqualTo(EXPECTED_CODE_HASH);
        }

        @Test
        void producesAFixedLengthLowercaseHexDigest() {
            assertThat(service.hash(USER_ID, "000000")).matches("[0-9a-f]{64}");
        }

        @Test
        void keepsLeadingZerosOfACodeSignificant() {
            assertThat(service.hash(USER_ID, "004829")).isNotEqualTo(service.hash(USER_ID, "4829"));
        }
    }

    @Nested
    class ErrorPaths {

        @Test
        void doesNotMatchAWrongCode() {
            assertThat(service.matches(USER_ID, "482914", EXPECTED_CODE_HASH)).isFalse();
        }

        @Test
        void doesNotMatchTheRightCodeForADifferentUser() {
            assertThat(service.matches(OTHER_USER_ID, CODE, EXPECTED_CODE_HASH)).isFalse();
        }

        @Test
        void doesNotMatchAStoredHashOfTheWrongLengthOrAnEmptyOne() {
            assertThat(service.matches(USER_ID, CODE, "deadbeef")).isFalse();
            assertThat(service.matches(USER_ID, CODE, "")).isFalse();
        }

        @Test
        void producesADifferentHashWhenTheServerSecretDiffers() {
            VerificationTokenService rotated = serviceWithOtpSecret(OTHER_SECRET);

            assertThat(rotated.hash(USER_ID, CODE)).isEqualTo(EXPECTED_CODE_HASH_WITH_OTHER_SECRET)
                    .isNotEqualTo(EXPECTED_CODE_HASH);
        }

        @Test
        void doesNotAcceptAHashMadeWithAnotherSecret() {
            assertThat(service.matches(USER_ID, CODE, EXPECTED_CODE_HASH_WITH_OTHER_SECRET)).isFalse();
        }

        @Test
        void keepsAResetTokenHashApartFromAnOtpHashOfTheSameText() {
            assertThat(service.hashToken(CODE)).isNotEqualTo(service.hash(USER_ID, CODE));
        }
    }
}
