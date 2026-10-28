package com.nemouw.user.session;

import com.nemouw.user.session.SessionDtos.SessionResponse;
import org.junit.jupiter.api.Test;

import static com.nemouw.support.Fixtures.USER_ID;
import static com.nemouw.support.Fixtures.props;
import static org.assertj.core.api.Assertions.assertThat;

class IssuedSessionTest {

    private static final int ACCESS_TTL_SECONDS =
            props().security().accessTtlSeconds();

    private static final IssuedSession SESSION = new IssuedSession(
            "eyJhbGciOiJIUzI1NiJ9.access.sig",
            "Hq7-Lm2_Xv9Tn4Rb1Zp8",
            USER_ID,
            "specialist"
    );

    @Test
    void keepsBothTokensOutOfAnIssuedSessionsToString() {
        assertThat(SESSION.toString())
                .doesNotContain(SESSION.accessToken())
                .doesNotContain(SESSION.refreshToken())
                .contains(USER_ID.toString(), "specialist");
    }

    @Test
    void exposesOnlyUserRoleAndLifetimeToTheClientAfterLogin() {
        SessionResponse response = SessionResponse.from(
                SESSION,
                ACCESS_TTL_SECONDS
        );

        assertThat(response.userId()).isEqualTo(USER_ID);
        assertThat(response.role()).isEqualTo("specialist");
        assertThat(response.expiresIn()).isEqualTo(ACCESS_TTL_SECONDS);

        assertThat(response.toString())
                .doesNotContain(SESSION.accessToken())
                .doesNotContain(SESSION.refreshToken());
    }
}
