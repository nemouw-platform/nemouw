package com.nemouw.syndicate.access;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static com.nemouw.support.Fixtures.USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SyndicateAccessTest {

    private static final UUID SYNDICATE_ID = UUID.fromString("a1c4e7f0-3b52-4d96-8e1a-60f2b9d7c314");

    private final SyndicateMembershipRepository membership = mock(SyndicateMembershipRepository.class);
    private final SyndicateAccess access = new SyndicateAccess(membership);

    private static Authentication jwtOf(UUID userId) {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject(userId.toString()).build();
        return new JwtAuthenticationToken(jwt);
    }

    @Test
    void asksWhetherTheJwtSubjectIsStaffOfTheSyndicate() {
        when(membership.isStaff(SYNDICATE_ID, USER_ID)).thenReturn(true);

        assertThat(access.isStaffOf(SYNDICATE_ID, jwtOf(USER_ID))).isTrue();
        assertThat(access.isStaffOf(SYNDICATE_ID, jwtOf(UUID.randomUUID()))).isFalse();
        verify(membership).isStaff(SYNDICATE_ID, USER_ID);
    }

    @Test
    void asksWhetherTheJwtSubjectIsTheOwnerOfTheSyndicate() {
        when(membership.isOwner(SYNDICATE_ID, USER_ID)).thenReturn(true);

        assertThat(access.isOwnerOf(SYNDICATE_ID, jwtOf(USER_ID))).isTrue();
        assertThat(access.isOwnerOf(SYNDICATE_ID, jwtOf(UUID.randomUUID()))).isFalse();
        verify(membership).isOwner(SYNDICATE_ID, USER_ID);
    }

    @Test
    void deniesANonJwtPrincipalWithoutAnyQuery() {
        Authentication notAJwt = new UsernamePasswordAuthenticationToken("user", "password");

        assertThat(access.isStaffOf(SYNDICATE_ID, notAJwt)).isFalse();
        assertThat(access.isOwnerOf(SYNDICATE_ID, notAJwt)).isFalse();
        verifyNoInteractions(membership);
    }

    @Test
    void staysInStepWithTheSpelExpressionsOfTheAccessAnnotations() throws NoSuchMethodException {
        assertThat(AdminOrSyndicateOwner.class.getAnnotation(PreAuthorize.class).value())
                .contains("@syndicateAccess.isOwnerOf(#syndicateId, authentication)");
        assertThat(AdminOrSyndicateStaff.class.getAnnotation(PreAuthorize.class).value())
                .contains("@syndicateAccess.isStaffOf(#syndicateId, authentication)");
        assertThat(SyndicateAccess.class.getAnnotation(Component.class).value()).isEqualTo("syndicateAccess");
        assertThat(SyndicateAccess.class.getMethod("isOwnerOf", UUID.class, Authentication.class).getReturnType())
                .isEqualTo(boolean.class);
        assertThat(SyndicateAccess.class.getMethod("isStaffOf", UUID.class, Authentication.class).getReturnType())
                .isEqualTo(boolean.class);
    }
}