package com.nemouw.specialist.discovery;

import com.nemouw.common.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SearchParamsParserTest {
    @Test
    void defaults() {
        SearchParams p = parse();

        assertThat(p.page()).isEqualTo(1);
        assertThat(p.pageSize()).isEqualTo(20);
        assertThat(p.filters()).isEmpty();
        assertThat(p.sorts()).isEmpty();
    }

    @Test
    void searchDropsFillerWordsAndKeepsTheRest() {
        assertThat(parse("search", "Dr. Sara in anxiety").words()).containsExactly("sara", "anxiety");
    }

    @Test
    void listFilterMeansAnyOf() {
        SearchParams.Filter f = parse("filter[last_name]", "haddad,khoury").filters().get(0);

        assertThat(f.field().name()).isEqualTo("last_name");
        assertThat(f.op()).isEqualTo(SearchFields.Op.EQ);
        assertThat(f.values()).containsExactly("haddad", "khoury");
    }

    @Test
    void operatorFilterParsesTheValueByFieldType() {
        SearchParams.Filter f = parse("filter[years_of_experience][gte]", "10").filters().get(0);

        assertThat(f.op()).isEqualTo(SearchFields.Op.GTE);
        assertThat(f.values()).hasSize(1);
    }

    @Test
    void sortKeepsOrderAndDirection() {
        assertThat(parse("sort", "-years_of_experience,last_name").sorts())
                .extracting(SearchParams.Sort::key, SearchParams.Sort::desc)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("years_of_experience", true),
                        org.assertj.core.groups.Tuple.tuple("last_name", false));
    }

    @Test
    void contactAndStatusFieldsAreNotFilterable() {
        assertInvalid("filter[whatsapp_number]", "1");
        assertInvalid("filter[verification_status]", "active");
    }

    @Test
    void unknownFieldOperatorAndBadValueAreRejected() {
        assertInvalid("filter[nope]", "1");
        assertInvalid("filter[username][gt]", "a");
        assertInvalid("filter[years_of_experience][gte]", "abc");
        assertInvalid("sort", "nope");
    }

    @Test
    void distanceSortNeedsACoordinate() {
        assertInvalid("sort", "distance");
        assertThat(parse("sort", "distance", "lat", "33.9", "lng", "35.5").near()).isNotNull();
    }

    @Test
    void pageSizeIsCapped() {
        assertInvalid("page_size", "51");
    }

    private static SearchParams parse(String... kv) {
        MultiValueMap<String, String> m = new LinkedMultiValueMap<>();
        for (int i = 0; i < kv.length; i += 2) m.add(kv[i], kv[i + 1]);
        return SearchParamsParser.parse(m);
    }

    private static void assertInvalid(String... kv) {
        assertThatThrownBy(() -> parse(kv)).isInstanceOf(ValidationException.class);
    }
}
