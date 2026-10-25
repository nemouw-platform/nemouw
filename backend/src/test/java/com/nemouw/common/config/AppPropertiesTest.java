package com.nemouw.common.config;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Set;

import static com.nemouw.support.Fixtures.JWT_SECRET;
import static com.nemouw.support.Fixtures.OTP_SECRET;
import static com.nemouw.support.Fixtures.props;
import static org.assertj.core.api.Assertions.assertThat;

/** The app must refuse to start with missing or weak secrets, so these constraints guard the whole auth layer. */
class AppPropertiesTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private static AppProperties withSecrets(String jwtSecret, String otpHashSecret) {
        AppProperties base = props();
        return new AppProperties(
                new AppProperties.Security(jwtSecret, otpHashSecret, true, false),
                base.cors(), base.rateLimit(), base.mail());
    }

    private static List<String> violatedPaths(AppProperties properties) {
        Set<ConstraintViolation<AppProperties>> violations = VALIDATOR.validate(properties);
        return violations.stream().map(v -> v.getPropertyPath().toString()).distinct().toList();
    }

    @Test
    void acceptsAFullyConfiguredApplication() {
        assertThat(violatedPaths(props())).isEmpty();
    }

    @Test
    void acceptsSecretsOfExactly32Characters() {
        AppProperties properties = withSecrets("a".repeat(32), "b".repeat(32));

        assertThat(violatedPaths(properties)).isEmpty();
    }

    @Test
    void rejectsAJwtSecretOf31Characters() {
        AppProperties properties = withSecrets("a".repeat(31), OTP_SECRET);

        assertThat(violatedPaths(properties)).containsExactly("security.jwtSecret");
    }

    @Test
    void rejectsAnOtpHashSecretOf31Characters() {
        AppProperties properties = withSecrets(JWT_SECRET, "b".repeat(31));

        assertThat(violatedPaths(properties)).containsExactly("security.otpHashSecret");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void rejectsABlankJwtSecret(String blank) {
        AppProperties properties = withSecrets(blank, OTP_SECRET);

        assertThat(violatedPaths(properties)).containsExactly("security.jwtSecret");
    }

    @Test
    void rejectsAMissingSecurityBlock() {
        AppProperties base = props();
        AppProperties properties = new AppProperties(null, base.cors(), base.rateLimit(), base.mail());

        assertThat(violatedPaths(properties)).containsExactly("security");
    }

    @Test
    void rejectsARateLimitOfZeroRequestsPerSecond() {
        AppProperties base = props();
        AppProperties properties = new AppProperties(base.security(), base.cors(),
                new AppProperties.RateLimit(0), base.mail());

        assertThat(violatedPaths(properties)).containsExactly("rateLimit.requestsPerSecond");
    }

    @Test
    void rejectsAnEmptyListOfAllowedCorsOrigins() {
        AppProperties base = props();
        AppProperties properties = new AppProperties(base.security(),
                new AppProperties.Cors(List.of(), false), base.rateLimit(), base.mail());

        assertThat(violatedPaths(properties)).containsExactly("cors.allowedOrigins");
    }
}
