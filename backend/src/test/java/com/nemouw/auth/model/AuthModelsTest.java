package com.nemouw.auth.model;

import com.nemouw.auth.dto.AuthDtos.PasswordSetupResponse;
import com.nemouw.auth.dto.AuthDtos.SessionResponse;
import org.junit.jupiter.api.Test;

import static com.nemouw.support.Fixtures.USER_ID;
import static org.assertj.core.api.Assertions.assertThat;

class AuthModelsTest {

    private static final IssuedSession SESSION = new IssuedSession(
            "eyJhbGciOiJIUzI1NiJ9.access.sig", "Hq7-Lm2_Xv9Tn4Rb1Zp8", USER_ID, "specialist");

    @Test
    void keepsBothTokensOutOfAnIssuedSessionsToString() {
        assertThat(SESSION.toString())
                .doesNotContain(SESSION.accessToken())
                .doesNotContain(SESSION.refreshToken())
                .contains(USER_ID.toString(), "specialist");
    }

    @Test
    void keepsTheTokenOutOfAnIssuedSetupTokensToString() {
        IssuedSetupToken setup = new IssuedSetupToken("eyJhbGciOiJIUzI1NiJ9.setup.sig", USER_ID);

        assertThat(setup.toString()).doesNotContain(setup.token()).contains(USER_ID.toString());
    }

    @Test
    void exposesOnlyUserRoleAndLifetimeToTheClientAfterLogin() {
        SessionResponse response = SessionResponse.from(SESSION);

        assertThat(response.userId()).isEqualTo(USER_ID);
        assertThat(response.role()).isEqualTo("specialist");
        assertThat(response.expiresIn()).isEqualTo(900);
        assertThat(response.toString()).doesNotContain(SESSION.accessToken()).doesNotContain(SESSION.refreshToken());
    }

    @Test
    void exposesOnlyTheLifetimeAfterEmailVerificationAndNeitherTheTokenNorTheUserId() {
        IssuedSetupToken setup = new IssuedSetupToken("eyJhbGciOiJIUzI1NiJ9.setup.sig", USER_ID);

        PasswordSetupResponse response = PasswordSetupResponse.from(setup);

        assertThat(response.expiresIn()).isEqualTo(900);
        assertThat(response.toString()).doesNotContain(setup.token()).doesNotContain(USER_ID.toString());
    }
}
