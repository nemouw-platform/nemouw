package com.nemouw.specialist.profile;

import com.nemouw.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.nemouw.specialist.profile.SpecialistRows.SPECIALIST_ID;
import static com.nemouw.support.Fixtures.USER_ID;
import static com.nemouw.support.Fixtures.assertApiError;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpecialistLookupTest {

    private final SpecialistRepository specialists = mock(SpecialistRepository.class);
    private final SpecialistLookup lookup = new SpecialistLookup(specialists);

    @Test
    void returnsTheSpecialistIdOfTheUser() {
        when(specialists.findIdByUserId(USER_ID)).thenReturn(Optional.of(SPECIALIST_ID));

        assertThat(lookup.requireIdOf(USER_ID)).isEqualTo(SPECIALIST_ID);
    }

    @Test
    void answersOnboardingRequiredForAUserWithoutAProfile() {
        assertApiError(() -> lookup.requireIdOf(USER_ID), ErrorCode.ONBOARDING_REQUIRED);
    }
}