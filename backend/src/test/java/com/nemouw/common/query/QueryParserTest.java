package com.nemouw.common.query;

import com.nemouw.common.exception.ValidationException;
import com.nemouw.common.util.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static com.nemouw.common.query.FieldType.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QueryParserTest {
    private static final FieldSet FIELDS = FieldSet.of(
                    new Field("name", TEXT, "t.name"),
                    new Field("age", NUMBER, "t.age"),
                    new Field("born_at", TIMESTAMP, "t.born_at"),
                    Field.enumOf("status", "t.status", "active", "suspended"))
            .withComputedSorts("recommended");

    @Test
    void defaults() {
        QuerySpec q = parse();

        assertThat(q.page()).isEqualTo(1);
        assertThat(q.pageSize()).isEqualTo(20);
        assertThat(q.filters()).isEmpty();
        assertThat(q.sorts()).isEmpty();
    }

    @Test
    void listFilterMeansAnyOf() {
        QuerySpec.Condition c = parse("filter[name]", "a,b").filters().getFirst();

        assertThat(c.op()).isEqualTo(Op.EQ);
        assertThat(c.values()).containsExactly("a", "b");
    }

    @Test
    void operatorFilterParsesByType() {
        assertThat(parse("filter[age][gte]", "10").filters().getFirst().values()).hasSize(1);
        assertThat(parse("filter[born_at][gte]", "2026-01-01").filters()).hasSize(1);
        assertThat(parse("filter[born_at][lt]", "2026-01-01T10:00:00+02:00").filters()).hasSize(1);
    }

    @Test
    void enumValuesAreChecked() {
        assertThat(parse("filter[status]", "active,suspended").filters()).hasSize(1);
        assertInvalid("filter[status]", "actve");
    }

    @Test
    void sortKeepsOrderAndComputedKeysCannotBeDescending() {
        assertThat(parse("sort", "-age,name,recommended").sorts())
                .extracting(QuerySpec.SortKey::key, QuerySpec.SortKey::desc)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("age", true),
                        org.assertj.core.groups.Tuple.tuple("name", false),
                        org.assertj.core.groups.Tuple.tuple("recommended", false));
        assertInvalid("sort", "-recommended");
    }

    @Test
    void unknownFieldOperatorValueAndSortAreRejected() {
        assertInvalid("filter[nope]", "1");
        assertInvalid("filter[name][gt]", "a");
        assertInvalid("filter[age][gte]", "abc");
        assertInvalid("filter[age][is_null]", "maybe");
        assertInvalid("sort", "nope");
        assertInvalid("filter[age]", "1,2,3,4,5,6,7,8,9,10,11");
    }

    @Test
    void pageSizeIsCapped() {
        assertInvalid("page_size", "51");
        assertInvalid("page", "0");
    }

    private static QuerySpec parse(String... kv) {
        MultiValueMap<String, String> m = new LinkedMultiValueMap<>();
        for (int i = 0; i < kv.length; i += 2) m.add(kv[i], kv[i + 1]);
        Validator v = new Validator();
        QuerySpec spec = QueryParser.parse(FIELDS, m, v, 50);
        v.throwIfInvalid();
        return spec;
    }

    private static void assertInvalid(String... kv) {
        assertThatThrownBy(() -> parse(kv)).isInstanceOf(ValidationException.class);
    }
}
