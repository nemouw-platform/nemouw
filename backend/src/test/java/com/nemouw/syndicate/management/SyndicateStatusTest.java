package com.nemouw.syndicate.management;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SyndicateStatusTest {

    @Nested
    class HappyPaths {

        @Test
        void spellsTheOnlyStatusAsActive() {
            assertThat(Arrays.stream(SyndicateStatus.values()).map(SyndicateStatus::code))
                    .containsExactly("active");
        }

        @Test
        void findsActiveByItsCode() {
            assertThat(SyndicateStatus.tryFrom("active")).contains(SyndicateStatus.ACTIVE);
        }

        @Test
        void returnsActiveFromFromCode() {
            assertThat(SyndicateStatus.fromCode("active")).isEqualTo(SyndicateStatus.ACTIVE);
        }
    }

    @Nested
    class BoundaryValues {

        @Test
        void hasExactlyOneStatusSoAddingOneForcesTheContractAndDatabaseToBeUpdated() {
            assertThat(SyndicateStatus.values()).containsExactly(SyndicateStatus.ACTIVE);
        }
    }

    @Nested
    class ErrorPaths {

        @ParameterizedTest
        @ValueSource(strings = {"Active", "ACTIVE", "aCtIvE"})
        void isCaseSensitive(String code) {
            assertThat(SyndicateStatus.tryFrom(code)).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {"suspended", "pending", "act ive", "1"})
        void returnsEmptyForUnknownStatuses(String code) {
            assertThat(SyndicateStatus.tryFrom(code)).isEmpty();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        void returnsEmptyForNullEmptyAndBlank(String code) {
            assertThat(SyndicateStatus.tryFrom(code)).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {" active", "active ", " active ", "\tactive\n", "\u00A0active", "active\u2003"})
        void doesNotTrimBecauseItOnlyReadsDatabaseValues(String code) {
            assertThat(SyndicateStatus.tryFrom(code)).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {" active ", "suspended", "Active", ""})
        void throwsIllegalArgumentExceptionFromFromCodeForAnInvalidCode(String code) {
            assertThatThrownBy(() -> SyndicateStatus.fromCode(code))
                    .isExactlyInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Unknown SyndicateStatus code: " + code);
        }

        @Test
        void throwsIllegalArgumentExceptionNotNullPointerExceptionFromFromCodeForNull() {
            assertThatThrownBy(() -> SyndicateStatus.fromCode(null))
                    .isExactlyInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Unknown SyndicateStatus code: null");
        }
    }
}