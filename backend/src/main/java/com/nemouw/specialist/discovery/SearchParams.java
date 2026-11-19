package com.nemouw.specialist.discovery;

import com.nemouw.common.enums.LocaleCode;
import com.nemouw.common.query.QuerySpec;

import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** A validated search request. Empty or null means "not asked for". Filters, sort and paging live in {@code query}. */
record SearchParams(List<String> words, List<String> place, UUID countryId, Near near, Set<UUID> conditionIds,
                    Integer patientAge, Availability availability, QuerySpec query, LocaleCode locale) {
    static final String RECOMMENDED = "recommended";
    static final String DISTANCE = "distance";

    record Near(double lat, double lng, double radiusKm) {}

    /** Wanted weekdays (empty means any) and hours (null means open-ended); one window must fit all of them. */
    record Availability(List<String> days, LocalTime from, LocalTime to) {}
}
