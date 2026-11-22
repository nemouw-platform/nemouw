package com.nemouw.specialist.profile;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UsernamePolicyTest {

    @Nested
    class HappyPaths {

        @ParameterizedTest
        @ValueSource(strings = {"dr.amira", "amira_haddad", "sara-khoury", "karim.nassar7"})
        void allowsOrdinaryUsernames(String username) {
            assertThat(UsernamePolicy.isReserved(username)).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {"apiary", "authors", "administrate", "searching"})
        void allowsNamesThatOnlyExtendAReservedWord(String username) {
            assertThat(UsernamePolicy.isReserved(username)).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {"dr.nemouri", "nemoura", "dr.nemowitz"})
        void allowsNamesThatMerelyContainNemouOrNemow(String username) {
            assertThat(UsernamePolicy.isReserved(username)).isFalse();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = "   ")
        void treatsNullEmptyAndBlankAsNotReserved(String username) {
            assertThat(UsernamePolicy.isReserved(username)).isFalse();
        }

        @Test
        void loadsTheShippedFileTogetherWithTheTwoWordsTheDatabaseRefuses() {
            Set<String> reserved = UsernamePolicy.load("/reserved-usernames.txt");

            assertThat(reserved).contains("me", "byid", "admin", "nemouw");
        }
    }

    @Nested
    class ErrorPaths {

        @ParameterizedTest
        @ValueSource(strings = {"api", "login", "specialist", "syndicate", "credentials", "admin"})
        void rejectsReservedWords(String username) {
            assertThat(UsernamePolicy.isReserved(username)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"me", "by-id", "byid", "by_id"})
        void rejectsTheWordsTheDatabaseRefuses(String username) {
            assertThat(UsernamePolicy.isReserved(username)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"ADMIN", " admin ", "Admin"})
        void ignoresCaseAndSurroundingSpaces(String username) {
            assertThat(UsernamePolicy.isReserved(username)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"a.d.m.i.n", "sign-up", "s_i_g_n_u_p"})
        void seesThroughSeparators(String username) {
            assertThat(UsernamePolicy.isReserved(username)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"adm1n", "l0gin", "5ettings", "4uth", "r00t"})
        void seesThroughLeetSpeak(String username) {
            assertThat(UsernamePolicy.isReserved(username)).isTrue();
        }

        @Test
        void seesThroughSeparatorsAndLeetSpeakTogether() {
            assertThat(UsernamePolicy.isReserved("4.d-m1n")).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"nemouw", "dr.nemouw", "thenemouw", "NemoUW", "n3m0uw", "ne.mo.uw", "xn3m0uwx"})
        void rejectsThePlatformNameAnywhereInsideAUsername(String username) {
            assertThat(UsernamePolicy.isReserved(username)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"nemou", "nemow", "n3mou", "n.e.m.o.w"})
        void rejectsTheBrandLookalikesAsWholeNames(String username) {
            assertThat(UsernamePolicy.isReserved(username)).isTrue();
        }

        @Test
        void reservesEveryEntryOfTheReservedFile() throws IOException {
            try (InputStream in = UsernamePolicy.class.getResourceAsStream("/reserved-usernames.txt")) {
                List<String> entries = new String(in.readAllBytes(), StandardCharsets.UTF_8).lines()
                        .map(String::trim)
                        .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                        .toList();

                assertThat(entries).isNotEmpty().allSatisfy(entry ->
                        assertThat(UsernamePolicy.isReserved(entry)).as(entry).isTrue());
            }
        }
    }

    @Nested
    class BoundaryValues {

        @ParameterizedTest
        @ValueSource(strings = {"m3", "4pi"})
        void ignoresLeetSpeakWhenTheDecodedNameIsShorterThanFourCharacters(String username) {
            assertThat(UsernamePolicy.isReserved(username)).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {"4uth", "r00t"})
        void appliesLeetSpeakFromFourCharacters(String username) {
            assertThat(UsernamePolicy.isReserved(username)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"me", "api", "v1"})
        void stillRejectsShortReservedWordsWrittenLiterally(String username) {
            assertThat(UsernamePolicy.isReserved(username)).isTrue();
        }

        @ParameterizedTest
        @CsvSource({
                "nemouw,     true",
                "dr.nemouwi, true",
                "nemou,      true",
                "dr.nemouri, false",
                "nemowitz,   false"
        })
        void blocksNemouwAnywhereButNemouAndNemowOnlyAsTheWholeName(String username, boolean reserved) {
            assertThat(UsernamePolicy.isReserved(username)).isEqualTo(reserved);
        }
    }

    @Nested
    class DependencyFailures {

        @Test
        void failsWhenTheReservedFileIsMissing() {
            assertThatThrownBy(() -> UsernamePolicy.load("/missing-usernames.txt"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("/missing-usernames.txt");
        }
    }
}