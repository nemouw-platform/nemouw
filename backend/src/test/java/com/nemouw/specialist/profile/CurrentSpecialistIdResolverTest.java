package com.nemouw.specialist.profile;

import com.nemouw.common.exception.ErrorCode;
import com.nemouw.common.exception.Errors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.UUID;

import static com.nemouw.specialist.profile.SpecialistRows.SPECIALIST_ID;
import static com.nemouw.support.Fixtures.USER_ID;
import static com.nemouw.support.Fixtures.assertApiError;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CurrentSpecialistIdResolverTest {

    @SuppressWarnings("unused")
    private static final class Handlers {
        void annotated(@CurrentSpecialistId UUID id) {
        }

        void wrongType(@CurrentSpecialistId String id) {
        }

        void plain(UUID id) {
        }
    }

    private final SpecialistLookup lookup = mock(SpecialistLookup.class);
    private final CurrentSpecialistIdResolver resolver = new CurrentSpecialistIdResolver(lookup);

    @AfterEach
    void clearTheSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private static MethodParameter parameter(String method, Class<?> type) throws NoSuchMethodException {
        return new MethodParameter(Handlers.class.getDeclaredMethod(method, type), 0);
    }

    private static void authenticateAs(UUID userId) {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject(userId.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @Test
    void supportsOnlyAUuidParameterAnnotatedWithCurrentSpecialistId() throws Exception {
        assertThat(resolver.supportsParameter(parameter("annotated", UUID.class))).isTrue();
        assertThat(resolver.supportsParameter(parameter("wrongType", String.class))).isFalse();
        assertThat(resolver.supportsParameter(parameter("plain", UUID.class))).isFalse();
    }

    @Test
    void resolvesTheSpecialistOfTheAuthenticatedUser() throws Exception {
        authenticateAs(USER_ID);
        when(lookup.requireIdOf(USER_ID)).thenReturn(SPECIALIST_ID);

        assertThat(resolver.resolveArgument(parameter("annotated", UUID.class), null, null, null))
                .isEqualTo(SPECIALIST_ID);
    }

    @Test
    void answersUnauthenticatedWithoutAnyAuthentication() throws Exception {
        MethodParameter parameter = parameter("annotated", UUID.class);

        assertApiError(() -> resolver.resolveArgument(parameter, null, null, null), ErrorCode.UNAUTHENTICATED);
    }

    @Test
    void answersUnauthenticatedForANonJwtAuthentication() throws Exception {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("user", "password"));
        MethodParameter parameter = parameter("annotated", UUID.class);

        assertApiError(() -> resolver.resolveArgument(parameter, null, null, null), ErrorCode.UNAUTHENTICATED);
    }

    @Test
    void passesThroughOnboardingRequiredForAUserWithoutAProfile() throws Exception {
        authenticateAs(USER_ID);
        when(lookup.requireIdOf(USER_ID)).thenThrow(Errors.onboardingRequired());
        MethodParameter parameter = parameter("annotated", UUID.class);

        assertApiError(() -> resolver.resolveArgument(parameter, null, null, null), ErrorCode.ONBOARDING_REQUIRED);
    }
}