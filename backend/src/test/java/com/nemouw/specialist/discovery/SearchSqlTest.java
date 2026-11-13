package com.nemouw.specialist.discovery;

import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.LocalDateTime;

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

    private static SearchSql.Query build(String... kv) {
        MultiValueMap<String, String> m = new LinkedMultiValueMap<>();
        for (int i = 0; i < kv.length; i += 2) m.add(kv[i], kv[i + 1]);
        return SearchSql.build(SearchParamsParser.parse(m), NOW);
    }
}
