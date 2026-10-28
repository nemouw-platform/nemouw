package com.nemouw.user.token;

import com.nemouw.support.ShippedProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class TokenPropertiesTest {

    private final TokenProperties props = ShippedProperties.bind("app.user.token", TokenProperties.class);

    @Test
    void signupOtpLivesTenMinutesAndAllowsThreeSendsPer15MinutesAndTenPerDay() {
        assertThat(props.of(VerificationPurpose.SIGNUP_OTP)).isEqualTo(new TokenProperties.Throttle(
                Duration.ofMinutes(10), Duration.ofSeconds(60), Duration.ofMinutes(15), 3, 10));
    }

    @Test
    void passwordResetLivesOneHourAndAllowsThreeSendsPerHourAndTenPerDay() {
        assertThat(props.of(VerificationPurpose.PASSWORD_RESET)).isEqualTo(new TokenProperties.Throttle(
                Duration.ofHours(1), Duration.ofSeconds(60), Duration.ofHours(1), 3, 10));
    }

    @Test
    void invitesLiveOneDayAndAnOtpAllowsFiveAttempts() {
        assertThat(props.inviteTtl()).isEqualTo(Duration.ofHours(24));
        assertThat(props.maxOtpAttempts()).isEqualTo(5);
    }

    @ParameterizedTest
    @EnumSource(VerificationPurpose.class)
    void everyPurposeHasASelfConsistentThrottle(VerificationPurpose purpose) {
        TokenProperties.Throttle t = props.of(purpose);

        assertThat(t.cooldownSeconds()).isPositive().isLessThan(t.windowSeconds());
        assertThat(t.sendCap()).isPositive().isLessThanOrEqualTo(t.dailyCap());
        assertThat(t.ttlSeconds()).isPositive();
    }
}