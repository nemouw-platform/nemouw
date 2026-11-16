package com.nemouw.specialist.discovery;

import com.nemouw.common.query.QuerySql;
import com.nemouw.common.query.QuerySpec.SortKey;
import com.nemouw.specialist.discovery.SearchParams.Availability;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Builds the search statements. Each handler adds one concern (filters, text, location, sort);
 * adding a concern means adding one handler to {@link #HANDLERS}. Every value is bound, never concatenated.
 */
final class SearchSql {
    record Query(String dataSql, String countSql, Map<String, Object> params) {
    }

    /** What the handlers contribute. {@code joins} run in the count too, {@code selectJoins} only in the page. */
    private static final class Parts {
        final List<String> columns = new ArrayList<>();
        final List<String> joins = new ArrayList<>();
        final List<String> selectJoins = new ArrayList<>();
        final List<String> where = new ArrayList<>();
        final List<String> relevance = new ArrayList<>();
        final List<String> orderBy = new ArrayList<>();
        final Map<String, Object> params = new LinkedHashMap<>();
    }

    private static final List<BiConsumer<SearchParams, Parts>> HANDLERS = List.of(
            SearchSql::content, SearchSql::fieldFilters, SearchSql::conditions, SearchSql::patientAge,
            SearchSql::text, SearchSql::location, SearchSql::sort);

    private static final List<String> BASE_COLUMNS = List.of(
            "s.id", "s.username", "u.first_name", "u.last_name", "s.specialty_id",
            "pm.s3_key AS profile_picture_key", "s.years_of_experience", "s.primary_language",
            "s.session_price_min", "s.session_price_max", "s.price_currency",
            "s.min_age_treated", "s.max_age_treated");

    private SearchSql() {
    }

    static Query build(SearchParams p, LocalDateTime now) {
        Parts q = new Parts();
        q.columns.addAll(BASE_COLUMNS);
        q.selectJoins.add("LEFT JOIN media pm ON pm.id = s.profile_picture_id");
        q.params.put("limit", p.query().pageSize());
        q.params.put("offset", p.query().offset());
        q.params.put("today", now.toLocalDate());
        HANDLERS.forEach(h -> h.accept(p, q));

        String from = "FROM specialists s\nJOIN users u ON u.id = s.user_id\n" + String.join("\n", q.joins) + "\n";
        String where = "WHERE " + PublicSql.DISCOVERABLE + q.where.stream().map(c -> "\n  AND " + c).reduce("", String::concat);
        String data = "SELECT " + String.join(",\n       ", q.columns) + "\n" + from + String.join("\n", q.selectJoins)
                + "\n" + where + "\nORDER BY " + String.join(", ", q.orderBy) + "\nLIMIT :limit OFFSET :offset";
        return new Query(data, "SELECT count(*)\n" + from + where, Map.copyOf(q.params));
    }

    private static void content(SearchParams p, Parts q) {
        q.joins.add(PublicSql.contentJoin(false));
        q.columns.add("pc.title");
        q.columns.add("pc.headline");
        q.params.put("locale", p.locale().code());
        q.params.put("defaultLocale", PublicSql.DEFAULT_LOCALE);
    }

    private static void fieldFilters(SearchParams p, Parts q) {
        q.where.addAll(QuerySql.where(p.query().filters(), "ff", q.params));
    }

    private static void conditions(SearchParams p, Parts q) {
        if (p.conditionIds().isEmpty()) return;
        q.joins.add("""
                CROSS JOIN LATERAL (
                    SELECT count(*) AS n FROM specialist_treated_conditions tc
                    WHERE tc.specialist_id = s.id AND tc.condition_id IN (:conditionIds)
                ) cm""");
        q.where.add("cm.n > 0");
        q.relevance.add("cm.n * 15");
        q.params.put("conditionIds", p.conditionIds());
    }

    private static void patientAge(SearchParams p, Parts q) {
        if (p.patientAge() == null) return;
        q.where.add(PublicSql.COVERS_AGE);
        q.params.put("patientAge", p.patientAge());
    }

    /** Every word must match somewhere; better fields score higher; long words also match near-spellings. */
    private static void text(SearchParams p, Parts q) {
        List<String> words = p.words();
        if (words.isEmpty()) return;
        List<String> normalized = new ArrayList<>();
        List<String> scores = new ArrayList<>();
        for (int i = 0; i < words.size(); i++) {
            normalized.add("search_normalize(CAST(:t" + i + " AS text)) AS n" + i);
            q.params.put("t" + i, words.get(i));
            List<String> perField = new ArrayList<>();
            for (TextField f : TEXT_FIELDS) perField.add(f.score(i, words.get(i).length() >= 4));
            scores.add("GREATEST(" + String.join(", ", perField) + ") AS s" + i);
            q.where.add("sc.s" + i + " > 0");
            q.relevance.add("sc.s" + i);
        }
        q.joins.add("CROSS JOIN (SELECT " + String.join(", ", normalized) + ") tm");
        q.joins.add(TEXT_DOCUMENTS);
        q.joins.add("CROSS JOIN LATERAL (SELECT " + String.join(", ", scores) + ") sc");
    }

    private record TextField(String doc, int points, boolean fuzzy) {
        String score(int i, boolean longWord) {
            String near = fuzzy && longWord
                    ? " WHEN word_similarity(tm.n" + i + ", doc." + doc + ") >= 0.5 THEN " + points / 2 : "";
            return "CASE WHEN strpos(doc." + doc + ", tm.n" + i + ") > 0 THEN " + points + near + " ELSE 0 END";
        }
    }

    private static final List<TextField> TEXT_FIELDS = List.of(
            new TextField("who", 50, true), new TextField("specialty", 40, true),
            new TextField("conditions", 40, true), new TextField("headline", 30, false),
            new TextField("place", 20, false), new TextField("bio", 10, false));

    private static final String TEXT_DOCUMENTS = """
            CROSS JOIN LATERAL (
                SELECT
                    search_normalize(concat_ws(' ', u.first_name, u.last_name, s.username)) AS who,
                    COALESCE(search_normalize((SELECT string_agg(concat_ws(' ', p.title, p.headline), ' ')
                                               FROM specialist_profile p WHERE p.specialist_id = s.id)), '') AS headline,
                    COALESCE(search_normalize((SELECT string_agg(p.bio, ' ')
                                               FROM specialist_profile p WHERE p.specialist_id = s.id)), '') AS bio,
                    COALESCE(search_normalize((SELECT concat_ws(' ', sp.name, sp.label, sp.label_fr, sp.label_ar)
                                               FROM specialties sp WHERE sp.id = s.specialty_id)), '') AS specialty,
                    COALESCE(search_normalize((SELECT string_agg(concat_ws(' ', c.name, c.label, c.label_fr, c.label_ar), ' ')
                                               FROM specialist_treated_conditions tc
                                               JOIN conditions c ON c.id = tc.condition_id
                                               WHERE tc.specialist_id = s.id)), '') AS conditions,
                    COALESCE(search_normalize((SELECT string_agg(concat_ws(' ', l.city, l.label, l.address_line1, l.address_line2), ' ')
                                               FROM specialist_locations l WHERE l.specialist_id = s.id)), '') AS place
            ) doc""";

    private static final String NO_DISTANCE = "CAST(NULL AS double precision)";
    private static final String PLACE_TEXT =
            "search_normalize(concat_ws(' ', l.city, l.label, l.address_line1, l.address_line2))";
    private static final String HAVERSINE = """
            CASE WHEN l.latitude IS NOT NULL THEN 6371
                 * acos(least(1, cos(radians(:lat)) * cos(radians(CAST(l.latitude AS double precision)))
                       * cos(radians(CAST(l.longitude AS double precision)) - radians(:lng))
                       + sin(radians(:lat)) * sin(radians(CAST(l.latitude AS double precision))))) END""";

    /**
     * One location per specialist: the nearest (or primary) one matching every place and availability filter,
     * so "Beirut, Monday mornings" means a Beirut location with a Monday morning window.
     */
    private static void location(SearchParams p, Parts q) {
        List<String> match = new ArrayList<>(List.of("l.specialist_id = s.id"));
        List<String> keep = new ArrayList<>();
        List<String> order = new ArrayList<>();
        if (p.countryId() != null) {
            match.add("l.country_id = :countryId");
            q.params.put("countryId", p.countryId());
        }
        for (int i = 0; i < p.place().size(); i++) {
            String term = "search_normalize(CAST(:p" + i + " AS text))";
            String hit = "strpos(" + PLACE_TEXT + ", " + term + ") > 0";
            match.add(p.place().get(i).length() >= 5
                    ? "(" + hit + " OR word_similarity(" + term + ", " + PLACE_TEXT + ") >= 0.6)" : hit);
            q.params.put("p" + i, p.place().get(i));
        }
        if (p.availability() != null) match.add(availability(p.availability(), q));
        boolean near = p.near() != null;
        if (near) {
            q.params.put("lat", p.near().lat());
            q.params.put("lng", p.near().lng());
            q.params.put("radiusKm", p.near().radiusKm());
            keep.add("d.distance_km <= :radiusKm");
            order.add("d.distance_km");
        }
        order.addAll(List.of("d.is_primary DESC", "d.created_at", "d.id"));

        boolean restricts = match.size() > 1 || !keep.isEmpty();
        q.columns.addAll(List.of("loc.city", "loc.country_id", "loc.distance_km"));
        q.joins.add("""
                %sJOIN LATERAL (
                    SELECT d.city, d.country_id, d.distance_km
                    FROM (SELECT l.id, l.city, l.country_id, l.is_primary, l.created_at, %s AS distance_km
                          FROM specialist_locations l WHERE %s) d
                    %s
                    ORDER BY %s
                    LIMIT 1
                ) loc ON true""".formatted(restricts ? "" : "LEFT ", near ? HAVERSINE : NO_DISTANCE,
                String.join(" AND ", match), keep.isEmpty() ? "" : "WHERE " + String.join(" AND ", keep),
                String.join(", ", order)));
    }

    /** One weekly window of location {@code l} (or of no location, which fits every one) fitting all wishes. */
    private static String availability(Availability a, Parts q) {
        List<String> on = new ArrayList<>(List.of(
                "w.specialist_id = l.specialist_id", "(w.location_id = l.id OR w.location_id IS NULL)"));
        if (!a.days().isEmpty()) {
            on.add("w.day_of_week IN (:availDays)");
            q.params.put("availDays", a.days());
        }
        if (a.from() != null) {
            on.add("w.end_time > CAST(:availFrom AS time)");
            q.params.put("availFrom", a.from());
        }
        if (a.to() != null) {
            on.add("w.start_time < CAST(:availTo AS time)");
            q.params.put("availTo", a.to());
        }
        return "EXISTS (SELECT 1 FROM specialist_location_availabilities w WHERE " + String.join(" AND ", on) + ")";
    }

    private static final String QUALITY = """
            (CASE WHEN s.profile_picture_id IS NOT NULL THEN 2 ELSE 0 END
              + CASE WHEN EXISTS (SELECT 1 FROM specialist_profile p
                                  WHERE p.specialist_id = s.id AND p.bio IS NOT NULL) THEN 1 ELSE 0 END
              + CASE WHEN EXISTS (SELECT 1 FROM specialist_treated_conditions tc
                                  WHERE tc.specialist_id = s.id) THEN 1 ELSE 0 END
              + CASE WHEN EXISTS (SELECT 1 FROM specialist_location_availabilities a
                                  WHERE a.specialist_id = s.id) THEN 1 ELSE 0 END
              + CASE WHEN EXISTS (SELECT 1 FROM specialist_credentials cr
                                  WHERE cr.specialist_id = s.id AND cr.verification_status = 'verified'
                                    AND (cr.expiry_date IS NULL OR cr.expiry_date >= CAST(:today AS date))) THEN 1 ELSE 0 END)""";

    /** Requested keys first, then relevance and id, so pages never repeat or skip a specialist. */
    private static void sort(SearchParams p, Parts q) {
        String relevance = q.relevance.isEmpty() ? null : "(" + String.join(" + ", q.relevance) + ") DESC";
        boolean ranked = false;
        List<SortKey> sorts = p.query().sorts().isEmpty() ? List.of(new SortKey(SearchParams.RECOMMENDED, false))
                : p.query().sorts();
        for (SortKey s : sorts) {
            switch (s.key()) {
                case SearchParams.RECOMMENDED -> {
                    ranked = true;
                    if (relevance != null) q.orderBy.add(relevance);
                    q.orderBy.add(QUALITY + " DESC");
                    q.orderBy.add("s.years_of_experience DESC NULLS LAST");
                }
                case SearchParams.DISTANCE -> q.orderBy.add("loc.distance_km ASC NULLS LAST");
                default -> q.orderBy.add(QuerySql.order(SearchFields.FIELDS.find(s.key()).orElseThrow(), s.desc()));
            }
        }
        if (!ranked && relevance != null) q.orderBy.add(relevance);
        q.orderBy.add("s.id");
    }
}