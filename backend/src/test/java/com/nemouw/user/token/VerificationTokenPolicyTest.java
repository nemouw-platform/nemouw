package com.nemouw.user.token;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class VerificationTokenPolicyTest {

    @Test
    void signupOtpLivesTenMinutesAndAllowsThreeSendsPer15MinutesAndTenPerDay() {
        VerificationTokenPolicy policy = VerificationTokenPolicy.of(VerificationPurpose.SIGNUP_OTP);

        assertThat(policy).isEqualTo(new VerificationTokenPolicy(600, 60, 900, 3, 10));
    }

    @Test
    void passwordResetLivesOneHourAndAllowsThreeSendsPerHourAndTenPerDay() {
        VerificationTokenPolicy policy = VerificationTokenPolicy.of(VerificationPurpose.PASSWORD_RESET);

        assertThat(policy).isEqualTo(new VerificationTokenPolicy(3600, 60, 3600, 3, 10));
    }

    @ParameterizedTest
    @EnumSource(VerificationPurpose.class)
    void everyPurposeHasASelfConsistentPolicy(VerificationPurpose purpose) {
        VerificationTokenPolicy policy = VerificationTokenPolicy.of(purpose);

        assertThat(policy.cooldownSeconds()).isPositive().isLessThan(policy.windowSeconds());
        assertThat(policy.windowCap()).isPositive().isLessThanOrEqualTo(policy.dayCap());
        assertThat(policy.ttlSeconds()).isPositive();
    }
}
