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

        assertThat(p.query().page()).isEqualTo(1);
        assertThat(p.query().pageSize()).isEqualTo(20);
        assertThat(p.query().filters()).isEmpty();
        assertThat(p.query().sorts()).isEmpty();
    }

    @Test
    void searchDropsFillerWordsAndKeepsTheRest() {
        assertThat(parse("search", "Dr. Sara in anxiety").words()).containsExactly("sara", "anxiety");
    }

    @Test
    void contactAndStatusFieldsAreNotFilterable() {
        assertInvalid("filter[whatsapp_number]", "1");
        assertInvalid("filter[verification_status]", "active");
    }

    @Test
    void sortAndFiltersUseTheSpecialistFields() {
        assertThat(parse("sort", "-years_of_experience,last_name").query().sorts()).hasSize(2);
        assertThat(parse("filter[years_of_experience][gte]", "10").query().filters()).hasSize(1);
        assertInvalid("filter[nope]", "1");
    }

    @Test
    void distanceSortNeedsACoordinate() {
        assertInvalid("sort", "distance");
        assertThat(parse("sort", "distance", "lat", "33.9", "lng", "35.5").near()).isNotNull();
    }

    @Test
    void availabilityTakesDaysAndHoursEitherAlone() {
        SearchParams.Availability a = parse("available_days", "Monday, friday,monday").availability();

        assertThat(a.days()).containsExactly("monday", "friday");
        assertThat(a.from()).isNull();
        assertThat(parse("available_from", "09:30").availability().from()).hasHour(9).hasMinute(30);
        assertThat(parse().availability()).isNull();
    }

    @Test
    void badDaysAndHoursAreRejected() {
        assertInvalid("available_days", "funday");
        assertInvalid("available_from", "9am");
        assertInvalid("available_from", "24:00");
        assertInvalid("available_from", "12:00", "available_to", "09:00");
    }

    @Test
    void pageSizeIsCappedAt50() {
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
