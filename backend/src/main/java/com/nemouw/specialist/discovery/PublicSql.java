package com.nemouw.specialist.discovery;

import com.nemouw.specialist.availability.WeekDay;

import java.util.Arrays;
import java.util.stream.Collectors;

/** SQL fragments shared by search and the public profile. */
final class PublicSql {
    static final String DEFAULT_LOCALE = "en";

    /** Verified profile and an active, non-deleted account. */
    static final String DISCOVERABLE = """
            s.verification_status = 'active' AND s.deleted_at IS NULL
              AND u.status = 'active' AND u.deleted_at IS NULL""";

    static final String COVERS_AGE = "COALESCE(s.min_age_treated, 0) <= :patientAge"
            + " AND COALESCE(s.max_age_treated, 150) >= :patientAge";

    private static final String WEEK = Arrays.stream(WeekDay.values())
            .map(d -> "'" + d.code() + "'").collect(Collectors.joining(", ", "ARRAY[", "]"));

    private PublicSql() {
    }

    /** Monday-first position of a day-name column. */
    static String weekdayPosition(String column) {
        return "array_position(" + WEEK + ", " + column + ")";
    }

    /** Joins the profile content row as {@code pc}: requested locale, then English, then any. */
    static String contentJoin(boolean withBio) {
        return """
                LEFT JOIN LATERAL (
                    SELECT p.locale_code, p.title, p.headline%s
                    FROM specialist_profile p
                    WHERE p.specialist_id = s.id
                    ORDER BY (p.locale_code = :locale) DESC, (p.locale_code = :defaultLocale) DESC, p.locale_code
                    LIMIT 1
                ) pc ON true""".formatted(withBio ? ", p.bio" : "");
    }
}
