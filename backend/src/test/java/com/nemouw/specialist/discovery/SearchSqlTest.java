package com.nemouw.specialist.discovery;

import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.nemouw.common.time.PlatformTime;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class SearchSqlTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 12, 10, 0);

    @Test
    void alwaysRestrictsToDiscoverableSpecialists() {
        SearchSql.Query q = build();

        assertThat(q.dataSql()).contains("u.status = 'active'").contains("s.verification_status = 'active'");
        assertThat(q.countSql()).contains("u.status = 'active'");
    }

    @Test
    void valuesAreBoundNeverInlined() {
        SearchSql.Query q = build("filter[last_name]", "x'; drop table users;--");

        assertThat(q.dataSql()).doesNotContain("drop table");
        assertThat(q.params().values()).anyMatch(v -> v.toString().contains("drop table"));
    }

    @Test
    void distanceIsATypedNullWithoutNear() {
        assertThat(build().dataSql()).contains("CAST(NULL AS double precision) AS distance_km");
        assertThat(build("location", "beirut").dataSql()).contains("CAST(NULL AS double precision) AS distance_km");
    }

    @Test
    void noLocationFilterKeepsSpecialistsWithoutLocations() {
        assertThat(build().countSql().split("LEFT JOIN LATERAL", -1)).hasSize(3);
        assertThat(build().countSql()).doesNotContain("\nJOIN LATERAL");
    }

    @Test
    void placeDaysAndHoursMustFitTheSameLocation() {
        SearchSql.Query q = build("location", "beirut", "available_days", "monday,tuesday",
                "available_from", "09:00", "available_to", "12:00");

        assertThat(q.countSql()).contains("\nJOIN LATERAL").contains("w.day_of_week IN (:availDays)")
                .contains("w.end_time > CAST(:availFrom AS time)").contains("w.start_time < CAST(:availTo AS time)")
                .contains("w.location_id = l.id OR w.location_id IS NULL");
        assertThat(q.params()).containsEntry("availDays", java.util.List.of("monday", "tuesday"));
    }

    @Test
    void anyOneOfDaysOrHoursIsEnough() {
        assertThat(build("available_days", "friday").countSql()).contains("availDays").doesNotContain("availFrom");
        assertThat(build("available_from", "18:00").countSql()).contains("availFrom").doesNotContain("availDays");
    }

    @Test
    void explicitSortEndsWithATieBreaker() {
        String sql = build("sort", "-years_of_experience").dataSql();

        assertThat(sql).contains("s.years_of_experience DESC NULLS LAST").contains("s.id");
    }

    @Test
    void anyOfFilterBecomesAnInList() {
        assertThat(build("filter[last_name]", "a,b").countSql()).contains("IN (");
    }

    @Test
    void nullTestsNeedNoBoundValue() {
        assertThat(build("filter[session_price_max][is_null]", "true").countSql()).contains("IS NULL");
    }

    @Test
    void todayFollowsTheBeirutCalendarAcrossBothClockChanges() {
        // spring: 22:00Z on Mar 28 is 01:00 on Sunday Mar 29 (00:00 to 00:59 never shows)
        assertDay("2026-03-28T21:59:59Z", LocalDate.of(2026, 3, 28));
        assertDay("2026-03-28T22:00:00Z", LocalDate.of(2026, 3, 29));
        // autumn: 23:30 on Saturday Oct 24 happens twice and stays Saturday both times
        assertDay("2026-10-24T20:30:00Z", LocalDate.of(2026, 10, 24));
        assertDay("2026-10-24T21:30:00Z", LocalDate.of(2026, 10, 24));
        assertDay("2026-10-24T22:00:00Z", LocalDate.of(2026, 10, 25));
    }

    @Test
    void genderAndCurrencyFiltersTakeOnlyTheirCodes() {
        assertThat(build("filter[gender]", "female").countSql()).contains("s.gender IN (");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> build("filter[gender]", "Female"))
                .isInstanceOf(com.nemouw.common.exception.ValidationException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> build("filter[price_currency]", "EUR"))
                .isInstanceOf(com.nemouw.common.exception.ValidationException.class);
    }

    private static void assertDay(String instant, LocalDate date) {
        PlatformTime clock = new PlatformTime(Clock.fixed(Instant.parse(instant), ZoneId.of("UTC")), ZoneId.of("Asia/Beirut"));
        SearchSql.Query q = SearchSql.build(SearchParamsParser.parse(new LinkedMultiValueMap<>()), clock.now());

        assertThat(q.params()).containsEntry("today", date);
    }

    private static SearchSql.Query build(String... kv) {
        MultiValueMap<String, String> m = new LinkedMultiValueMap<>();
        for (int i = 0; i < kv.length; i += 2) m.add(kv[i], kv[i + 1]);
        return SearchSql.build(SearchParamsParser.parse(m), NOW);
    }
}