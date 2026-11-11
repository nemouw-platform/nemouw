package com.nemouw.specialist.profile;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpecialistVerificationStatusTest {

    @Nested
    class HappyPaths {

        @Test
        void spellsEveryStatusAsItsLowercaseCodeInDeclarationOrder() {
            assertThat(Arrays.stream(SpecialistVerificationStatus.values()).map(SpecialistVerificationStatus::code))
                    .containsExactly("pending", "active", "rejected");
        }

        @ParameterizedTest
        @EnumSource(SpecialistVerificationStatus.class)
        void findsEveryStatusByItsCode(SpecialistVerificationStatus status) {
            assertThat(SpecialistVerificationStatus.tryFrom(status.code())).contains(status);
        }

        @ParameterizedTest
        @EnumSource(SpecialistVerificationStatus.class)
        void returnsEveryStatusFromFromCode(SpecialistVerificationStatus status) {
            assertThat(SpecialistVerificationStatus.fromCode(status.code())).isEqualTo(status);
        }
    }

    @Nested
    class BoundaryValues {

        @Test
        void hasExactlyThreeStatuses() {
            assertThat(SpecialistVerificationStatus.values()).hasSize(3);
        }

        @Test
        void usesActiveWhereTheCredentialStatusUsesVerified() {
            assertThat(SpecialistVerificationStatus.tryFrom("verified")).isEmpty();
        }
    }

    @Nested
    class ErrorPaths {

        @ParameterizedTest
        @ValueSource(strings = {"Pending", "ACTIVE", "rEjEcTeD"})
        void isCaseSensitive(String code) {
            assertThat(SpecialistVerificationStatus.tryFrom(code)).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {"suspended", "approved", "act ive", "1"})
        void returnsEmptyForUnknownStatuses(String code) {
            assertThat(SpecialistVerificationStatus.tryFrom(code)).isEmpty();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        void returnsEmptyForNullEmptyAndBlank(String code) {
            assertThat(SpecialistVerificationStatus.tryFrom(code)).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {" active", "active ", " active ", "\tactive\n", "\u00A0active", "active\u2003"})
        void doesNotTrimBecauseItOnlyReadsDatabaseValues(String code) {
            assertThat(SpecialistVerificationStatus.tryFrom(code)).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {" active ", "suspended", "Active", ""})
        void throwsIllegalArgumentExceptionFromFromCodeForAnInvalidCode(String code) {
            assertThatThrownBy(() -> SpecialistVerificationStatus.fromCode(code))
                    .isExactlyInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Unknown SpecialistVerificationStatus code: " + code);
        }

        @Test
        void throwsIllegalArgumentExceptionNotNullPointerExceptionFromFromCodeForNull() {
            assertThatThrownBy(() -> SpecialistVerificationStatus.fromCode(null))
                    .isExactlyInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Unknown SpecialistVerificationStatus code: null");
        }
    }
}