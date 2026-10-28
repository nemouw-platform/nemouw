package com.nemouw.user.signup;

import com.nemouw.user.signup.SignupDtos.PasswordSetupResponse;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static com.nemouw.support.Fixtures.USER_ID;
import static org.assertj.core.api.Assertions.assertThat;

class SignupModelsTest {

    @Test
    void keepsTheTokenOutOfAnIssuedSetupTokensToString() {
        IssuedSetupToken setup = new IssuedSetupToken(
                "eyJhbGciOiJIUzI1NiJ9.setup.sig",
                USER_ID,
                Duration.ofMinutes(15)
        );

        assertThat(setup.toString())
                .doesNotContain(setup.token())
                .contains(USER_ID.toString());
    }

    @Test
    void exposesOnlyTheLifetimeAfterEmailVerificationAndNeitherTheTokenNorTheUserId() {
        IssuedSetupToken setup = new IssuedSetupToken(
                "eyJhbGciOiJIUzI1NiJ9.setup.sig",
                USER_ID,
                Duration.ofMinutes(15)
        );

        PasswordSetupResponse response = new PasswordSetupResponse(900);

        assertThat(response.expiresIn()).isEqualTo(900);
        assertThat(response.toString())
                .doesNotContain(setup.token())
                .doesNotContain(USER_ID.toString());
    }
}