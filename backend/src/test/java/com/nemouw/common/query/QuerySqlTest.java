package com.nemouw.common.query;

import com.nemouw.common.util.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static com.nemouw.common.query.FieldType.*;
import static org.assertj.core.api.Assertions.assertThat;

class QuerySqlTest {
    private static final FieldSet FIELDS = FieldSet.of(
            new Field("email", TEXT, "u.email"),
            new Field("age", NUMBER, "u.age"));

    @Test
    void baseRestrictionAlwaysApplies() {
        QuerySql.Statement s = list();

        assertThat(s.dataSql()).contains("WHERE (u.deleted_at IS NULL)");
        assertThat(s.countSql()).contains("WHERE (u.deleted_at IS NULL)");
    }

    @Test
    void filtersNarrowAndValuesAreBound() {
        QuerySql.Statement s = list("filter[email]", "x'; drop table users;--", "filter[age][gte]", "5");

        assertThat(s.dataSql()).doesNotContain("drop table").contains("lower(u.email) IN (:ff0)")
                .contains("u.age >= :ff1");
        assertThat(s.params()).containsKeys("ff0", "ff1", "limit", "offset");
    }

    @Test
    void defaultOrderThenTieBreaker() {
        assertThat(list().dataSql()).contains("ORDER BY u.created_at DESC, u.id");
    }

    @Test
    void requestedSortReplacesDefaultAndKeepsTieBreaker() {
        assertThat(list("sort", "-age").dataSql()).contains("ORDER BY u.age DESC NULLS LAST, u.id")
                .doesNotContain("u.created_at");
    }

    @Test
    void containsEscapesLikeWildcards() {
        QuerySql.Statement s = list("filter[email][contains]", "50%_off");

        assertThat(s.params().get("ff0")).isEqualTo("50\\%\\_off");
    }

    private static QuerySql.Statement list(String... kv) {
        MultiValueMap<String, String> m = new LinkedMultiValueMap<>();
        for (int i = 0; i < kv.length; i += 2) m.add(kv[i], kv[i + 1]);
        Validator v = new Validator();
        QuerySpec spec = QueryParser.parse(FIELDS, m, v, 50);
        v.throwIfInvalid();
        return QuerySql.list("u.id, u.email", "users u", "u.deleted_at IS NULL", spec, FIELDS, "u.created_at DESC", "u.id");
    }
}
