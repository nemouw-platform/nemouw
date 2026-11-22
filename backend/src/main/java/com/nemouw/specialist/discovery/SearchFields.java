package com.nemouw.specialist.discovery;

import com.nemouw.common.enums.Currency;
import com.nemouw.common.enums.Gender;
import com.nemouw.common.enums.LocaleCode;
import com.nemouw.common.query.Field;
import com.nemouw.common.query.FieldSet;

import static com.nemouw.common.query.FieldType.*;

/**
 * Every field clients can filter and sort by. One line per field: name, type, SQL expression.
 * Contact and review data (whatsapp_number, verification and user status) is deliberately absent.
 */
final class SearchFields {
    static final FieldSet FIELDS = FieldSet.of(
                    new Field("specialty_id", UUID, "s.specialty_id"),
                    new Field("syndicate_id", UUID, "s.syndicate_id"),
                    new Field("username", TEXT, "s.username"),
                    new Field("first_name", TEXT, "u.first_name"),
                    new Field("last_name", TEXT, "u.last_name"),
                    new Field("title", TEXT, "pc.title"),
                    new Field("headline", TEXT, "pc.headline"),
                    Field.enumOf("gender", "s.gender", Gender.class),
                    Field.enumOf("primary_language", "s.primary_language", LocaleCode.class),
                    new Field("years_of_experience", NUMBER, "s.years_of_experience"),
                    new Field("min_age_treated", NUMBER, "s.min_age_treated"),
                    new Field("max_age_treated", NUMBER, "s.max_age_treated"),
                    new Field("session_price_min", NUMBER, "s.session_price_min"),
                    new Field("session_price_max", NUMBER, "s.session_price_max"),
                    Field.enumOf("price_currency", "s.price_currency", Currency.class),
                    new Field("created_at", DATE, "CAST(s.created_at AS date)"))
            .withComputedSorts(SearchParams.RECOMMENDED, SearchParams.DISTANCE);

    private SearchFields() {
    }
}
