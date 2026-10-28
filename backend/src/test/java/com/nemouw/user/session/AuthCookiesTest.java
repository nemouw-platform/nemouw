package com.nemouw.user.session;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.util.List;

import static com.nemouw.support.Fixtures.USER_ID;
import static com.nemouw.support.Fixtures.props;
import static org.assertj.core.api.Assertions.assertThat;

class AuthCookiesTest {

    private static final int SETUP_TTL_SECONDS = 15 * 60;
    private static final IssuedSession SESSION =
            new IssuedSession("eyJhbGciOiJIUzI1NiJ9.access.sig", "Hq7-Lm2_Xv9Tn4Rb1Zp8", USER_ID, "specialist");
    private static final String SETUP_TOKEN = "eyJhbGciOiJIUzI1NiJ9.setup.sig";

    private final AuthCookies secureCookies = new AuthCookies(props(true, false));

    private static List<String> setCookies(HttpHeaders headers) {
        return headers.get(HttpHeaders.SET_COOKIE);
    }

    @Nested
    class HappyPaths {

        @Test
        void setsAnHttpOnlyAccessCookieForTheWholeSiteLastingFifteenMinutes() {
            String access = setCookies(secureCookies.sessionHeaders(SESSION)).getFirst();

            assertThat(access)
                    .startsWith("access_token=eyJhbGciOiJIUzI1NiJ9.access.sig;")
                    .contains("Path=/;", "Max-Age=900", "HttpOnly", "SameSite=Lax");
        }

        @Test
        void setsAStrictRefreshCookieScopedToTheAuthEndpointsLastingNinetyDays() {
            String refresh = setCookies(secureCookies.sessionHeaders(SESSION)).get(1);

            assertThat(refresh)
                    .startsWith("refresh_token=Hq7-Lm2_Xv9Tn4Rb1Zp8;")
                    .contains("Path=/v1/auth;", "Max-Age=7776000", "HttpOnly", "SameSite=Strict");
        }

        @Test
        void marksBothSessionCookiesSecureWhenConfiguredSo() {
            List<String> cookies = setCookies(secureCookies.sessionHeaders(SESSION));

            assertThat(cookies).hasSize(2).allMatch(cookie -> cookie.contains("Secure"));
        }

        @Test
        void omitsTheSecureFlagForLocalHttpDevelopment() {
            AuthCookies insecure = new AuthCookies(props(false, false));

            List<String> cookies = setCookies(insecure.sessionHeaders(SESSION));

            assertThat(cookies).hasSize(2).noneMatch(cookie -> cookie.contains("Secure"));
        }

        @Test
        void clearsBothCookiesOnTheSamePathsTheyWereSetWith() {
            List<String> cookies = setCookies(secureCookies.clearHeaders());

            assertThat(cookies.get(0)).startsWith("access_token=;").contains("Max-Age=0", "Path=/;");
            assertThat(cookies.get(1)).startsWith("refresh_token=;").contains("Max-Age=0", "Path=/v1/auth;");
        }
    }

    @Nested
    class BoundaryValues {

        @Test
        void givesThePasswordSetupTokenItsOwnFifteenMinuteLifetimeInTheAccessCookie() {
            List<String> cookies = setCookies(secureCookies.setupHeaders(SETUP_TOKEN, SETUP_TTL_SECONDS));

            assertThat(cookies.getFirst())
                    .startsWith("access_token=eyJhbGciOiJIUzI1NiJ9.setup.sig;")
                    .contains("Max-Age=900", "Path=/;", "HttpOnly", "SameSite=Lax");
        }

        @Test
        void neverIssuesARefreshCookieWithASetupTokenSoItCannotBeRenewedIntoASession() {
            List<String> cookies = setCookies(secureCookies.setupHeaders(SETUP_TOKEN, SETUP_TTL_SECONDS));

            assertThat(cookies).hasSize(1).noneMatch(cookie -> cookie.startsWith("refresh_token="));
        }
    }
}
